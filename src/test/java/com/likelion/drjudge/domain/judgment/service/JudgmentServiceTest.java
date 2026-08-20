package com.likelion.drjudge.domain.judgment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import com.likelion.drjudge.domain.category.repository.CategoryRepository;
import com.likelion.drjudge.domain.judgment.dto.ai.AiJudgmentRequest;
import com.likelion.drjudge.domain.judgment.dto.ai.AiJudgmentResponse;
import com.likelion.drjudge.domain.judgment.dto.request.CreateJudgmentRequest;
import com.likelion.drjudge.domain.judgment.dto.response.CreateJudgmentResponse;
import com.likelion.drjudge.domain.judgment.dto.response.JudgmentDetailResponse;
import com.likelion.drjudge.domain.judgment.entity.InputType;
import com.likelion.drjudge.domain.judgment.entity.Judgment;
import com.likelion.drjudge.domain.judgment.entity.JudgmentRequestCount;
import com.likelion.drjudge.domain.judgment.entity.JudgmentStatus;
import com.likelion.drjudge.domain.judgment.exception.JudgmentErrorCode;
import com.likelion.drjudge.domain.judgment.extraction.ClovaOcrClient;
import com.likelion.drjudge.domain.judgment.extraction.YoutubeClient;
import com.likelion.drjudge.domain.judgment.repository.JudgmentRepository;
import com.likelion.drjudge.domain.judgment.repository.JudgmentRequestCountRepository;
import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.domain.user.repository.UserRepository;
import com.likelion.drjudge.global.constant.TrustLevel;
import com.likelion.drjudge.global.exception.BusinessException;
import com.likelion.drjudge.global.exception.CommonErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class JudgmentServiceTest {

    private static final int DAILY_LIMIT = 5;

    @Mock
    private JudgmentRepository judgmentRepository;
    @Mock
    private JudgmentRequestCountRepository requestCountRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private RestClient aiServiceRestClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ClovaOcrClient clovaOcrClient;
    @Mock
    private YoutubeClient youtubeClient;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private JudgmentService judgmentService;

    @BeforeEach
    void setUp() {
        // @Value는 순수 Mockito 테스트에선 주입 안 되니 수동으로 세팅한다.
        ReflectionTestUtils.setField(judgmentService, "dailyLimit", DAILY_LIMIT);

        // TransactionTemplate mock 기본 동작은 콜백을 실행 안 하고 null을 반환해서,
        // create()가 실제로 동작하도록 콜백을 그대로 실행해주는 pass-through로 만든다.
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        lenient().doAnswer(invocation -> {
            java.util.function.Consumer<Object> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void 판정_생성에_성공하면_PROCESSING_상태를_반환하고_일일카운트를_증가시킨다() {
        // 그 유저의 그날 첫 요청(카운트 행이 아직 없는 상태) — 운영에서 실제로 500이
        // 재현됐던 바로 그 경로라 명시적으로 검증한다.
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(requestCountRepository.findByUserIdAndRequestDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(judgmentRepository.save(any(Judgment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateJudgmentRequest request = new CreateJudgmentRequest(InputType.TEXT, "테스트 주장", null, null, null);

        CreateJudgmentResponse response = judgmentService.create(1L, request);

        assertEquals(JudgmentStatus.PROCESSING, response.status());

        ArgumentCaptor<JudgmentRequestCount> captor = ArgumentCaptor.forClass(JudgmentRequestCount.class);
        verify(entityManager).persist(captor.capture());
        assertEquals(1, captor.getValue().getRequestCount());
    }

    @Test
    void 오늘_카운트_행이_이미_있으면_명시적_UPDATE로_증가시킨다() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        JudgmentRequestCount existing = JudgmentRequestCount.create(1L, LocalDate.now());
        existing.increment(); // 오늘 이미 1번 요청함
        when(requestCountRepository.findByUserIdAndRequestDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));
        when(requestCountRepository.incrementCount(eq(1L), any(LocalDate.class))).thenReturn(1);
        when(judgmentRepository.save(any(Judgment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateJudgmentRequest request = new CreateJudgmentRequest(InputType.TEXT, "테스트 주장", null, null, null);

        CreateJudgmentResponse response = judgmentService.create(1L, request);

        assertEquals(JudgmentStatus.PROCESSING, response.status());
        verify(requestCountRepository).incrementCount(eq(1L), any(LocalDate.class));
        verify(entityManager, never()).persist(any());
    }

    @Test
    void 기존_카운트_증가_UPDATE가_0행이면_이상상황으로_예외를_던진다() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        JudgmentRequestCount existing = JudgmentRequestCount.create(1L, LocalDate.now());
        when(requestCountRepository.findByUserIdAndRequestDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));
        // PESSIMISTIC_WRITE로 잠근 행인데 UPDATE가 0행이면 정상 상황이 아니다.
        when(requestCountRepository.incrementCount(eq(1L), any(LocalDate.class))).thenReturn(0);

        CreateJudgmentRequest request = new CreateJudgmentRequest(InputType.TEXT, "테스트 주장", null, null, null);

        assertThrows(IllegalStateException.class, () -> judgmentService.create(1L, request));
        verify(judgmentRepository, never()).save(any());
    }

    @Test
    void 일일_한도를_이미_채웠으면_DAILY_LIMIT_EXCEEDED() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        JudgmentRequestCount maxedOut = JudgmentRequestCount.create(1L, LocalDate.now());
        for (int i = 0; i < DAILY_LIMIT; i++) {
            maxedOut.increment();
        }
        when(requestCountRepository.findByUserIdAndRequestDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(maxedOut));

        CreateJudgmentRequest request = new CreateJudgmentRequest(InputType.TEXT, "테스트 주장", null, null, null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> judgmentService.create(1L, request));

        assertEquals(JudgmentErrorCode.DAILY_LIMIT_EXCEEDED, exception.getErrorCode());
    }

    @Test
    void IMAGE_입력은_OCR_추출_실패시_거부되고_Judgment가_저장되지_않는다() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(requestCountRepository.findByUserIdAndRequestDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(clovaOcrClient.extractText(eq("base64data")))
                .thenThrow(new BusinessException(JudgmentErrorCode.EXTRACTION_FAILED));

        CreateJudgmentRequest request = new CreateJudgmentRequest(InputType.IMAGE, null, "base64data", null, null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> judgmentService.create(1L, request));

        assertEquals(JudgmentErrorCode.EXTRACTION_FAILED, exception.getErrorCode());
        verify(judgmentRepository, never()).save(any());
        // 추출 실패 시 일일 한도 예약분을 환불하는 트랜잭션이 실제로 실행됐는지 확인한다.
        verify(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void LINK_입력은_유튜브_추출_실패시_거부되고_Judgment가_저장되지_않는다() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(requestCountRepository.findByUserIdAndRequestDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(youtubeClient.extractText(eq("https://youtube.com/watch?v=x")))
                .thenThrow(new BusinessException(JudgmentErrorCode.EXTRACTION_FAILED));

        CreateJudgmentRequest request =
                new CreateJudgmentRequest(InputType.LINK, null, null, "https://youtube.com/watch?v=x", null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> judgmentService.create(1L, request));

        assertEquals(JudgmentErrorCode.EXTRACTION_FAILED, exception.getErrorCode());
        verify(judgmentRepository, never()).save(any());
        verify(transactionTemplate).executeWithoutResult(any());
    }

    @ParameterizedTest
    @EnumSource(TrustLevel.class)
    void AI_서비스가_성공하면_반환된_trustLevel_그대로_COMPLETED로_완료된다(TrustLevel trustLevel) throws Exception {
        Judgment judgment = Judgment.create(mock(User.class), InputType.TEXT, "테스트 주장", null, LocalDate.now());
        when(judgmentRepository.findById(10L)).thenReturn(Optional.of(judgment));
        stubAiServiceSuccess(aiResponse(trustLevel.name()));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        judgmentService.processAsync(10L);

        assertEquals(JudgmentStatus.COMPLETED, judgment.getStatus());
        assertEquals(trustLevel, judgment.getTrustLevel());
        assertEquals("이 주장이 사실일까?", judgment.getTitle());
    }

    @Test
    void AI_응답의_title이_비어있으면_FAILED_및_한도환불() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        LocalDate today = LocalDate.now();
        Judgment judgment = Judgment.create(user, InputType.TEXT, "테스트 주장", null, today);
        when(judgmentRepository.findById(13L)).thenReturn(Optional.of(judgment));

        AiJudgmentResponse blankTitleResponse = new AiJudgmentResponse(
                "  ",
                TrustLevel.PENDING.name(),
                "근거 요약",
                new AiJudgmentResponse.ConflictOfInterest(false, null, null),
                "안전 안내",
                List.of(),
                new AiJudgmentResponse.GuideCard("제목", "출처유형", "출처", List.of()));
        stubAiServiceSuccess(blankTitleResponse);

        judgmentService.processAsync(13L);

        assertEquals(JudgmentStatus.FAILED, judgment.getStatus());
        verify(requestCountRepository).decrementCount(1L, today);
    }

    @Test
    void title이_255코드포인트를_넘으면_서로게이트_쌍을_안_끊고_잘린다() {
        // 이모지(😀)는 UTF-16에서 서로게이트 쌍(char 2개)이라, char 인덱스로 substring하면
        // 쌍 중간이 잘려 깨진 문자가 남을 수 있다. 300개를 넣어 255 코드포인트 경계를 넘긴다.
        String longEmojiTitle = "😀".repeat(300);
        Judgment judgment = Judgment.create(mock(User.class), InputType.TEXT, "테스트 주장", null, LocalDate.now());
        when(judgmentRepository.findById(14L)).thenReturn(Optional.of(judgment));

        AiJudgmentResponse response = new AiJudgmentResponse(
                longEmojiTitle,
                TrustLevel.PENDING.name(),
                "근거 요약",
                new AiJudgmentResponse.ConflictOfInterest(false, null, null),
                "안전 안내",
                List.of(),
                new AiJudgmentResponse.GuideCard("제목", "출처유형", "출처", List.of()));
        stubAiServiceSuccess(response);

        judgmentService.processAsync(14L);

        assertEquals(JudgmentStatus.COMPLETED, judgment.getStatus());
        String title = judgment.getTitle();
        assertEquals(255, title.codePointCount(0, title.length()));
        // 서로게이트 쌍이 안 끊겼으면 문자열이 완전한 이모지들로만 구성돼 길이가 짝수(char 기준)여야 한다.
        assertEquals(0, title.length() % 2);
    }

    @Test
    void AI_서비스가_5xx를_반환하면_1회_재시도하고_그래도_실패하면_FAILED_및_한도환불() {
        assertRetriesOnceThenFails(HttpServerErrorException.create(
                HttpStatusCode.valueOf(503), "Service Unavailable", null, null, null));
    }

    @Test
    void AI_서비스가_타임아웃나면_1회_재시도하고_그래도_실패하면_FAILED_및_한도환불() {
        assertRetriesOnceThenFails(new ResourceAccessException("Read timed out"));
    }

    private void assertRetriesOnceThenFails(RuntimeException aiServiceFailure) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        // 일일 한도를 예약한 날짜(requestDate)와 실제 INSERT된 시각(createdAt)이 다른
        // 상황을 재현한다 — 추출이 자정을 걸쳐서 진행된 경우. 환불은 반드시 requestDate
        // 기준이어야 하고, createdAt을 참조하면 안 된다.
        LocalDate today = LocalDate.now();
        LocalDate nextDay = today.plusDays(1);
        Judgment judgment = Judgment.create(user, InputType.TEXT, "테스트 주장", null, today);
        ReflectionTestUtils.setField(judgment, "createdAt", nextDay.atStartOfDay());
        when(judgmentRepository.findById(12L)).thenReturn(Optional.of(judgment));

        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(aiServiceRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.body(any(AiJudgmentRequest.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AiJudgmentResponse.class)).thenThrow(aiServiceFailure);

        judgmentService.processAsync(12L);

        assertEquals(JudgmentStatus.FAILED, judgment.getStatus());
        assertEquals(JudgmentErrorCode.AI_SERVICE_UNAVAILABLE.getCode(), judgment.getFailureErrorCode());
        // 환불은 예약 때 실제로 썼던 날짜(today) 기준이어야 한다 — createdAt(nextDay) 아님.
        verify(requestCountRepository).decrementCount(1L, today);
        // 5xx/timeout은 1회만 재시도하므로 총 2번 호출된다.
        verify(responseSpec, times(2)).body(AiJudgmentResponse.class);
    }

    private void stubAiServiceSuccess(AiJudgmentResponse response) {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(aiServiceRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.body(any(AiJudgmentRequest.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AiJudgmentResponse.class)).thenReturn(response);
    }

    private AiJudgmentResponse aiResponse(String trustLevel) {
        return new AiJudgmentResponse(
                "이 주장이 사실일까?",
                trustLevel,
                "근거 요약",
                new AiJudgmentResponse.ConflictOfInterest(false, null, null),
                "안전 안내",
                List.of(),
                new AiJudgmentResponse.GuideCard("제목", "출처유형", "출처", List.of()));
    }

    @Test
    void 다른_유저의_판정을_조회하면_FORBIDDEN() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);

        Judgment judgment = mock(Judgment.class);
        when(judgment.getUser()).thenReturn(owner);
        when(judgmentRepository.findById(100L)).thenReturn(Optional.of(judgment));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> judgmentService.get(2L, 100L));

        assertEquals(CommonErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void 완료된_판정_조회시_title이_그대로_노출된다() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        Judgment judgment = Judgment.create(owner, InputType.TEXT, "테스트 주장", null, LocalDate.now());
        judgment.complete("이 주장이 사실일까?", TrustLevel.PENDING, "근거 요약",
                false, null, null, null, null, null, null);
        when(judgmentRepository.findById(20L)).thenReturn(Optional.of(judgment));

        JudgmentDetailResponse response = judgmentService.get(1L, 20L);

        assertEquals("이 주장이 사실일까?", response.title());
    }

    @Test
    void 처리중인_판정_조회시_title은_null이다() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        Judgment judgment = Judgment.create(owner, InputType.TEXT, "테스트 주장", null, LocalDate.now());
        when(judgmentRepository.findById(21L)).thenReturn(Optional.of(judgment));

        JudgmentDetailResponse response = judgmentService.get(1L, 21L);

        assertEquals(null, response.title());
    }

    @Test
    void 존재하지_않는_판정을_조회하면_JUDGMENT_NOT_FOUND() {
        when(judgmentRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> judgmentService.get(1L, 999L));

        assertEquals(JudgmentErrorCode.JUDGMENT_NOT_FOUND, exception.getErrorCode());
    }
}
