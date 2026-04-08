package top.hcode.hoj.service.oj.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.manager.oj.TeamAwardManager;
import top.hcode.hoj.pojo.entity.common.TeamAward;
import top.hcode.hoj.service.oj.TeamAwardService;

import javax.annotation.Resource;

@Service
public class TeamAwardServiceImpl implements TeamAwardService {

    @Resource
    private TeamAwardManager teamAwardManager;

    @Override
    public CommonResult<IPage<TeamAward>> getTeamAwardList(Integer limit, Integer currentPage) {
        return CommonResult.successResponse(teamAwardManager.getTeamAwardList(limit, currentPage));
    }

    @Override
    public CommonResult<Integer> getTeamAwardPageSize() {
        return CommonResult.successResponse(teamAwardManager.getTeamAwardPageSize());
    }
}
