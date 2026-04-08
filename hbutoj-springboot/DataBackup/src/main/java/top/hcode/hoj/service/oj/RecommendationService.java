package top.hcode.hoj.service.oj;

import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.vo.RecommendProblemVO;

public interface RecommendationService {

    CommonResult<RecommendProblemVO> getDailyProblem(Long excludePid);
}
