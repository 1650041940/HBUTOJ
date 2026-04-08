package top.hcode.hoj.manager.oj;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import top.hcode.hoj.dao.judge.JudgeEntityService;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.dao.problem.ProblemTagEntityService;
import top.hcode.hoj.dao.problem.TagEntityService;
import top.hcode.hoj.dao.user.UserAcproblemEntityService;
import top.hcode.hoj.mapper.UserPracticeRatingMapper;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.problem.ProblemTag;
import top.hcode.hoj.pojo.entity.problem.Tag;
import top.hcode.hoj.pojo.entity.user.UserAcproblem;
import top.hcode.hoj.pojo.entity.rating.UserPracticeRating;
import top.hcode.hoj.pojo.vo.DailyProblemVO;
import top.hcode.hoj.utils.Constants;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DailyProblemManager {

    private static final int USER_PROFILE_AC_LIMIT = 200;
    private static final int USER_PROFILE_JUDGE_LIMIT = 500;
    private static final int WEAK_TAG_MIN_ATTEMPTS = 2;
    private static final int MAX_TOP_TAGS = 5;
    private static final int MAX_PICK_POOL = 50;

    private static final int DIFFICULTY_RATING_MIN = 800;
    private static final int DIFFICULTY_RATING_MAX = 3500;

    private static final DifficultyRatingRange RANGE_ENTRY = new DifficultyRatingRange(800, 1100, "入门");
    private static final DifficultyRatingRange RANGE_EASY = new DifficultyRatingRange(1200, 1500, "简单");
    private static final DifficultyRatingRange RANGE_MEDIUM = new DifficultyRatingRange(1600, 1900, "中等");
    private static final DifficultyRatingRange RANGE_HARD = new DifficultyRatingRange(2000, 2300, "困难");
    private static final DifficultyRatingRange RANGE_EXTREME = new DifficultyRatingRange(2400, 3500, "极难");

    @Autowired
    private UserAcproblemEntityService userAcproblemEntityService;

    @Autowired
    private JudgeEntityService judgeEntityService;

    @Autowired
    private ProblemEntityService problemEntityService;

    @Autowired
    private ProblemTagEntityService problemTagEntityService;

    @Autowired
    private TagEntityService tagEntityService;

    @Autowired
    private UserPracticeRatingMapper userPracticeRatingMapper;

    public DailyProblemVO getDailyProblem(String uid) {
        return recommend(uid, 0, null);
    }

    public DailyProblemVO getNextProblem(String uid, Integer index, Long currentPid) {
        int safeIndex = index == null ? 1 : Math.max(1, index);
        return recommend(uid, safeIndex, currentPid);
    }

    private DailyProblemVO recommend(String uid, int index, Long excludePid) {
        boolean isPublic = StrUtil.isBlank(uid);
        String seedUid = isPublic ? "public" : uid;

        List<Long> recentAcPids = isPublic ? Collections.emptyList() : loadRecentAcPids(uid);
        Set<Long> solvedSet = new HashSet<>(recentAcPids);

        int targetDifficultyRating = isPublic ? 900 : estimateTargetDifficultyRating(uid, recentAcPids);
        DifficultyRatingRange ratingRange = isPublic
            ? new DifficultyRatingRange(800, 1100, "入门")
            : pickDifficultyRatingRange(targetDifficultyRating);

        TagProfile tagProfile = isPublic ? TagProfile.empty() : buildTagProfile(uid);
        List<Long> tagPool = !CollectionUtils.isEmpty(tagProfile.weakTagIds)
            ? tagProfile.weakTagIds
            : loadTopTagIds(recentAcPids);

        List<Problem> candidates = loadCandidateProblems(tagPool, solvedSet, excludePid, ratingRange);
        if (CollectionUtils.isEmpty(candidates)) {
            candidates = loadFallbackProblems(solvedSet, excludePid, ratingRange);
        }
        if (CollectionUtils.isEmpty(candidates)) {
            return null;
        }

        Map<Long, Set<Long>> pidToTagIds = loadPidToTagIds(
                candidates.stream().map(Problem::getId).collect(Collectors.toSet())
        );

        List<Problem> sorted = candidates.stream()
            .sorted((a, b) -> Integer.compare(score(b, pidToTagIds, tagPool, targetDifficultyRating), score(a, pidToTagIds, tagPool, targetDifficultyRating)))
                .limit(MAX_PICK_POOL)
                .collect(Collectors.toList());

        if (sorted.isEmpty()) {
            return null;
        }

        long seed = buildSeed(seedUid, index);
        int chosenIdx = (int) Math.floorMod(seed, (long) sorted.size());
        Problem chosen = sorted.get(chosenIdx);

        Set<Long> chosenTagIds = pidToTagIds.getOrDefault(chosen.getId(), Collections.emptySet());
        List<String> chosenTags = resolveTagNames(chosenTagIds);

        String reason = isPublic
            ? "今日推荐（未登录）"
            : buildReason(tagProfile, tagPool, targetDifficultyRating, ratingRange);

        Integer rawDifficultyRating = chosen.getDifficultyRating();
        int difficultyRating = (rawDifficultyRating == null || rawDifficultyRating == 0) ? 800 : rawDifficultyRating;

        return DailyProblemVO.builder()
                .id(chosen.getId())
                .problemId(chosen.getProblemId())
                .title(chosen.getTitle())
            .difficulty(null)
                .difficultyRating(difficultyRating)
                .tags(chosenTags)
                .reason(reason)
                .build();
    }

    private static class DifficultyRatingRange {
        final int min;
        final int max;
        final String label;

        private DifficultyRatingRange(int min, int max, String label) {
            this.min = min;
            this.max = max;
            this.label = label;
        }
    }

    private static class TagAttemptStats {
        int attempts;
        int accepted;

        double successRate() {
            if (attempts <= 0) {
                return 0.0;
            }
            return accepted * 1.0 / attempts;
        }

        double smoothedSuccessRate() {
            // Laplace smoothing to avoid unstable 0%/100% with tiny samples.
            return (accepted + 1.0) / (attempts + 2.0);
        }
    }

    private static class TagProfile {
        final List<Long> weakTagIds;
        final Map<Long, TagAttemptStats> statsByTagId;
        final boolean lowConfidence;

        private TagProfile(List<Long> weakTagIds, Map<Long, TagAttemptStats> statsByTagId, boolean lowConfidence) {
            this.weakTagIds = weakTagIds;
            this.statsByTagId = statsByTagId;
            this.lowConfidence = lowConfidence;
        }

        static TagProfile empty() {
            return new TagProfile(Collections.emptyList(), Collections.emptyMap(), false);
        }
    }

    private TagProfile buildTagProfile(String uid) {
        List<Judge> recentJudges = loadRecentJudges(uid);
        if (CollectionUtils.isEmpty(recentJudges)) {
            return TagProfile.empty();
        }

        Set<Long> attemptedPids = recentJudges.stream()
                .map(Judge::getPid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (attemptedPids.isEmpty()) {
            return TagProfile.empty();
        }

        Map<Long, Set<Long>> pidToTagIds = loadPidToTagIds(attemptedPids);
        if (CollectionUtils.isEmpty(pidToTagIds)) {
            return TagProfile.empty();
        }

        Map<Long, TagAttemptStats> statsByTagId = new HashMap<>();
        for (Judge judge : recentJudges) {
            Long pid = judge.getPid();
            if (pid == null) {
                continue;
            }
            Set<Long> tagIds = pidToTagIds.get(pid);
            if (CollectionUtils.isEmpty(tagIds)) {
                continue;
            }
            boolean accepted = Objects.equals(judge.getStatus(), Constants.Judge.STATUS_ACCEPTED.getStatus());
            for (Long tid : tagIds) {
                if (tid == null) {
                    continue;
                }
                TagAttemptStats stats = statsByTagId.computeIfAbsent(tid, k -> new TagAttemptStats());
                stats.attempts++;
                if (accepted) {
                    stats.accepted++;
                }
            }
        }

        List<Map.Entry<Long, TagAttemptStats>> ordered = statsByTagId.entrySet().stream()
            .sorted((a, b) -> {
                int cmp = Double.compare(a.getValue().smoothedSuccessRate(), b.getValue().smoothedSuccessRate());
                if (cmp != 0) return cmp;
                cmp = Integer.compare(b.getValue().attempts, a.getValue().attempts);
                if (cmp != 0) return cmp;
                return Long.compare(a.getKey(), b.getKey());
            })
            .collect(Collectors.toList());

        List<Long> weakReliable = ordered.stream()
            .filter(e -> e.getValue().attempts >= WEAK_TAG_MIN_ATTEMPTS)
            .limit(MAX_TOP_TAGS)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        if (!weakReliable.isEmpty()) {
            return new TagProfile(weakReliable, statsByTagId, false);
        }

        // If data is sparse, still pick the weakest tags, but mark low confidence.
        List<Long> weakLowConf = ordered.stream()
            .filter(e -> e.getValue().attempts >= 1)
            .limit(MAX_TOP_TAGS)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        return new TagProfile(weakLowConf, statsByTagId, true);
    }

    private List<Judge> loadRecentJudges(String uid) {
        QueryWrapper<Judge> wrapper = new QueryWrapper<>();
        wrapper.select("pid", "status");
        wrapper.eq("uid", uid);
        wrapper.orderByDesc("gmt_create");
        wrapper.last("limit " + USER_PROFILE_JUDGE_LIMIT);
        return judgeEntityService.list(wrapper);
    }

    private List<Long> loadRecentAcPids(String uid) {
        QueryWrapper<UserAcproblem> wrapper = new QueryWrapper<>();
        wrapper.select("pid");
        wrapper.eq("uid", uid);
        wrapper.orderByDesc("gmt_create");
        wrapper.last("limit " + USER_PROFILE_AC_LIMIT);
        List<UserAcproblem> acList = userAcproblemEntityService.list(wrapper);
        if (CollectionUtils.isEmpty(acList)) {
            return Collections.emptyList();
        }
        return acList.stream().map(UserAcproblem::getPid).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private int estimateTargetDifficultyRating(String uid, List<Long> recentAcPids) {
        Integer practiceRating = loadUserPracticeRating(uid);
        if (practiceRating != null) {
            return clampDifficultyRating(practiceRating);
        }
        return estimateTargetDifficultyRatingFromAc(recentAcPids);
    }

    private Integer loadUserPracticeRating(String uid) {
        if (StrUtil.isBlank(uid)) {
            return null;
        }
        UserPracticeRating rating = userPracticeRatingMapper.selectById(uid);
        if (rating == null || rating.getRating() == null) {
            return null;
        }
        if (rating.getRating() <= 0) {
            return null;
        }
        return rating.getRating();
    }

    private int clampDifficultyRating(int rating) {
        return Math.max(DIFFICULTY_RATING_MIN, Math.min(DIFFICULTY_RATING_MAX, rating));
    }

    private DifficultyRatingRange pickDifficultyRatingRange(int rating) {
        int r = clampDifficultyRating(rating);
        // 为了避免段位边界的“空档”（如 1100~1200），用阈值中点把用户 rating 归入最近的一个区间。
        // Entry/Easy 分界中点：1150；Easy/Medium：1550；Medium/Hard：1950；Hard/Extreme：2350。
        if (r < 1150) {
            return RANGE_ENTRY;
        }
        if (r < 1550) {
            return RANGE_EASY;
        }
        if (r < 1950) {
            return RANGE_MEDIUM;
        }
        if (r < 2350) {
            return RANGE_HARD;
        }
        return RANGE_EXTREME;
    }

    private int estimateTargetDifficultyRatingFromAc(List<Long> recentAcPids) {
        if (CollectionUtils.isEmpty(recentAcPids)) {
            return 900;
        }
        Collection<Problem> problems = problemEntityService.listByIds(recentAcPids);
        if (CollectionUtils.isEmpty(problems)) {
            return 900;
        }
        IntSummaryStatistics stats = problems.stream()
                .map(Problem::getDifficultyRating)
                .filter(Objects::nonNull)
                .map(r -> r == 0 ? 800 : r)
                .mapToInt(Integer::intValue)
                .summaryStatistics();
        if (stats.getCount() == 0) {
            return 900;
        }
        int avg = (int) Math.round(stats.getAverage());
        int target = avg + 100;
        return Math.max(800, Math.min(3500, target));
    }

    private List<Long> loadTopTagIds(List<Long> recentAcPids) {
        if (CollectionUtils.isEmpty(recentAcPids)) {
            return Collections.emptyList();
        }
        QueryWrapper<ProblemTag> wrapper = new QueryWrapper<>();
        wrapper.select("pid", "tid");
        wrapper.in("pid", recentAcPids);
        List<ProblemTag> tags = problemTagEntityService.list(wrapper);
        if (CollectionUtils.isEmpty(tags)) {
            return Collections.emptyList();
        }

        Map<Long, Long> tidCount = tags.stream()
                .filter(pt -> pt.getTid() != null)
                .collect(Collectors.groupingBy(ProblemTag::getTid, Collectors.counting()));

        return tidCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(MAX_TOP_TAGS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<Problem> loadCandidateProblems(List<Long> tagPoolIds, Set<Long> solvedSet, Long excludePid, DifficultyRatingRange ratingRange) {
        if (CollectionUtils.isEmpty(tagPoolIds)) {
            return Collections.emptyList();
        }

        QueryWrapper<ProblemTag> ptWrapper = new QueryWrapper<>();
        ptWrapper.select("pid");
        ptWrapper.in("tid", tagPoolIds);
        ptWrapper.last("limit 2000");
        List<ProblemTag> ptList = problemTagEntityService.list(ptWrapper);
        if (CollectionUtils.isEmpty(ptList)) {
            return Collections.emptyList();
        }

        Set<Long> candidatePids = ptList.stream()
                .map(ProblemTag::getPid)
                .filter(Objects::nonNull)
                .filter(pid -> !solvedSet.contains(pid))
                .filter(pid -> excludePid == null || !excludePid.equals(pid))
                .collect(Collectors.toSet());

        if (candidatePids.isEmpty()) {
            return Collections.emptyList();
        }

        QueryWrapper<Problem> wrapper = new QueryWrapper<>();
    wrapper.select("id", "problem_id", "title", "difficulty", "difficulty_rating");
        wrapper.eq("auth", 1);
        wrapper.eq("is_group", false);
        wrapper.in("id", candidatePids);

        // 按用户所在 rating 区间过滤（而不是 ±200）
        wrapper.apply("COALESCE(NULLIF(difficulty_rating,0),800) between {0} and {1}", ratingRange.min, ratingRange.max);
        wrapper.orderByDesc("gmt_modified");
        wrapper.last("limit 500");
        List<Problem> list = problemEntityService.list(wrapper);
        if (!CollectionUtils.isEmpty(list)) {
            return list;
        }

        // 放宽难度限制
        QueryWrapper<Problem> wrapper2 = new QueryWrapper<>();
        wrapper2.select("id", "problem_id", "title", "difficulty", "difficulty_rating");
        wrapper2.eq("auth", 1);
        wrapper2.eq("is_group", false);
        wrapper2.in("id", candidatePids);
        wrapper2.orderByDesc("gmt_modified");
        wrapper2.last("limit 500");
        return problemEntityService.list(wrapper2);
    }

    private List<Problem> loadFallbackProblems(Set<Long> solvedSet, Long excludePid, DifficultyRatingRange ratingRange) {
        QueryWrapper<Problem> wrapper = new QueryWrapper<>();
        wrapper.select("id", "problem_id", "title", "difficulty", "difficulty_rating");
        wrapper.eq("auth", 1);
        wrapper.eq("is_group", false);
        if (!CollectionUtils.isEmpty(solvedSet)) {
            wrapper.notIn("id", solvedSet);
        }
        if (excludePid != null) {
            wrapper.ne("id", excludePid);
        }
        wrapper.apply("COALESCE(NULLIF(difficulty_rating,0),800) between {0} and {1}", ratingRange.min, ratingRange.max);
        wrapper.orderByDesc("gmt_modified");
        wrapper.last("limit 200");
        return problemEntityService.list(wrapper);
    }

    private Map<Long, Set<Long>> loadPidToTagIds(Set<Long> pids) {
        if (CollectionUtils.isEmpty(pids)) {
            return Collections.emptyMap();
        }
        QueryWrapper<ProblemTag> wrapper = new QueryWrapper<>();
        wrapper.select("pid", "tid");
        wrapper.in("pid", pids);
        List<ProblemTag> list = problemTagEntityService.list(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        return list.stream()
                .filter(pt -> pt.getPid() != null && pt.getTid() != null)
                .collect(Collectors.groupingBy(ProblemTag::getPid, Collectors.mapping(ProblemTag::getTid, Collectors.toSet())));
    }

    private List<String> resolveTagNames(Set<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return Collections.emptyList();
        }
        Collection<Tag> tags = tagEntityService.listByIds(tagIds);
        if (CollectionUtils.isEmpty(tags)) {
            return Collections.emptyList();
        }
        return tags.stream().map(Tag::getName).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private int score(Problem problem, Map<Long, Set<Long>> pidToTagIds, List<Long> tagPoolIds, int targetDifficultyRating) {
        int tagMatch = 0;
        if (!CollectionUtils.isEmpty(tagPoolIds)) {
            Set<Long> tagSet = pidToTagIds.getOrDefault(problem.getId(), Collections.emptySet());
            for (Long tid : tagPoolIds) {
                if (tagSet.contains(tid)) {
                    tagMatch++;
                }
            }
        }
        Integer raw = problem.getDifficultyRating();
        int rating = (raw == null || raw == 0) ? 800 : raw;
        int ratingPenalty = Math.abs(rating - targetDifficultyRating) / 100;
        return tagMatch * 10 - ratingPenalty * 2;
    }

    private long buildSeed(String uid, int index) {
        String date = DateUtil.format(new Date(), "yyyy-MM-dd");
        String key = uid + ":" + date + ":" + index;
        return (long) key.hashCode();
    }

    private String buildReason(TagProfile tagProfile, List<Long> tagPoolIds, int targetDifficultyRating, DifficultyRatingRange ratingRange) {
        if (!CollectionUtils.isEmpty(tagProfile.weakTagIds)) {
            Long tid = tagProfile.weakTagIds.get(0);
            TagAttemptStats stats = tagProfile.statsByTagId.get(tid);
            String tagName = resolveTagNames(Collections.singleton(tid)).stream().findFirst().orElse(null);
            if (!StrUtil.isBlank(tagName) && stats != null && stats.attempts > 0) {
                int percent = (int) Math.round(stats.successRate() * 100);
                String prefix = tagProfile.lowConfidence ? "数据较少，初步判断：" : "";
                return prefix + "你在「" + tagName + "」题方面成功率偏低（" + stats.accepted + "/" + stats.attempts + "，约" + percent + "%），建议针对性练习；本题难度分落在你的区间：" + ratingRange.label + "（" + ratingRange.min + "~" + ratingRange.max + "）。";
            }
        }

        if (CollectionUtils.isEmpty(tagPoolIds)) {
            return "难度分区间：" + ratingRange.label + "（" + ratingRange.min + "~" + ratingRange.max + "）";
        }

        Collection<Tag> tags = tagEntityService.listByIds(tagPoolIds.stream().limit(2).collect(Collectors.toList()));
        String tagText = tags.stream().map(Tag::getName).filter(Objects::nonNull).collect(Collectors.joining("、"));
        if (StrUtil.isBlank(tagText)) {
            return "难度分区间：" + ratingRange.label + "（" + ratingRange.min + "~" + ratingRange.max + "）";
        }
        return "根据你最近的薄弱标签/相关标签：" + tagText + "（难度分区间：" + ratingRange.label + " " + ratingRange.min + "~" + ratingRange.max + "）";
    }
}
