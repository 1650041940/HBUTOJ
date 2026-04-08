package top.hcode.hoj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.hcode.hoj.pojo.entity.common.TeamAward;

@Mapper
@Repository
public interface TeamAwardMapper extends BaseMapper<TeamAward> {
}
