package top.hcode.hoj.manager.rating;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.dao.contest.ContestEntityService;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.dao.user.UserInfoEntityService;
import top.hcode.hoj.mapper.*;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.rating.*;
import top.hcode.hoj.pojo.entity.user.UserInfo;
import top.hcode.hoj.pojo.vo.*;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.manager.oj.ContestCalculateRankManager;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.RatingUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j(topic = "hbutoj")
public class RatingManager {

    private static final int DEFAULT_PRACTICE_RATING = RatingUtils.DEFAULT_PRACTICE_RATING;
    private static final int DEFAULT_CONTEST_RATING = RatingUtils.DEFAULT_CONTEST_RATING;

    private static final int DIFFICULTY_MIN = RatingUtils.MIN_PROBLEM_DIFFICULTY_RATING;
    private static final int DIFFICULTY_MAX = RatingUtils.MAX_PROBLEM_DIFFICULTY_RATING;

    @Resource
    private ProblemEntityService problemEntityService;

    @Resource
    private JudgeMapper judgeMapper;

    @Resource
    private ProblemDifficultyHistoryMapper problemDifficultyHistoryMapper;

    @Resource
    private UserInfoEntityService userInfoEntityService;

    @Resource
    private UserAcproblemMapper userAcproblemMapper;

    @Resource
    private UserPracticeRatingMapper userPracticeRatingMapper;

    @Resource
    private UserPracticeRatingHistoryMapper userPracticeRatingHistoryMapper;

    @Resource
    private ContestEntityService contestEntityService;

    @Resource
    private ContestCalculateRankManager contestCalculateRankManager;

    @Resource
    private UserContestRatingMapper userContestRatingMapper;

    @Resource
    private UserContestRatingHistoryMapper userContestRatingHistoryMapper;

    @Resource
    private ContestRatingEventMapper contestRatingEventMapper;

    public UserRatingVO getMyRating() throws StatusFailException {
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        if (userRolesVo == null) {
            throw new StatusFailException("请先登录");
        }

        String uid = userRolesVo.getUid();
        UserInfo userInfo = userInfoEntityService.getById(uid);

        UserContestRating contestRating = ensureContestRating(uid);
        UserPracticeRating practiceRating = ensurePracticeRating(uid);

        UserRatingVO vo = new UserRatingVO();
        vo.setUid(uid);
        if (userInfo != null) {
            vo.setUsername(userInfo.getUsername());
            vo.setNickname(userInfo.getNickname());
        }
        vo.setContestRating(contestRating.getRating());
        vo.setContestCount(contestRating.getContestCount());
        vo.setPracticeRating(practiceRating.getRating());
        vo.setSolvedCount(practiceRating.getSolvedCount());
        return vo;
    }

    private UserPracticeRating ensurePracticeRating(String uid) {
        UserPracticeRating rating = userPracticeRatingMapper.selectById(uid);
        if (rating != null) {
            return rating;
        }
        rating = new UserPracticeRating()
                .setUid(uid)
                .setRating(DEFAULT_PRACTICE_RATING)
                .setSolvedCount(0)
                .setLastCalcMonth(null);
        userPracticeRatingMapper.insert(rating);
        return rating;
    }

    private UserContestRating ensureContestRating(String uid) {
        UserContestRating rating = userContestRatingMapper.selectById(uid);
        if (rating != null) {
            return rating;
        }
        rating = new UserContestRating()
                .setUid(uid)
                .setRating(DEFAULT_CONTEST_RATING)
                .setContestCount(0);
        userContestRatingMapper.insert(rating);
        return rating;
    }

    @Transactional(rollbackFor = Exception.class)
    public void monthlyAdjustProblemDifficultyAndPracticeRating() {
        DateTime now = DateUtil.date();
        String month = DateUtil.format(DateUtil.beginOfMonth(DateUtil.offsetMonth(now, -1)), "yyyy-MM");

        initMissingProblemDifficulty(month);
        snapshotProblemDifficulty(month);
        snapshotCurrentPracticeRating(month);

        log.info("月度题目难度与做题rating快照完成，month={}", month);
    }

    private void initMissingProblemDifficulty(String month) {
        List<Problem> problems = problemEntityService.list(new QueryWrapper<Problem>().select("id", "difficulty", "difficulty_rating"));
        if (CollectionUtils.isEmpty(problems)) {
            return;
        }
        List<Problem> needInit = new ArrayList<>();
        for (Problem p : problems) {
            Integer dr = p.getDifficultyRating();
            if (dr == null || dr <= 0) {
                int init = RatingUtils.DEFAULT_PROBLEM_DIFFICULTY_RATING;
                needInit.add(new Problem().setId(p.getId()).setDifficultyRating(init));
            }
        }
        if (!needInit.isEmpty()) {
            problemEntityService.updateBatchById(needInit, 200);
        }
    }

    private void snapshotProblemDifficulty(String month) {
        List<Problem> problems = problemEntityService.list(new QueryWrapper<Problem>().select("id", "difficulty_rating"));
        if (CollectionUtils.isEmpty(problems)) {
            return;
        }
        for (Problem problem : problems) {
            if (problem.getId() == null) {
                continue;
            }
            QueryWrapper<ProblemDifficultyHistory> existQ = new QueryWrapper<ProblemDifficultyHistory>()
                    .eq("pid", problem.getId())
                    .eq("month", month);
            if (problemDifficultyHistoryMapper.selectCount(existQ) > 0) {
                continue;
            }
            int difficulty = RatingUtils.normalizeProblemDifficultyRating(problem.getDifficultyRating());
            problemDifficultyHistoryMapper.insert(new ProblemDifficultyHistory()
                    .setPid(problem.getId())
                    .setMonth(month)
                    .setOldDifficulty(difficulty)
                    .setDelta(0)
                    .setNewDifficulty(difficulty)
                    .setAttemptedUsers(0)
                    .setAcceptedUsers(0)
                    .setAvgAttempts(null));
        }
    }

    private void snapshotCurrentPracticeRating(String month) {
        List<UserPracticeRating> ratings = userPracticeRatingMapper.selectList(new QueryWrapper<UserPracticeRating>()
                .select("uid", "rating", "solved_count"));
        if (CollectionUtils.isEmpty(ratings)) {
            return;
        }

        for (UserPracticeRating current : ratings) {
            String uid = current.getUid();
            if (uid == null) {
                continue;
            }
            QueryWrapper<UserPracticeRatingHistory> existQ = new QueryWrapper<UserPracticeRatingHistory>()
                    .eq("uid", uid)
                    .eq("month", month);
            if (userPracticeRatingHistoryMapper.selectCount(existQ) > 0) {
                continue;
            }

            int oldRating = Optional.ofNullable(current.getRating()).orElse(DEFAULT_PRACTICE_RATING);
            int solvedCount = Optional.ofNullable(current.getSolvedCount()).orElse(0);

            UserPracticeRatingHistory history = new UserPracticeRatingHistory()
                    .setUid(uid)
                    .setMonth(month)
                    .setOldRating(oldRating)
                    .setDelta(0)
                    .setNewRating(oldRating)
                    .setSolvedCount(solvedCount);
            userPracticeRatingHistoryMapper.insert(history);
        }
    }

    private int calcPracticeRating(List<UserSolvedProblemStatVO> stats) {
        if (CollectionUtils.isEmpty(stats)) {
            return DEFAULT_PRACTICE_RATING;
        }
        return RatingUtils.calculatePracticeRating(stats);
    }

    private int clampDifficulty(int v) {
        return Math.max(DIFFICULTY_MIN, Math.min(DIFFICULTY_MAX, v));
    }

    @Transactional(rollbackFor = Exception.class)
    public void processEndedContestsForRating() {
        DateTime now = DateUtil.date();
        DateTime since = DateUtil.offsetDay(now, -90);

        List<Contest> contests = contestEntityService.list(new QueryWrapper<Contest>()
                .ge("end_time", since)
                .le("end_time", now)
            .eq("status", Constants.Contest.STATUS_ENDED.getCode()));

        if (CollectionUtils.isEmpty(contests)) {
            return;
        }

        for (Contest contest : contests) {
            ContestRatingEvent event = contestRatingEventMapper.selectById(contest.getId());
            if (event != null && Boolean.TRUE.equals(event.getProcessed())) {
                continue;
            }
            try {
                doProcessOneContest(contest);
            } catch (Exception e) {
                log.error("比赛rating处理失败 cid={} title={} err={}", contest.getId(), contest.getTitle(), e.getMessage());
            }
        }
    }

    private void doProcessOneContest(Contest contest) {
        Long cid = contest.getId();

        boolean isContainsAfter = Boolean.TRUE.equals(contest.getAllowEndSubmit());
        int participants;
        Map<String, Integer> rankMap = new HashMap<>();

        if (contest.getType() != null && contest.getType().intValue() == Constants.Contest.TYPE_ACM.getCode()) {
            List<ACMContestRankVO> ranks = contestCalculateRankManager.calcACMRank(false,
                    true,
                    contest,
                    null,
                    null,
                    new ArrayList<>(),
                    isContainsAfter);
            ranks = ranks.stream().filter(r -> r.getRank() != null && r.getRank() > 0).collect(Collectors.toList());
            participants = ranks.size();
            for (ACMContestRankVO r : ranks) {
                rankMap.put(r.getUid(), r.getRank());
            }
        } else {
            List<OIContestRankVO> ranks = contestCalculateRankManager.calcOIRank(false,
                    true,
                    contest,
                    null,
                    null,
                    new ArrayList<>(),
                    isContainsAfter);
            ranks = ranks.stream().filter(r -> r.getRank() != null && r.getRank() > 0).collect(Collectors.toList());
            participants = ranks.size();
            for (OIContestRankVO r : ranks) {
                rankMap.put(r.getUid(), r.getRank());
            }
        }

        ContestRatingEvent event = contestRatingEventMapper.selectById(cid);
        if (event == null) {
            event = new ContestRatingEvent().setCid(cid);
            event.setProcessed(false);
            contestRatingEventMapper.insert(event);
        }

        if (participants <= 0) {
            event.setProcessed(true);
            event.setParticipants(0);
            event.setProcessedTime(new Date());
            contestRatingEventMapper.updateById(event);
            return;
        }

        // 初始化参赛选手当前rating
        Map<String, UserContestRating> ratingMap = new HashMap<>();
        for (String uid : rankMap.keySet()) {
            ratingMap.put(uid, ensureContestRating(uid));
        }

        double avgRating = ratingMap.values().stream().mapToInt(r -> Optional.ofNullable(r.getRating()).orElse(DEFAULT_CONTEST_RATING)).average().orElse(DEFAULT_CONTEST_RATING);
        int k = calcContestK(participants);

        // 按rank升序处理
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(rankMap.entrySet());
        entries.sort(Comparator.comparingInt(Map.Entry::getValue));

        for (Map.Entry<String, Integer> entry : entries) {
            String uid = entry.getKey();
            int rank = entry.getValue();

            QueryWrapper<UserContestRatingHistory> existQ = new QueryWrapper<UserContestRatingHistory>()
                    .eq("uid", uid)
                    .eq("cid", cid);
            if (userContestRatingHistoryMapper.selectCount(existQ) > 0) {
                continue;
            }

            UserContestRating cur = ratingMap.get(uid);
            int old = Optional.ofNullable(cur.getRating()).orElse(DEFAULT_CONTEST_RATING);
            double expected = 1.0 / (1.0 + Math.pow(10.0, (avgRating - old) / 400.0));
            double actual = participants == 1 ? 0.5 : ((participants - rank) * 1.0 / (participants - 1));
            int delta = (int) Math.round(k * (actual - expected));

            int newRating = Math.max(0, old + delta);

            UserContestRatingHistory history = new UserContestRatingHistory()
                    .setUid(uid)
                    .setCid(cid)
                    .setOldRating(old)
                    .setDelta(delta)
                    .setNewRating(newRating)
                    .setRank(rank)
                    .setParticipants(participants);
            userContestRatingHistoryMapper.insert(history);

            cur.setRating(newRating);
            cur.setContestCount(Optional.ofNullable(cur.getContestCount()).orElse(0) + 1);
            userContestRatingMapper.updateById(cur);
        }

        event.setProcessed(true);
        event.setParticipants(participants);
        event.setProcessedTime(new Date());
        contestRatingEventMapper.updateById(event);

        log.info("比赛rating处理完成 cid={} participants={}", cid, participants);
    }

    private int calcContestK(int participants) {
        double scale = Math.log(Math.max(2, participants)) / Math.log(2.0);
        int k = (int) Math.round(20.0 * scale);
        return Math.max(20, Math.min(80, k));
    }
}
