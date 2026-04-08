package top.hcode.hoj.controller.oj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.manager.oj.DailyProblemManager;
import top.hcode.hoj.pojo.vo.DailyProblemVO;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.ShiroUtils;

@RestController
@RequestMapping("/api")
public class DailyProblemController {

    @Autowired
    private DailyProblemManager dailyProblemManager;

    @GetMapping("/daily-problem")
    public CommonResult<DailyProblemVO> getDailyProblem() {
        AccountProfile profile = ShiroUtils.getProfile();
        String uid = profile == null ? null : profile.getUid();
        return CommonResult.successResponse(dailyProblemManager.getDailyProblem(uid));
    }

    @GetMapping("/daily-problem/next")
    public CommonResult<DailyProblemVO> getNextProblem(
            @RequestParam(value = "index", required = false) Integer index,
            @RequestParam(value = "currentPid", required = false) Long currentPid) {
        AccountProfile profile = ShiroUtils.getProfile();
        String uid = profile == null ? null : profile.getUid();
        return CommonResult.successResponse(dailyProblemManager.getNextProblem(uid, index, currentPid));
    }
}
