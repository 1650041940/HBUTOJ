package top.hcode.hoj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;
import top.hcode.hoj.pojo.entity.rating.UserPracticeRating;

@Repository
public interface UserPracticeRatingMapper extends BaseMapper<UserPracticeRating> {
}
