package top.hcode.hoj.dao.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.hcode.hoj.dao.UserPracticeRatingEntityService;
import top.hcode.hoj.dao.JudgeEntityService;
import top.hcode.hoj.dao.ProblemEntityService;
import top.hcode.hoj.dao.UserAcproblemEntityService;
import top.hcode.hoj.mapper.UserPracticeRatingMapper;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.rating.UserPracticeRating;
import top.hcode.hoj.pojo.entity.user.UserAcproblem;
import top.hcode.hoj.pojo.vo.UserSolvedProblemStatVO;
import top.hcode.hoj.utils.RatingUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserPracticeRatingEntityServiceImpl
        extends ServiceImpl<UserPracticeRatingMapper, UserPracticeRating>
        implements UserPracticeRatingEntityService {

    @Resource
    private UserAcproblemEntityService userAcproblemEntityService;

    @Resource
    private ProblemEntityService problemEntityService;

    @Resource
    private JudgeEntityService judgeEntityService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshPracticeRating(String uid) {
        if (uid == null || uid.isEmpty()) {
            return;
        }

        QueryWrapper<UserAcproblem> acWrapper = new QueryWrapper<>();
        acWrapper.select("pid", "submit_id");
        acWrapper.eq("uid", uid);
        List<UserAcproblem> acProblems = userAcproblemEntityService.list(acWrapper);

        Map<Long, Long> firstAcceptedSubmitMap = acProblems.stream()
                .filter(item -> item.getPid() != null && item.getSubmitId() != null)
                .collect(Collectors.toMap(UserAcproblem::getPid, UserAcproblem::getSubmitId,
                        (left, right) -> left < right ? left : right));

        UserPracticeRating rating = this.getById(uid);
        if (rating == null) {
            rating = new UserPracticeRating().setUid(uid);
        }

        if (firstAcceptedSubmitMap.isEmpty()) {
            rating.setRating(RatingUtils.DEFAULT_PRACTICE_RATING);
            rating.setSolvedCount(0);
            this.saveOrUpdate(rating);
            return;
        }

        QueryWrapper<Problem> problemWrapper = new QueryWrapper<>();
        problemWrapper.select("id", "difficulty_rating");
        problemWrapper.in("id", firstAcceptedSubmitMap.keySet());
        Map<Long, Problem> problemMap = problemEntityService.list(problemWrapper).stream()
                .filter(problem -> problem.getId() != null)
                .collect(Collectors.toMap(Problem::getId, Function.identity(), (a, b) -> a));

        List<UserSolvedProblemStatVO> stats = new ArrayList<>();
        List<Map.Entry<Long, Long>> orderedEntries = firstAcceptedSubmitMap.entrySet().stream()
                .sorted(Comparator.comparingLong(Map.Entry::getValue))
                .collect(Collectors.toList());

        for (Map.Entry<Long, Long> entry : orderedEntries) {
            Long pid = entry.getKey();
            Long firstAcceptedSubmitId = entry.getValue();

            QueryWrapper<Judge> judgeWrapper = new QueryWrapper<>();
            judgeWrapper.eq("uid", uid)
                    .eq("pid", pid)
                    .eq("cid", 0)
                    .isNull("gid")
                    .le("submit_id", firstAcceptedSubmitId);
            long attempts = judgeEntityService.count(judgeWrapper);

            Problem problem = problemMap.get(pid);
            Integer difficultyRating = problem == null ? RatingUtils.DEFAULT_PROBLEM_DIFFICULTY_RATING
                    : RatingUtils.normalizeProblemDifficultyRating(problem.getDifficultyRating());

            UserSolvedProblemStatVO stat = new UserSolvedProblemStatVO();
            stat.setUid(uid);
            stat.setPid(pid);
            stat.setDifficultyRating(difficultyRating);
            stat.setAttempts((int) Math.max(1L, attempts));
            stats.add(stat);
        }

        rating.setRating(RatingUtils.calculatePracticeRating(stats));
        rating.setSolvedCount(stats.size());
        this.saveOrUpdate(rating);
    }
}
