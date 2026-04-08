package top.hcode.hoj.service.oj;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.entity.common.TeamAward;

public interface TeamAwardService {

    CommonResult<IPage<TeamAward>> getTeamAwardList(Integer limit, Integer currentPage);

    CommonResult<Integer> getTeamAwardPageSize();
}
