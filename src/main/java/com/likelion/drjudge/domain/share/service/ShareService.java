package com.likelion.drjudge.domain.share.service;

import com.likelion.drjudge.domain.judgment.entity.Judgment;
import com.likelion.drjudge.domain.judgment.entity.JudgmentStatus;
import com.likelion.drjudge.domain.judgment.exception.JudgmentErrorCode;
import com.likelion.drjudge.domain.judgment.repository.JudgmentRepository;
import com.likelion.drjudge.domain.share.dto.response.ShareLinkResponse;
import com.likelion.drjudge.domain.share.dto.response.SharedJudgmentResponse;
import com.likelion.drjudge.domain.share.entity.ShareLink;
import com.likelion.drjudge.domain.share.exception.ShareErrorCode;
import com.likelion.drjudge.domain.share.repository.ShareLinkRepository;
import com.likelion.drjudge.global.exception.BusinessException;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShareService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final int MAX_TOKEN_RETRY = 5;

    private final ShareLinkRepository shareLinkRepository;
    private final JudgmentRepository judgmentRepository;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    /** POST /judgments/{judgmentId}/share */
    @Transactional
    public ShareLinkResponse createShareLink(Long userId, Long judgmentId) {
        Judgment judgment = judgmentRepository.findById(judgmentId)
                .orElseThrow(() -> new BusinessException(JudgmentErrorCode.JUDGMENT_NOT_FOUND));

        if (!judgment.getUser().getId().equals(userId)) {
            throw new BusinessException(ShareErrorCode.NOT_JUDGMENT_OWNER);
        }
        if (judgment.getStatus() != JudgmentStatus.COMPLETED) {
            throw new BusinessException(ShareErrorCode.JUDGMENT_NOT_COMPLETED);
        }

        String token = generateUniqueToken();
        ShareLink shareLink = ShareLink.create(judgment, judgment.getUser(), token);
        shareLinkRepository.save(shareLink);

        return ShareLinkResponse.of(token, frontendBaseUrl);
    }

    /** GET /share/{token} — 비회원도 접근 가능(인증 불필요) */
    public SharedJudgmentResponse getSharedJudgment(String token) {
        ShareLink shareLink = shareLinkRepository.findByTokenAndIsActiveTrue(token)
                .orElseThrow(() -> new BusinessException(ShareErrorCode.SHARE_LINK_NOT_FOUND));

        return SharedJudgmentResponse.from(shareLink.getJudgment());
    }

    /** DELETE /judgments/{judgmentId}/share — 공유링크 회수 */
    @Transactional
    public void revokeShareLink(Long userId, Long judgmentId) {
        ShareLink shareLink = shareLinkRepository
                .findFirstByJudgmentIdAndUserIdAndIsActiveTrueOrderByCreatedAtDesc(judgmentId, userId)
                .orElseThrow(() -> new BusinessException(ShareErrorCode.NO_ACTIVE_SHARE_LINK));

        shareLink.revoke();
    }

    private String generateUniqueToken() {
        for (int i = 0; i < MAX_TOKEN_RETRY; i++) {
            String token = generateToken();
            if (!shareLinkRepository.existsByToken(token)) {
                return token;
            }
        }
        throw new IllegalStateException("공유 토큰 생성에 반복적으로 실패했습니다.");
    }

    private String generateToken() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(32);
        for (byte b : bytes) {
            sb.append(HEX_CHARS[(b >> 4) & 0xF]);
            sb.append(HEX_CHARS[b & 0xF]);
        }
        return sb.toString();
    }
}