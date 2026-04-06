package top.hcode.hoj.service.admin.oj.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.manager.oj.TeamAwardManager;
import top.hcode.hoj.pojo.entity.common.TeamAward;
import top.hcode.hoj.service.admin.oj.TeamAwardAdminService;

import javax.annotation.Resource;

@Service
public class TeamAwardAdminServiceImpl implements TeamAwardAdminService {

    @Resource
    private TeamAwardManager teamAwardManager;

    @Override
    public CommonResult<IPage<TeamAward>> getTeamAwardList(Integer limit, Integer currentPage, String keyword) {
        return CommonResult.successResponse(teamAwardManager.getAdminTeamAwardList(limit, currentPage, keyword));
    }

    @Override
    public CommonResult<Void> addTeamAward(TeamAward teamAward) {
        try {
            teamAwardManager.addTeamAward(teamAward);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (Exception e) {
            return CommonResult.errorResponse("Server Error!");
        }
    }

    @Override
    public CommonResult<Void> updateTeamAward(TeamAward teamAward) {
        try {
            teamAwardManager.updateTeamAward(teamAward);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (Exception e) {
            return CommonResult.errorResponse("Server Error!");
        }
    }

    @Override
    public CommonResult<Void> deleteTeamAward(Long id) {
        try {
            teamAwardManager.deleteTeamAward(id);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (Exception e) {
            return CommonResult.errorResponse("Server Error!");
        }
    }

    @Override
    public CommonResult<Integer> getTeamAwardPageSize() {
        try {
            return CommonResult.successResponse(teamAwardManager.getTeamAwardPageSize());
        } catch (Exception e) {
            return CommonResult.successResponse(6);
        }
    }

    @Override
    public CommonResult<Void> setTeamAwardPageSize(Integer pageSize) {
        try {
            teamAwardManager.setTeamAwardPageSize(pageSize);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (Exception e) {
            return CommonResult.errorResponse("Server Error!");
        }
    }
}
