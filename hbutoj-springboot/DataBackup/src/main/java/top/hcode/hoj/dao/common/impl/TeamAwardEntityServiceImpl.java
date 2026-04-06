package top.hcode.hoj.dao.common.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.hcode.hoj.dao.common.TeamAwardEntityService;
import top.hcode.hoj.mapper.TeamAwardMapper;
import top.hcode.hoj.pojo.entity.common.TeamAward;

@Service
public class TeamAwardEntityServiceImpl extends ServiceImpl<TeamAwardMapper, TeamAward> implements TeamAwardEntityService {
}
