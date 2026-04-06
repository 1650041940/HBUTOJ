package top.hcode.hoj.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.entity.common.TeamAward;
import top.hcode.hoj.service.admin.oj.TeamAwardAdminService;

@RestController
@RequestMapping("/api/admin/team-award")
public class TeamAwardAdminController {

    @Autowired
    private TeamAwardAdminService teamAwardAdminService;

    @RequiresRoles(value = {"root", "admin"}, logical = Logical.OR)
    @GetMapping("/list")
    public CommonResult<IPage<TeamAward>> getTeamAwardList(@RequestParam(value = "limit", required = false) Integer limit,
                                                           @RequestParam(value = "currentPage", required = false) Integer currentPage,
                                                           @RequestParam(value = "keyword", required = false) String keyword) {
        return teamAwardAdminService.getTeamAwardList(limit, currentPage, keyword);
    }

    @RequiresRoles(value = {"root", "admin"}, logical = Logical.OR)
    @PostMapping("/add")
    public CommonResult<Void> addTeamAward(@RequestBody TeamAward teamAward) {
        return teamAwardAdminService.addTeamAward(teamAward);
    }

    @RequiresRoles(value = {"root", "admin"}, logical = Logical.OR)
    @PutMapping("/update")
    public CommonResult<Void> updateTeamAward(@RequestBody TeamAward teamAward) {
        return teamAwardAdminService.updateTeamAward(teamAward);
    }

    @RequiresRoles(value = {"root", "admin"}, logical = Logical.OR)
    @DeleteMapping("/delete")
    public CommonResult<Void> deleteTeamAward(@RequestParam("id") Long id) {
        return teamAwardAdminService.deleteTeamAward(id);
    }

    @RequiresRoles(value = {"root", "admin"}, logical = Logical.OR)
    @GetMapping("/page-size")
    public CommonResult<Integer> getTeamAwardPageSize() {
        return teamAwardAdminService.getTeamAwardPageSize();
    }

    @RequiresRoles(value = {"root", "admin"}, logical = Logical.OR)
    @PutMapping("/page-size")
    public CommonResult<Void> setTeamAwardPageSize(@RequestParam("pageSize") Integer pageSize) {
        return teamAwardAdminService.setTeamAwardPageSize(pageSize);
    }
}
