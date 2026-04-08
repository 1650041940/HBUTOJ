package top.hcode.hoj.manager.oj;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.dao.common.TeamAwardConfigEntityService;
import top.hcode.hoj.dao.common.TeamAwardEntityService;
import top.hcode.hoj.pojo.entity.common.TeamAward;
import top.hcode.hoj.pojo.entity.common.TeamAwardConfig;

import java.util.Collections;
import java.util.Date;

@Component
public class TeamAwardManager {

    private static final int DEFAULT_PAGE_SIZE = 6;

    private static final Logger log = LoggerFactory.getLogger(TeamAwardManager.class);

    @Autowired
    private TeamAwardEntityService teamAwardEntityService;

    @Autowired
    private TeamAwardConfigEntityService teamAwardConfigEntityService;

    public IPage<TeamAward> getTeamAwardList(Integer limit, Integer currentPage) {
        if (currentPage == null || currentPage < 1) {
            currentPage = 1;
        }
        if (limit == null || limit < 1) {
            limit = getTeamAwardPageSize();
        }

        Page<TeamAward> page = new Page<>(currentPage, limit);
        QueryWrapper<TeamAward> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0)
                .orderByDesc("award_time")
                .orderByDesc("id");
        try {
            return teamAwardEntityService.page(page, wrapper);
        } catch (Exception e) {
            log.error("Failed to query team_award list, fallback to empty page. message={}", e.getMessage());
            page.setTotal(0);
            page.setRecords(Collections.emptyList());
            return page;
        }
    }

    public IPage<TeamAward> getAdminTeamAwardList(Integer limit, Integer currentPage, String keyword) {
        if (currentPage == null || currentPage < 1) {
            currentPage = 1;
        }
        if (limit == null || limit < 1) {
            limit = 20;
        }

        Page<TeamAward> page = new Page<>(currentPage, limit);
        QueryWrapper<TeamAward> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(keyword)) {
            wrapper.and(w -> w.like("title", keyword).or().like("contest_name", keyword).or().like("award", keyword));
        }
        wrapper.orderByDesc("award_time")
                .orderByDesc("id");
        try {
            return teamAwardEntityService.page(page, wrapper);
        } catch (Exception e) {
            log.error("Failed to query admin team_award list, fallback to empty page. message={}", e.getMessage());
            page.setTotal(0);
            page.setRecords(Collections.emptyList());
            return page;
        }
    }

    public void addTeamAward(TeamAward teamAward) throws StatusFailException {
        if (teamAward == null) {
            throw new StatusFailException("参数不能为空");
        }
        if (teamAward.getStatus() == null) {
            teamAward.setStatus(0);
        }
        if (teamAward.getAwardTime() == null) {
            teamAward.setAwardTime(new Date());
        }
        try {
            boolean ok = teamAwardEntityService.save(teamAward);
            if (!ok) {
                throw new StatusFailException("新增获奖记录失败");
            }
        } catch (Exception e) {
            throw new StatusFailException("数据库未升级或表不存在，请先执行数据库升级脚本后重试！");
        }
    }

    public void updateTeamAward(TeamAward teamAward) throws StatusFailException {
        if (teamAward == null || teamAward.getId() == null) {
            throw new StatusFailException("参数不完整");
        }
        try {
            boolean ok = teamAwardEntityService.updateById(teamAward);
            if (!ok) {
                throw new StatusFailException("更新获奖记录失败");
            }
        } catch (Exception e) {
            throw new StatusFailException("数据库未升级或表不存在，请先执行数据库升级脚本后重试！");
        }
    }

    public void deleteTeamAward(Long id) throws StatusFailException {
        if (id == null) {
            throw new StatusFailException("参数不完整");
        }
        try {
            boolean ok = teamAwardEntityService.removeById(id);
            if (!ok) {
                throw new StatusFailException("删除获奖记录失败");
            }
        } catch (Exception e) {
            throw new StatusFailException("数据库未升级或表不存在，请先执行数据库升级脚本后重试！");
        }
    }

    public Integer getTeamAwardPageSize() {
        try {
            TeamAwardConfig config = teamAwardConfigEntityService.getById(1L);
            if (config == null) {
                config = new TeamAwardConfig().setId(1L).setPageSize(DEFAULT_PAGE_SIZE);
                teamAwardConfigEntityService.save(config);
            }
            if (config.getPageSize() == null || config.getPageSize() < 1) {
                return DEFAULT_PAGE_SIZE;
            }
            return config.getPageSize();
        } catch (Exception e) {
            log.error("Failed to query team_award_config page_size, fallback to default. message={}", e.getMessage());
            return DEFAULT_PAGE_SIZE;
        }
    }

    public void setTeamAwardPageSize(Integer pageSize) throws StatusFailException {
        if (pageSize == null || pageSize < 1 || pageSize > 50) {
            throw new StatusFailException("每页数量不合法(1-50)");
        }
        try {
            TeamAwardConfig config = new TeamAwardConfig().setId(1L).setPageSize(pageSize);
            teamAwardConfigEntityService.saveOrUpdate(config);
        } catch (Exception e) {
            throw new StatusFailException("数据库未升级或表不存在，请先执行数据库升级脚本后重试！");
        }
    }
}
