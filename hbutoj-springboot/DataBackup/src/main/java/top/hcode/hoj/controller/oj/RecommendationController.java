package top.hcode.hoj.controller.oj;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.vo.RecommendProblemVO;
import top.hcode.hoj.service.oj.RecommendationService;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api")
public class RecommendationController {

    @Resource
    private RecommendationService recommendationService;

    @GetMapping("/recommend/daily")
    public CommonResult<RecommendProblemVO> getDailyProblem(@RequestParam(value = "excludePid", required = false) Long excludePid) {
        return recommendationService.getDailyProblem(excludePid);
    }
}
