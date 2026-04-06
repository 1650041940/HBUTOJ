package top.hcode.hoj.controller.oj;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.vo.RatingRankVO;
import top.hcode.hoj.pojo.vo.UserRatingVO;
import top.hcode.hoj.service.oj.RatingService;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api")
public class RatingController {

    @Resource
    private RatingService ratingService;

    @GetMapping("/rating/get-my")
    public CommonResult<UserRatingVO> getMyRating() {
        return ratingService.getMyRating();
    }

    @GetMapping("/rating/contest-rank")
    public CommonResult<IPage<RatingRankVO>> getContestRatingRank(@RequestParam(value = "limit", required = false) Integer limit,
                                                                  @RequestParam(value = "currentPage", required = false) Integer currentPage,
                                                                  @RequestParam(value = "searchUser", required = false) String searchUser) {
        return ratingService.getContestRatingRank(limit, currentPage, searchUser);
    }

    @GetMapping("/rating/practice-rank")
    public CommonResult<IPage<RatingRankVO>> getPracticeRatingRank(@RequestParam(value = "limit", required = false) Integer limit,
                                                                   @RequestParam(value = "currentPage", required = false) Integer currentPage,
                                                                   @RequestParam(value = "searchUser", required = false) String searchUser) {
        return ratingService.getPracticeRatingRank(limit, currentPage, searchUser);
    }
}
