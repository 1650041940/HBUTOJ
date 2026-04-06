package top.hcode.hoj.service.oj.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.manager.rating.RatingManager;
import top.hcode.hoj.mapper.UserContestRatingMapper;
import top.hcode.hoj.mapper.UserPracticeRatingMapper;
import top.hcode.hoj.pojo.vo.RatingRankVO;
import top.hcode.hoj.pojo.vo.UserRatingVO;
import top.hcode.hoj.service.oj.RatingService;

import javax.annotation.Resource;

@Service
public class RatingServiceImpl implements RatingService {

    @Resource
    private RatingManager ratingManager;

    @Resource
    private UserContestRatingMapper userContestRatingMapper;

    @Resource
    private UserPracticeRatingMapper userPracticeRatingMapper;

    @Override
    public CommonResult<UserRatingVO> getMyRating() {
        try {
            return CommonResult.successResponse(ratingManager.getMyRating());
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<IPage<RatingRankVO>> getContestRatingRank(Integer limit, Integer currentPage, String searchUser) {
        if (currentPage == null || currentPage < 1) currentPage = 1;
        if (limit == null || limit < 1) limit = 50;
        Page<RatingRankVO> page = new Page<>(currentPage, limit);
        return CommonResult.successResponse(userContestRatingMapper.getContestRatingRank(page, searchUser));
    }

    @Override
    public CommonResult<IPage<RatingRankVO>> getPracticeRatingRank(Integer limit, Integer currentPage, String searchUser) {
        if (currentPage == null || currentPage < 1) currentPage = 1;
        if (limit == null || limit < 1) limit = 50;
        Page<RatingRankVO> page = new Page<>(currentPage, limit);
        return CommonResult.successResponse(userPracticeRatingMapper.getPracticeRatingRank(page, searchUser));
    }
}
