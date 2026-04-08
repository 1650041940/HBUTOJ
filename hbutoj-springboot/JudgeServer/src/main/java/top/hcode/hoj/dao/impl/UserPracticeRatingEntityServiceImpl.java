package top.hcode.hoj.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.hcode.hoj.dao.UserPracticeRatingEntityService;
import top.hcode.hoj.mapper.UserPracticeRatingMapper;
import top.hcode.hoj.pojo.entity.rating.UserPracticeRating;

@Service
public class UserPracticeRatingEntityServiceImpl
        extends ServiceImpl<UserPracticeRatingMapper, UserPracticeRating>
        implements UserPracticeRatingEntityService {
}
