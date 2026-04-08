package top.hcode.hoj.judge;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.hcode.hoj.common.exception.SystemError;
import top.hcode.hoj.dao.ContestRecordEntityService;
import top.hcode.hoj.dao.JudgeEntityService;
import top.hcode.hoj.dao.ProblemEntityService;
import top.hcode.hoj.dao.UserAcproblemEntityService;
import top.hcode.hoj.dao.UserPracticeRatingEntityService;
import top.hcode.hoj.judge.entity.LanguageConfig;
import top.hcode.hoj.pojo.dto.TestJudgeReq;
import top.hcode.hoj.pojo.dto.TestJudgeRes;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.user.UserAcproblem;
import top.hcode.hoj.pojo.entity.rating.UserPracticeRating;
import top.hcode.hoj.util.Constants;

import javax.annotation.Resource;
import java.util.HashMap;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/12 15:49
 * @Description:
 */
@Component
public class JudgeContext {

    private static final int DEFAULT_PRACTICE_RATING = 1200;
    private static final int MIN_PRACTICE_RATING = 800;
    private static final int MAX_PRACTICE_RATING = 3500;
    private static final int EASY_NO_GAIN_GAP = 200;
    private static final double ELO_K = 40.0;

    @Autowired
    private JudgeStrategy judgeStrategy;

    @Autowired
    private UserAcproblemEntityService userAcproblemEntityService;

    @Autowired
    private UserPracticeRatingEntityService userPracticeRatingEntityService;

    @Autowired
    private JudgeEntityService judgeEntityService;

    @Autowired
    private ProblemEntityService problemEntityService;

    @Autowired
    private ContestRecordEntityService contestRecordEntityService;

    @Resource
    private LanguageConfigLoader languageConfigLoader;

    public Judge Judge(Problem problem, Judge judge) {

        // c和c++为一倍时间和空间，其它语言为2倍时间和空间
        LanguageConfig languageConfig = languageConfigLoader.getLanguageConfigByName(judge.getLanguage());
        if (languageConfig.getSrcName() == null
                || (!languageConfig.getSrcName().endsWith(".c")
                && !languageConfig.getSrcName().endsWith(".cpp"))) {
            problem.setTimeLimit(problem.getTimeLimit() * 2);
            problem.setMemoryLimit(problem.getMemoryLimit() * 2);
        }

        HashMap<String, Object> judgeResult = judgeStrategy.judge(problem, judge);

        Judge finalJudgeRes = new Judge();
        finalJudgeRes.setSubmitId(judge.getSubmitId());
        // 如果是编译失败、提交错误或者系统错误就有错误提醒
        if (judgeResult.get("code") == Constants.Judge.STATUS_COMPILE_ERROR.getStatus() ||
                judgeResult.get("code") == Constants.Judge.STATUS_SYSTEM_ERROR.getStatus() ||
                judgeResult.get("code") == Constants.Judge.STATUS_RUNTIME_ERROR.getStatus() ||
                judgeResult.get("code") == Constants.Judge.STATUS_SUBMITTED_FAILED.getStatus()) {
            finalJudgeRes.setErrorMessage((String) judgeResult.getOrDefault("errMsg", ""));
        }
        // 设置最终结果状态码
        finalJudgeRes.setStatus((Integer) judgeResult.get("code"));
        // 设置最大时间和最大空间不超过题目限制时间和空间
        // kb
        Integer memory = (Integer) judgeResult.get("memory");
        finalJudgeRes.setMemory(Math.min(memory, problem.getMemoryLimit() * 1024));
        // ms
        Integer time = (Integer) judgeResult.get("time");
        finalJudgeRes.setTime(Math.min(time, problem.getTimeLimit()));
        // score
        finalJudgeRes.setScore((Integer) judgeResult.getOrDefault("score", null));
        // oi_rank_score
        finalJudgeRes.setOiRankScore((Integer) judgeResult.getOrDefault("oiRankScore", null));

        return finalJudgeRes;
    }

    public TestJudgeRes testJudge(TestJudgeReq testJudgeReq) {
        // c和c++为一倍时间和空间，其它语言为2倍时间和空间
        LanguageConfig languageConfig = languageConfigLoader.getLanguageConfigByName(testJudgeReq.getLanguage());
        if (languageConfig.getSrcName() == null
                || (!languageConfig.getSrcName().endsWith(".c")
                && !languageConfig.getSrcName().endsWith(".cpp"))) {
            testJudgeReq.setTimeLimit(testJudgeReq.getTimeLimit() * 2);
            testJudgeReq.setMemoryLimit(testJudgeReq.getMemoryLimit() * 2);
        }
        return judgeStrategy.testJudge(testJudgeReq);
    }

    public Boolean compileSpj(String code, Long pid, String spjLanguage, HashMap<String, String> extraFiles) throws SystemError {
        return Compiler.compileSpj(code, pid, spjLanguage, extraFiles);
    }

    public Boolean compileInteractive(String code, Long pid, String interactiveLanguage, HashMap<String, String> extraFiles) throws SystemError {
        return Compiler.compileInteractive(code, pid, interactiveLanguage, extraFiles);
    }


    @Transactional(rollbackFor = Exception.class)
    public void updateOtherTable(Long submitId,
                                 Integer status,
                                 Long cid,
                                 String uid,
                                 Long pid,
                                 Long gid,
                                 Integer score,
                                 Integer useTime) {

        if (cid == 0) { // 非比赛提交
            // 如果是AC,就更新user_acproblem表,
            if (status.intValue() == Constants.Judge.STATUS_ACCEPTED.getStatus() && gid == null) {
                onFirstAccepted(uid, pid, submitId);
            }

        } else { //如果是比赛提交
            contestRecordEntityService.updateContestRecord(score, status, submitId, useTime);
        }
    }

    private void onFirstAccepted(String uid, Long pid, Long submitId) {
        QueryWrapper<UserAcproblem> lockWrapper = new QueryWrapper<UserAcproblem>()
            .select("id")
            .eq("uid", uid)
            .eq("pid", pid)
            .last("LIMIT 1 FOR UPDATE");
        if (userAcproblemEntityService.getOne(lockWrapper, false) != null) {
            return;
        }

        boolean saved = userAcproblemEntityService.save(new UserAcproblem()
                .setUid(uid)
                .setPid(pid)
                .setSubmitId(submitId));
        if (!saved) {
            return;
        }

        int attempts = Math.max(1, judgeEntityService.count(new QueryWrapper<Judge>()
                .eq("uid", uid)
                .eq("pid", pid)
                .eq("cid", 0)
                .le("submit_id", submitId)));

        Integer difficultyRating = getProblemDifficultyRating(pid);

        UserPracticeRating ratingRow = userPracticeRatingEntityService.getOne(
                new QueryWrapper<UserPracticeRating>().eq("uid", uid).last("FOR UPDATE"));

        int currentRating = DEFAULT_PRACTICE_RATING;
        int solvedCount = 0;
        if (ratingRow != null) {
            if (ratingRow.getRating() != null) {
                currentRating = ratingRow.getRating();
            }
            if (ratingRow.getSolvedCount() != null) {
                solvedCount = ratingRow.getSolvedCount();
            }
        }

        int delta = calcDelta(currentRating, difficultyRating, attempts);
        int newRating = clamp(currentRating + delta);

        UserPracticeRating updated = new UserPracticeRating()
                .setUid(uid)
                .setRating(newRating)
                .setSolvedCount(solvedCount + 1);
        userPracticeRatingEntityService.saveOrUpdate(updated);
    }

    private Integer getProblemDifficultyRating(Long pid) {
        Problem problem = problemEntityService.getOne(new QueryWrapper<Problem>()
                .select("difficulty_rating")
                .eq("id", pid)
                .last("LIMIT 1"));
        if (problem == null || problem.getDifficultyRating() == null) {
            return DEFAULT_PRACTICE_RATING;
        }
        return problem.getDifficultyRating();
    }

    private int calcDelta(int currentRating, int difficultyRating, int attempts) {
        int gap = difficultyRating - currentRating;
        if (gap <= -EASY_NO_GAIN_GAP) {
            return 0;
        }
        double expected = 1.0 / (1.0 + Math.pow(10.0, (difficultyRating - currentRating) / 400.0));
        double attemptsFactor = 1.0 / Math.sqrt(Math.max(1, attempts));
        int delta = (int) Math.round(ELO_K * (1.0 - expected) * attemptsFactor);
        return Math.max(0, delta);
    }

    private int clamp(int rating) {
        return Math.max(MIN_PRACTICE_RATING, Math.min(MAX_PRACTICE_RATING, rating));
    }
}