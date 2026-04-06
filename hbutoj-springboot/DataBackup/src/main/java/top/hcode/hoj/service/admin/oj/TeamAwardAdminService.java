package top.hcode.hoj.service.admin.oj;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.entity.common.TeamAward;

public interface TeamAwardAdminService {

    CommonResult<IPage<TeamAward>> getTeamAwardList(Integer limit, Integer currentPage, String keyword);

    CommonResult<Void> addTeamAward(TeamAward teamAward);

    CommonResult<Void> updateTeamAward(TeamAward teamAward);

    CommonResult<Void> deleteTeamAward(Long id);

    CommonResult<Integer> getTeamAwardPageSize();

    CommonResult<Void> setTeamAwardPageSize(Integer pageSize);
}
