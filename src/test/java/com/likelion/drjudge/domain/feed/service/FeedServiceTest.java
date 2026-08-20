package com.likelion.drjudge.domain.feed.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.likelion.drjudge.domain.category.entity.Category;
import com.likelion.drjudge.domain.feed.dto.response.FeedPostPageResponse;
import com.likelion.drjudge.domain.feed.entity.FeedLike;
import com.likelion.drjudge.domain.feed.entity.FeedLikeId;
import com.likelion.drjudge.domain.feed.entity.FeedPost;
import com.likelion.drjudge.domain.feed.repository.FeedLikeRepository;
import com.likelion.drjudge.domain.feed.repository.FeedPostRepository;
import com.likelion.drjudge.domain.judgment.entity.Judgment;
import com.likelion.drjudge.domain.judgment.entity.InputType;
import com.likelion.drjudge.domain.judgment.repository.JudgmentRepository;
import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.global.constant.TrustLevel;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private FeedPostRepository feedPostRepository;
    @Mock
    private JudgmentRepository judgmentRepository;
    @Mock
    private FeedLikeRepository feedLikeRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private FeedService feedService;

    private FeedPost feedPost;

    @BeforeEach
    void setUp() {
        User author = mock(User.class);
        when(author.getId()).thenReturn(1L);
        when(author.getNickname()).thenReturn("닉네임");

        Category category = mock(Category.class);
        when(category.getCode()).thenReturn("NUTRITION");

        Judgment judgment = Judgment.create(author, InputType.TEXT, "테스트 주장", category, LocalDate.now());
        judgment.complete("이 주장이 사실일까?", TrustLevel.CLINICAL_EVIDENCE, "근거 요약",
                false, null, null, null, null, null, null);

        feedPost = FeedPost.create(judgment, author);
        ReflectionTestUtils.setField(feedPost, "id", 100L);
    }

    @Test
    void 공개_피드_조회시_title_summary_trustLevelLabel이_그대로_노출된다() {
        Page<FeedPost> page = new PageImpl<>(List.of(feedPost), PageRequest.of(0, 20), 1);
        when(feedPostRepository.findByIsPublicTrueOrderByCreatedAtDescIdDesc(any())).thenReturn(page);
        when(feedLikeRepository.findByIdUserIdAndIdFeedPostIdIn(anyLong(), any())).thenReturn(List.of());

        FeedPostPageResponse response = feedService.getFeedPosts(2L, "recent", 1, 20);

        assertEquals(1, response.items().size());
        var item = response.items().get(0);
        assertEquals("이 주장이 사실일까?", item.title());
        assertEquals("근거 요약", item.summary());
        assertEquals("임상적 근거 있음", item.trustLevelLabel());
        assertEquals("NUTRITION", item.category());
        assertEquals("닉네임", item.author().nickname());
        assertFalse(item.liked());
    }

    @Test
    void 좋아요_누른_게시물은_liked가_true로_내려간다() {
        Page<FeedPost> page = new PageImpl<>(List.of(feedPost), PageRequest.of(0, 20), 1);
        when(feedPostRepository.findByIsPublicTrueOrderByCreatedAtDescIdDesc(any())).thenReturn(page);

        FeedLike like = FeedLike.create(100L, 2L);
        when(feedLikeRepository.findByIdUserIdAndIdFeedPostIdIn(eq(2L), any())).thenReturn(List.of(like));

        FeedPostPageResponse response = feedService.getFeedPosts(2L, "recent", 1, 20);

        assertTrue(response.items().get(0).liked());
    }

    @Test
    void popular_정렬이면_좋아요순_리포지토리_메서드를_쓴다() {
        Page<FeedPost> page = new PageImpl<>(List.of(feedPost), PageRequest.of(0, 20), 1);
        when(feedPostRepository.findByIsPublicTrueOrderByLikeCountDescIdDesc(any())).thenReturn(page);
        when(feedLikeRepository.findByIdUserIdAndIdFeedPostIdIn(anyLong(), any())).thenReturn(List.of());

        feedService.getFeedPosts(2L, "popular", 1, 20);

        org.mockito.Mockito.verify(feedPostRepository).findByIsPublicTrueOrderByLikeCountDescIdDesc(any());
        org.mockito.Mockito.verify(feedPostRepository, org.mockito.Mockito.never())
                .findByIsPublicTrueOrderByCreatedAtDescIdDesc(any());
    }
}
