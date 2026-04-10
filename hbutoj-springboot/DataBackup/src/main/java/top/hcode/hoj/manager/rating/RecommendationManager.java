package top.hcode.hoj.manager.rating;

import cn.hutool.core.date.DateUtil;
import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Component;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.mapper.RecommendationMapper;
import top.hcode.hoj.mapper.UserPracticeRatingMapper;
import top.hcode.hoj.pojo.entity.rating.UserPracticeRating;
import top.hcode.hoj.pojo.vo.RecommendProblemVO;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.RatingUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class RecommendationManager {

    @Resource
    private RecommendationMapper recommendationMapper;

    @Resource
    private UserPracticeRatingMapper userPracticeRatingMapper;

    public RecommendProblemVO getDailyRecommend(Long excludePid) throws StatusFailException {
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        if (userRolesVo == null) {
            throw new StatusFailException("请先登录");
        }
        String uid = userRolesVo.getUid();

        UserPracticeRating rating = userPracticeRatingMapper.selectById(uid);
        int target = rating == null || rating.getRating() == null
                ? RatingUtils.DEFAULT_PRACTICE_RATING
                : rating.getRating();

        List<RecommendProblemVO> candidates = recommendationMapper.getRecommendCandidates(uid, target, excludePid, 50);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        String today = DateUtil.format(DateUtil.date(), "yyyy-MM-dd");
        int idx = Math.floorMod(Objects.hash(uid, today, excludePid), candidates.size());
        return candidates.get(idx);
    }
}
