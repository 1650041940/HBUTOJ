package top.hcode.hoj.controller.oj;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.hcode.hoj.annotation.AnonApi;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.entity.common.TeamAward;
import top.hcode.hoj.service.oj.TeamAwardService;

@RestController
@RequestMapping("/api")
@AnonApi
public class TeamAwardController {

    @Autowired
    private TeamAwardService teamAwardService;

    @GetMapping("/team-award")
    public CommonResult<IPage<TeamAward>> getTeamAwardList(@RequestParam(value = "limit", required = false) Integer limit,
                                                           @RequestParam(value = "currentPage", required = false) Integer currentPage) {
        return teamAwardService.getTeamAwardList(limit, currentPage);
    }

    @GetMapping("/team-award/page-size")
    public CommonResult<Integer> getTeamAwardPageSize() {
        return teamAwardService.getTeamAwardPageSize();
    }
}
