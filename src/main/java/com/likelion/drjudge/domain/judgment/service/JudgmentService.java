package com.likelion.drjudge.domain.judgment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.drjudge.domain.category.entity.Category;
import com.likelion.drjudge.domain.category.exception.CategoryErrorCode;
import com.likelion.drjudge.domain.category.repository.CategoryRepository;
import com.likelion.drjudge.domain.judgment.dto.ai.AiJudgmentRequest;
import com.likelion.drjudge.domain.judgment.dto.ai.AiJudgmentResponse;
import com.likelion.drjudge.domain.judgment.dto.request.CreateJudgmentRequest;
import com.likelion.drjudge.domain.judgment.dto.response.ConflictOfInterestResponse;
import com.likelion.drjudge.domain.judgment.dto.response.CreateJudgmentResponse;
import com.likelion.drjudge.domain.judgment.dto.response.GuideCardResponse;
import com.likelion.drjudge.domain.judgment.dto.response.JudgmentDetailResponse;
import com.likelion.drjudge.domain.judgment.dto.response.JudgmentListResponse;
import com.likelion.drjudge.domain.judgment.dto.response.JudgmentSummaryResponse;
import com.likelion.drjudge.domain.judgment.dto.response.SourceResponse;
import com.likelion.drjudge.domain.judgment.entity.Judgment;
import com.likelion.drjudge.domain.judgment.entity.JudgmentRequestCount;
import com.likelion.drjudge.domain.judgment.entity.JudgmentStatus;
import com.likelion.drjudge.domain.judgment.exception.JudgmentErrorCode;
import com.likelion.drjudge.domain.judgment.extraction.ClovaOcrClient;
import com.likelion.drjudge.domain.judgment.extraction.YoutubeClient;
import com.likelion.drjudge.domain.judgment.repository.JudgmentRepository;
import com.likelion.drjudge.domain.judgment.repository.JudgmentRequestCountRepository;
import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.domain.user.exception.UserErrorCode;
import com.likelion.drjudge.domain.user.repository.UserRepository;
import com.likelion.drjudge.global.constant.TrustLevel;
import com.likelion.drjudge.global.exception.BusinessException;
import com.likelion.drjudge.global.exception.CommonErrorCode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgmentService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String AI_SERVICE_ENDPOINT = "/internal/api/v1/judgments";

    private final JudgmentRepository judgmentRepository;
    private final JudgmentRequestCountRepository requestCountRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RestClient aiServiceRestClient;
    private final ObjectMapper objectMapper;
    private final ClovaOcrClient clovaOcrClient;
    private final YoutubeClient youtubeClient;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.judgment.daily-limit:5}")
    private int dailyLimit;

    /**
     * OCR/유튜브 추출은 최대 10초 걸리는 외부 API 호출이라, DB 트랜잭션(커넥션 점유) 밖에서
     * 실행해야 한다 — 트랜잭션 안에 있으면 동시 요청이 몰릴 때 커넥션 풀이 고갈될 수 있다.
     * 그래서 "일일 한도 예약(DB)" → "추출(외부 API, 트랜잭션 밖)" → "저장(DB)" 3단계로 쪼갠다.
     * 같은 빈 안에서 @Transactional 메서드를 직접 호출하면 프록시가 안 걸려서(self-invocation)
     * TransactionTemplate으로 명시적으로 트랜잭션을 연다.
     */
    public CreateJudgmentResponse create(Long userId, CreateJudgmentRequest request) {
        validateInput(request);

        PreparedJudgment prepared = transactionTemplate.execute(status -> reserve(userId, request));

        // 예약 이후(추출~저장) 어디서 실패하든, 예약 때 실제로 썼던 날짜 그대로 환불한다.
        // LocalDate.now()를 환불 시점에 다시 계산하면 자정을 걸친 요청에서 엉뚱한 날짜를
        // 환불하고 원래 예약분은 영영 안 풀리는 버그가 생긴다.
        try {
            String extractedText = extractText(request);
            String maskedText = PiiMasker.mask(extractedText);
            return transactionTemplate.execute(status -> save(prepared, request, maskedText));
        } catch (RuntimeException e) {
            transactionTemplate.executeWithoutResult(status -> refundDailyLimit(userId, prepared.requestDate()));
            throw e;
        }
    }

    private PreparedJudgment reserve(Long userId, CreateJudgmentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        LocalDate requestDate = LocalDate.now();
        checkAndIncrementDailyLimit(userId, requestDate);
        Category category = resolveCategory(request.categoryId());
        return new PreparedJudgment(user, category, requestDate);
    }

    private CreateJudgmentResponse save(PreparedJudgment prepared, CreateJudgmentRequest request, String extractedText) {
        Judgment judgment = Judgment.create(
                prepared.user(), request.inputType(), extractedText, prepared.category(), prepared.requestDate());
        judgmentRepository.save(judgment);
        return new CreateJudgmentResponse(judgment.getId(), judgment.getStatus());
    }

    private record PreparedJudgment(User user, Category category, LocalDate requestDate) {
    }

    /**
     * Controller가 create() 이후 별도로 호출한다 — 같은 빈 안에서 self-invocation하면
     * @Async 프록시가 안 걸려서 동기로 실행돼버린다.
     */
    @Async("taskExecutor")
    @Transactional
    public void processAsync(Long judgmentId) {
        Judgment judgment = judgmentRepository.findById(judgmentId)
                .orElseThrow(() -> new IllegalStateException("Judgment not found: " + judgmentId));

        try {
            Long categoryId = judgment.getCategory() != null ? judgment.getCategory().getId() : null;
            AiJudgmentResponse response = callAiService(judgment.getInputText(), categoryId);
            completeJudgment(judgment, response);
        } catch (Exception e) {
            log.warn("event=judgment_processing_failed judgmentId={} reason={}",
                    judgment.getId(), e.getMessage(), e);
            failJudgment(judgment, JudgmentErrorCode.AI_SERVICE_UNAVAILABLE.getCode());
        }
    }

    @Transactional(readOnly = true)
    public JudgmentDetailResponse get(Long userId, Long judgmentId) {
        Judgment judgment = judgmentRepository.findById(judgmentId)
                .orElseThrow(() -> new BusinessException(JudgmentErrorCode.JUDGMENT_NOT_FOUND));

        if (!judgment.getUser().getId().equals(userId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }

        return toDetailResponse(judgment);
    }

    @Transactional(readOnly = true)
    public JudgmentListResponse list(Long userId, Long categoryId, int page, int size) {
        User user = userRepository.getReferenceById(userId);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), safeSize);

        Slice<Judgment> slice = categoryId != null
                ? judgmentRepository.findAllByUserAndCategoryOrderByCreatedAtDescIdDesc(
                        user, categoryRepository.getReferenceById(categoryId), pageable)
                : judgmentRepository.findAllByUserOrderByCreatedAtDescIdDesc(user, pageable);

        List<JudgmentSummaryResponse> items = slice.getContent().stream()
                .map(this::toSummaryResponse)
                .toList();

        return new JudgmentListResponse(items, slice.hasNext());
    }

    private void validateInput(CreateJudgmentRequest request) {
        boolean valid = switch (request.inputType()) {
            case TEXT -> hasText(request.text());
            case IMAGE -> hasText(request.imageBase64());
            case LINK -> hasText(request.url());
        };
        if (!valid) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(CategoryErrorCode.INVALID_CATEGORY));
    }

    private String extractText(CreateJudgmentRequest request) {
        return switch (request.inputType()) {
            case TEXT -> request.text();
            case IMAGE -> clovaOcrClient.extractText(request.imageBase64());
            case LINK -> youtubeClient.extractText(request.url());
        };
    }

    private void checkAndIncrementDailyLimit(Long userId, LocalDate requestDate) {
        JudgmentRequestCount count = requestCountRepository.findByUserIdAndRequestDate(userId, requestDate)
                .orElseGet(() -> JudgmentRequestCount.create(userId, requestDate));

        if (count.getRequestCount() >= dailyLimit) {
            throw new BusinessException(JudgmentErrorCode.DAILY_LIMIT_EXCEEDED);
        }

        count.increment();
        requestCountRepository.save(count);
    }

    private void refundDailyLimit(Long userId, LocalDate requestDate) {
        // 자정 근처 요청은 생성일과 처리일(비동기)이 다를 수 있어, 증가시켰던 그 날짜를 그대로 써야 한다.
        requestCountRepository.findByUserIdAndRequestDate(userId, requestDate)
                .ifPresent(JudgmentRequestCount::decrement);
    }

    private AiJudgmentResponse callAiService(String text, Long categoryId) {
        AiJudgmentRequest request = new AiJudgmentRequest(text, categoryId);
        try {
            return doCallAiService(request);
        } catch (HttpServerErrorException | ResourceAccessException firstFailure) {
            // 5xx / timeout만 1회 재시도 (docs/AI_SERVICE_CONTRACT.md)
            return doCallAiService(request);
        }
    }

    private AiJudgmentResponse doCallAiService(AiJudgmentRequest request) {
        return aiServiceRestClient.post()
                .uri(AI_SERVICE_ENDPOINT)
                .body(request)
                .retrieve()
                .body(AiJudgmentResponse.class);
    }

    private void completeJudgment(Judgment judgment, AiJudgmentResponse response) {
        boolean detected = response.conflictOfInterest() != null && response.conflictOfInterest().detected();

        judgment.complete(
                response.title(),
                TrustLevel.valueOf(response.trustLevel()),
                response.evidenceSummary(),
                detected,
                detected ? response.conflictOfInterest().type() : null,
                detected ? response.conflictOfInterest().description() : null,
                response.safetyNotice(),
                writeJson(response.sources()),
                writeJson(response.guideCard()),
                null // TODO: Python이 판정 근거로 쓴 archive_item을 아직 안 돌려줌
        );
    }

    private void failJudgment(Judgment judgment, String errorCode) {
        judgment.fail(errorCode);
        refundDailyLimit(judgment.getUser().getId(), judgment.getRequestDate());
    }

    private JudgmentDetailResponse toDetailResponse(Judgment judgment) {
        Long categoryId = judgment.getCategory() != null ? judgment.getCategory().getId() : null;

        if (judgment.getStatus() == JudgmentStatus.COMPLETED) {
            return new JudgmentDetailResponse(
                    judgment.getId(), judgment.getStatus(), judgment.getTitle(),
                    judgment.getTrustLevel().name(), judgment.getTrustLevel().getLabel(),
                    judgment.getEvidenceSummary(),
                    toConflictResponse(judgment),
                    judgment.getSafetyNotice(),
                    readSources(judgment.getSourcesJson()),
                    readGuideCard(judgment.getGuideCardJson()),
                    judgment.getInputText(),
                    null, null,
                    judgment.getInputType(), categoryId, judgment.getCreatedAt());
        }

        if (judgment.getStatus() == JudgmentStatus.FAILED) {
            return new JudgmentDetailResponse(
                    judgment.getId(), judgment.getStatus(), null,
                    null, null, null, null, null, null, null, null,
                    judgment.getFailureErrorCode(), resolveErrorMessage(judgment.getFailureErrorCode()),
                    judgment.getInputType(), categoryId, judgment.getCreatedAt());
        }

        return new JudgmentDetailResponse(
                judgment.getId(), judgment.getStatus(), null,
                null, null, null, null, null, null, null, null,
                null, null,
                judgment.getInputType(), categoryId, judgment.getCreatedAt());
    }

    private JudgmentSummaryResponse toSummaryResponse(Judgment judgment) {
        boolean completed = judgment.getStatus() == JudgmentStatus.COMPLETED;
        Long categoryId = judgment.getCategory() != null ? judgment.getCategory().getId() : null;

        return new JudgmentSummaryResponse(
                judgment.getId(),
                judgment.getStatus(),
                completed ? judgment.getTitle() : null,
                completed ? judgment.getTrustLevel().name() : null,
                completed ? judgment.getTrustLevel().getLabel() : null,
                judgment.getInputType(),
                categoryId,
                judgment.getCreatedAt());
    }

    private ConflictOfInterestResponse toConflictResponse(Judgment judgment) {
        if (!judgment.isConflictDetected()) {
            return new ConflictOfInterestResponse(false, null, null, null);
        }
        return new ConflictOfInterestResponse(
                true,
                judgment.getConflictType(),
                resolveBadgeLabel(judgment.getConflictType()),
                judgment.getConflictDescription());
    }

    // TODO: conflictType 종류가 늘어나면 별도 enum(+라벨)으로 분리
    private String resolveBadgeLabel(String conflictType) {
        return "COMMERCIAL".equals(conflictType) ? "상업적 의도 있음" : conflictType;
    }

    private String resolveErrorMessage(String errorCode) {
        return Arrays.stream(JudgmentErrorCode.values())
                .filter(code -> code.getCode().equals(errorCode))
                .findFirst()
                .map(JudgmentErrorCode::getMessage)
                .orElse(JudgmentErrorCode.AI_SERVICE_UNAVAILABLE.getMessage());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("판정 결과 직렬화 실패", e);
        }
    }

    private List<SourceResponse> readSources(String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<SourceResponse>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("sources 역직렬화 실패", e);
        }
    }

    private GuideCardResponse readGuideCard(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, GuideCardResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("guideCard 역직렬화 실패", e);
        }
    }
}
