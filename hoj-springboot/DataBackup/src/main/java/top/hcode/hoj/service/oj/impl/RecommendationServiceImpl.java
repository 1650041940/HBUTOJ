package top.hcode.hoj.service.oj.impl;

import org.springframework.stereotype.Service;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.manager.rating.RecommendationManager;
import top.hcode.hoj.pojo.vo.RecommendProblemVO;
import top.hcode.hoj.service.oj.RecommendationService;

import javax.annotation.Resource;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    @Resource
    private RecommendationManager recommendationManager;

    @Override
    public CommonResult<RecommendProblemVO> getDailyProblem(Long excludePid) {
        try {
            return CommonResult.successResponse(recommendationManager.getDailyRecommend(excludePid));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }
}
