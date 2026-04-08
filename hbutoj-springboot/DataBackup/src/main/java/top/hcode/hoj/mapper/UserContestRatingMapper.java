package top.hcode.hoj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.hcode.hoj.pojo.entity.rating.UserContestRating;
import top.hcode.hoj.pojo.vo.RatingRankVO;

@Mapper
@Repository
public interface UserContestRatingMapper extends BaseMapper<UserContestRating> {

    IPage<RatingRankVO> getContestRatingRank(Page<RatingRankVO> page,
                                            @Param("searchUser") String searchUser);
}
