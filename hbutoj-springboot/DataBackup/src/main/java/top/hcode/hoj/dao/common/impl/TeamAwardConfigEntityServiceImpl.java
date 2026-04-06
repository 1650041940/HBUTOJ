package top.hcode.hoj.dao.common.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.hcode.hoj.dao.common.TeamAwardConfigEntityService;
import top.hcode.hoj.mapper.TeamAwardConfigMapper;
import top.hcode.hoj.pojo.entity.common.TeamAwardConfig;

@Service
public class TeamAwardConfigEntityServiceImpl extends ServiceImpl<TeamAwardConfigMapper, TeamAwardConfig> implements TeamAwardConfigEntityService {
}
