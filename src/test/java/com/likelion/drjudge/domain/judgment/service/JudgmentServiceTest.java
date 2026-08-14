package com.likelion.drjudge.domain.judgment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.drjudge.domain.category.repository.CategoryRepository;
import com.likelion.drjudge.domain.judgment.entity.Judgment;
import com.likelion.drjudge.domain.judgment.exception.JudgmentErrorCode;
import com.likelion.drjudge.domain.judgment.repository.JudgmentRepository;
import com.likelion.drjudge.domain.judgment.repository.JudgmentRequestCountRepository;
import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.domain.user.repository.UserRepository;
import com.likelion.drjudge.global.exception.BusinessException;
import com.likelion.drjudge.global.exception.CommonErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class JudgmentServiceTest {

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
