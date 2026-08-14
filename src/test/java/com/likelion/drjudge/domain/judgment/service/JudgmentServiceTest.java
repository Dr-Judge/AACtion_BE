package com.likelion.drjudge.domain.judgment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.drjudge.domain.category.repository.CategoryRepository;
import com.likelion.drjudge.domain.judgment.dto.ai.AiJudgmentRequest;
import com.likelion.drjudge.domain.judgment.dto.ai.AiJudgmentResponse;
import com.likelion.drjudge.domain.judgment.dto.request.CreateJudgmentRequest;
import com.likelion.drjudge.domain.judgment.dto.response.CreateJudgmentResponse;
import com.likelion.drjudge.domain.judgment.entity.InputType;
import com.likelion.drjudge.domain.judgment.entity.Judgment;
import com.likelion.drjudge.domain.judgment.entity.JudgmentRequestCount;
import com.likelion.drjudge.domain.judgment.entity.JudgmentStatus;
import com.likelion.drjudge.domain.judgment.exception.JudgmentErrorCode;
import com.likelion.drjudge.domain.judgment.repository.JudgmentRepository;
import com.likelion.drjudge.domain.judgment.repository.JudgmentRequestCountRepository;
import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.domain.user.repository.UserRepository;
import com.likelion.drjudge.global.constant.TrustLevel;
import com.likelion.drjudge.global.exception.BusinessException;
import com.likelion.drjudge.global.exception.CommonErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
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

    @InjectMocks
    private JudgmentService judgmentService;

    @BeforeEach
    void setUp() {
        // @Value는 순수 Mockito 테스트에선 주입 안 되니 수동으로 세팅한다.
        ReflectionTestUtils.setField(judgmentService, "dailyLimit", DAILY_LIMIT);
    }

    @Test
    void 판정_생성에_성공하면_PROCESSING_상태를_반환하고_일일카운트를_증가시킨다() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(requestCountRepository.findByUserIdAndRequestDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(judgmentRepository.save(any(Judgment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateJudgmentRequest request = new CreateJudgmentRequest(InputType.TEXT, "테스트 주장", null, null, null);

        CreateJudgmentResponse response = judgmentService.create(1L, request);

        assertEquals(JudgmentStatus.PROCESSING, response.status());
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
    void AI_서비스가_PENDING을_반환하면_판단보류로_완료된다() throws Exception {
        Judgment judgment = Judgment.create(mock(User.class), InputType.TEXT, "테스트 주장", null);
        when(judgmentRepository.findById(10L)).thenReturn(Optional.of(judgment));
        stubAiServiceSuccess(aiResponse("PENDING"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        judgmentService.processAsync(10L);

        assertEquals(JudgmentStatus.COMPLETED, judgment.getStatus());
        assertEquals(TrustLevel.PENDING, judgment.getTrustLevel());
    }

    @Test
    void AI_서비스가_COUNTER_EVIDENCE를_반환하면_반박근거있음으로_완료된다() throws Exception {
        Judgment judgment = Judgment.create(mock(User.class), InputType.TEXT, "테스트 주장", null);
        when(judgmentRepository.findById(11L)).thenReturn(Optional.of(judgment));
        stubAiServiceSuccess(aiResponse("COUNTER_EVIDENCE"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        judgmentService.processAsync(11L);

        assertEquals(JudgmentStatus.COMPLETED, judgment.getStatus());
        assertEquals(TrustLevel.COUNTER_EVIDENCE, judgment.getTrustLevel());
    }

    @Test
    void AI_서비스가_5xx를_반환하면_1회_재시도하고_그래도_실패하면_FAILED_및_한도환불() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        Judgment judgment = Judgment.create(user, InputType.TEXT, "테스트 주장", null);
        LocalDate today = LocalDate.now();
        ReflectionTestUtils.setField(judgment, "createdAt", today.atStartOfDay());
        when(judgmentRepository.findById(12L)).thenReturn(Optional.of(judgment));

        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(aiServiceRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.body(any(AiJudgmentRequest.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AiJudgmentResponse.class))
                .thenThrow(HttpServerErrorException.create(
                        HttpStatusCode.valueOf(503), "Service Unavailable", null, null, null));

        JudgmentRequestCount countRow = JudgmentRequestCount.create(1L, today);
        countRow.increment();
        when(requestCountRepository.findByUserIdAndRequestDate(1L, today)).thenReturn(Optional.of(countRow));

        judgmentService.processAsync(12L);

        assertEquals(JudgmentStatus.FAILED, judgment.getStatus());
        assertEquals(JudgmentErrorCode.AI_SERVICE_UNAVAILABLE.getCode(), judgment.getFailureErrorCode());
        assertEquals(0, countRow.getRequestCount());
        // 5xx는 1회만 재시도하므로 총 2번 호출된다.
        org.mockito.Mockito.verify(responseSpec, org.mockito.Mockito.times(2)).body(AiJudgmentResponse.class);
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
    void 존재하지_않는_판정을_조회하면_JUDGMENT_NOT_FOUND() {
        when(judgmentRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> judgmentService.get(1L, 999L));

        assertEquals(JudgmentErrorCode.JUDGMENT_NOT_FOUND, exception.getErrorCode());
    }
}
