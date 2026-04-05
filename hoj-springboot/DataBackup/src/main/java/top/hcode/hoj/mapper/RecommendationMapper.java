package top.hcode.hoj.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.hcode.hoj.pojo.vo.RecommendProblemVO;

import java.util.List;

@Mapper
@Repository
public interface RecommendationMapper {

    List<RecommendProblemVO> getRecommendCandidates(@Param("uid") String uid,
                                                    @Param("target") Integer target,
                                                    @Param("excludePid") Long excludePid,
                                                    @Param("limit") Integer limit);
}
