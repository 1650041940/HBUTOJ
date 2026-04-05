package top.hcode.hoj.service.oj;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.vo.RatingRankVO;
import top.hcode.hoj.pojo.vo.UserRatingVO;

public interface RatingService {

    CommonResult<UserRatingVO> getMyRating();

    CommonResult<IPage<RatingRankVO>> getContestRatingRank(Integer limit, Integer currentPage, String searchUser);

    CommonResult<IPage<RatingRankVO>> getPracticeRatingRank(Integer limit, Integer currentPage, String searchUser);
}
