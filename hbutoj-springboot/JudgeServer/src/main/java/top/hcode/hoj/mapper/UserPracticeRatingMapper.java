package top.hcode.hoj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.hcode.hoj.pojo.entity.rating.UserPracticeRating;

@Mapper
@Repository
public interface UserPracticeRatingMapper extends BaseMapper<UserPracticeRating> {
}
