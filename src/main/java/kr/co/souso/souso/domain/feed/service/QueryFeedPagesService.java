package kr.co.souso.souso.domain.feed.service;

import kr.co.souso.souso.domain.category.domain.repository.vo.CategoryVO;
import kr.co.souso.souso.domain.category.presentation.dto.response.CategoryResponse;
import kr.co.souso.souso.domain.feed.domain.FeedImage;
import kr.co.souso.souso.domain.feed.domain.repository.FeedImageRepository;
import kr.co.souso.souso.domain.feed.domain.repository.FeedRepository;
import kr.co.souso.souso.domain.feed.domain.repository.vo.FeedDetailsVO;
import kr.co.souso.souso.domain.feed.exception.FeedNotFoundException;
import kr.co.souso.souso.domain.feed.presentation.dto.response.QueryFeedDetailsResponse;
import kr.co.souso.souso.domain.feed.presentation.dto.response.QueryFeedPagesResponse;
import kr.co.souso.souso.domain.user.domain.User;
import kr.co.souso.souso.domain.user.domain.repository.vo.AuthorVO;
import kr.co.souso.souso.domain.user.facade.UserFacade;
import kr.co.souso.souso.domain.user.presentation.dto.response.AuthorResponse;
import kr.co.souso.souso.domain.viewcount.domain.FeedViewCount;
import kr.co.souso.souso.domain.viewcount.domain.repository.FeedViewCountRepository;
import kr.co.souso.souso.global.enums.SortPageType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class QueryFeedPagesService {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final FeedRepository feedRepository;
    private final FeedImageRepository feedImageRepository;
    private final FeedViewCountRepository feedViewCountRepository;
    private final UserFacade userFacade;

    @Transactional(readOnly = true)
    public QueryFeedPagesResponse execute(Long cursorId, Integer pageId, SortPageType sortType) {
        User user = userFacade.getCurrentUser();

        Slice<FeedDetailsVO> feedList = feedRepository.queryFeedPages(user.getId(), cursorId, pageId, sortType, PageRequest.of(0, DEFAULT_PAGE_SIZE));

        List<QueryFeedDetailsResponse> queryFeedDetailsResponseList = new ArrayList<>();

        for (FeedDetailsVO feedDetailsVO : feedList) {
            List<String> imageUrl = feedImageRepository.findByFeedId(feedDetailsVO.getFeedId())
                    .stream()
                    .map(FeedImage::getImageUrl)
                    .collect(Collectors.toList());

            FeedViewCount feedViewCount = feedViewCountRepository.findById(feedDetailsVO.getFeedId())
                    .orElseThrow(() -> FeedNotFoundException.EXCEPTION);

            queryFeedDetailsResponseList.add(
                    buildFeedDetailsResponse(feedDetailsVO, imageUrl,feedViewCount.getViewCount())
            );
        }

        return new QueryFeedPagesResponse(queryFeedDetailsResponseList, feedList.hasNext(), queryFeedDetailsResponseList.size());
    }


    private CategoryResponse buildCategoryResponse(CategoryVO categoryVO) {
        return CategoryResponse.builder()
                .categoryId(categoryVO.getCategoryId())
                .categoryName(categoryVO.getCategoryName())
                .build();
    }

    private AuthorResponse buildAuthorResponse(AuthorVO authorVO) {
        return AuthorResponse.builder()
                .userId(authorVO.getUserId())
                .nickname(authorVO.getNickname())
                .birth(authorVO.getBirth())
                .profileImageUrl(authorVO.getProfileImageUrl())
                .build();
    }

    private QueryFeedDetailsResponse buildFeedDetailsResponse(FeedDetailsVO feedDetailsVO, List<String> imageUrl, Long feedViewCount) {
        return QueryFeedDetailsResponse.builder()
                .category(buildCategoryResponse(feedDetailsVO.getCategoryVO()))
                .author(buildAuthorResponse(feedDetailsVO.getAuthorVO()))
                .feedId(feedDetailsVO.getFeedId())
                .imageUrl(imageUrl)
                .createdAt(feedDetailsVO.getCreatedAt())
                .content(feedDetailsVO.getContent())
                .isLike(feedDetailsVO.getIsLike())
                .isBookmark(feedDetailsVO.getIsBookmark())
                .likeCount(feedDetailsVO.getLikeCount())
                .bookmarkCount(feedDetailsVO.getBookmarkCount())
                .viewCount(feedViewCount)
                .build();
    }
}