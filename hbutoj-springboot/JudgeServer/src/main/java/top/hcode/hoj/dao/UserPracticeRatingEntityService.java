package top.hcode.hoj.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import top.hcode.hoj.pojo.entity.rating.UserPracticeRating;

public interface UserPracticeRatingEntityService extends IService<UserPracticeRating> {

    void refreshPracticeRating(String uid);
}
