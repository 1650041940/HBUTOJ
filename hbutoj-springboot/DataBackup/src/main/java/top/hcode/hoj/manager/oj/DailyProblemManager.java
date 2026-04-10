package top.hcode.hoj.manager.oj;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.dao.problem.ProblemTagEntityService;
import top.hcode.hoj.dao.problem.TagEntityService;
import top.hcode.hoj.dao.user.UserAcproblemEntityService;
import top.hcode.hoj.mapper.UserPracticeRatingMapper;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.problem.ProblemTag;
import top.hcode.hoj.pojo.entity.problem.Tag;
import top.hcode.hoj.pojo.entity.rating.UserPracticeRating;
import top.hcode.hoj.pojo.entity.user.UserAcproblem;
import top.hcode.hoj.pojo.vo.DailyProblemVO;
import top.hcode.hoj.utils.RatingUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DailyProblemManager {

    private static final int USER_PROFILE_AC_LIMIT = 200;
    private static final int MAX_PICK_POOL = 50;
    private static final int MAX_CANDIDATE_POOL = 500;
    private static final int MAX_REASON_TAGS = 2;

    private static final DifficultyRatingRange RANGE_ENTRY = new DifficultyRatingRange(800, 1100, "入门");
    private static final DifficultyRatingRange RANGE_EASY = new DifficultyRatingRange(1200, 1500, "基础");
    private static final DifficultyRatingRange RANGE_MEDIUM = new DifficultyRatingRange(1600, 1900, "进阶");
    private static final DifficultyRatingRange RANGE_HARD = new DifficultyRatingRange(2000, 2300, "困难");
    private static final DifficultyRatingRange RANGE_EXTREME = new DifficultyRatingRange(2400, 3500, "挑战");

    @Autowired
    private UserAcproblemEntityService userAcproblemEntityService;

    @Autowired
    private ProblemEntityService problemEntityService;

    @Autowired
    private ProblemTagEntityService problemTagEntityService;

    @Autowired
    private TagEntityService tagEntityService;

    @Resource
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

        int targetDifficultyRating = isPublic
                ? RatingUtils.DEFAULT_PROBLEM_DIFFICULTY_RATING
                : loadPracticeRating(uid);
        DifficultyRatingRange ratingRange = pickDifficultyRatingRange(targetDifficultyRating);

        List<Problem> candidates = loadCandidateProblems(solvedSet, excludePid, ratingRange);
        if (CollectionUtils.isEmpty(candidates)) {
            candidates = loadFallbackProblems(solvedSet, excludePid);
        }
        if (CollectionUtils.isEmpty(candidates)) {
            return null;
        }

        Map<Long, Set<Long>> pidToTagIds = loadPidToTagIds(
                candidates.stream().map(Problem::getId).collect(Collectors.toSet()));
        Map<Long, Integer> tagSolvedCount = isPublic ? Collections.emptyMap() : loadUserTagSolvedCount(recentAcPids);
        Map<Long, Tag> tagMap = loadTagMap(pidToTagIds.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet()));

        List<Problem> sorted = candidates.stream()
                .sorted((a, b) -> compareProblem(a, b, pidToTagIds, tagSolvedCount, targetDifficultyRating, isPublic))
                .limit(MAX_PICK_POOL)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(sorted)) {
            return null;
        }

        long seed = buildSeed(seedUid, index);
        int chosenIdx = (int) Math.floorMod(seed, (long) sorted.size());
        Problem chosen = sorted.get(chosenIdx);

        Set<Long> chosenTagIds = pidToTagIds.getOrDefault(chosen.getId(), Collections.emptySet());
        List<String> chosenTags = resolveTagNames(chosenTagIds, tagMap);
        int chosenDifficultyRating = RatingUtils.normalizeProblemDifficultyRating(chosen.getDifficultyRating());

        String reason = buildReason(isPublic, targetDifficultyRating, ratingRange, chosenDifficultyRating,
                chosenTagIds, tagSolvedCount, tagMap);

        return DailyProblemVO.builder()
                .id(chosen.getId())
                .problemId(chosen.getProblemId())
                .title(chosen.getTitle())
                .difficulty(chosen.getDifficulty())
                .difficultyRating(chosenDifficultyRating)
                .tags(chosenTags)
                .reason(reason)
                .build();
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
        return acList.stream()
                .map(UserAcproblem::getPid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private int loadPracticeRating(String uid) {
        UserPracticeRating rating = userPracticeRatingMapper.selectById(uid);
        if (rating == null || rating.getRating() == null || rating.getRating() <= 0) {
            return RatingUtils.DEFAULT_PRACTICE_RATING;
        }
        return rating.getRating();
    }

    private DifficultyRatingRange pickDifficultyRatingRange(int rating) {
        if (rating < 1200) {
            return RANGE_ENTRY;
        }
        if (rating < 1600) {
            return RANGE_EASY;
        }
        if (rating < 2000) {
            return RANGE_MEDIUM;
        }
        if (rating < 2400) {
            return RANGE_HARD;
        }
        return RANGE_EXTREME;
    }

    private List<Problem> loadCandidateProblems(Set<Long> solvedSet, Long excludePid, DifficultyRatingRange ratingRange) {
        QueryWrapper<Problem> wrapper = buildBaseProblemQuery(solvedSet, excludePid);
        wrapper.apply("COALESCE(NULLIF(difficulty_rating,0),{0}) between {1} and {2}",
                RatingUtils.DEFAULT_PROBLEM_DIFFICULTY_RATING, ratingRange.min, ratingRange.max);
        wrapper.orderByDesc("gmt_modified");
        wrapper.last("limit " + MAX_CANDIDATE_POOL);
        return problemEntityService.list(wrapper);
    }

    private List<Problem> loadFallbackProblems(Set<Long> solvedSet, Long excludePid) {
        QueryWrapper<Problem> wrapper = buildBaseProblemQuery(solvedSet, excludePid);
        wrapper.orderByDesc("gmt_modified");
        wrapper.last("limit " + MAX_CANDIDATE_POOL);
        return problemEntityService.list(wrapper);
    }

    private QueryWrapper<Problem> buildBaseProblemQuery(Set<Long> solvedSet, Long excludePid) {
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
        return wrapper;
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
                .collect(Collectors.groupingBy(ProblemTag::getPid,
                        Collectors.mapping(ProblemTag::getTid, Collectors.toSet())));
    }

    private Map<Long, Integer> loadUserTagSolvedCount(List<Long> recentAcPids) {
        if (CollectionUtils.isEmpty(recentAcPids)) {
            return Collections.emptyMap();
        }
        QueryWrapper<ProblemTag> wrapper = new QueryWrapper<>();
        wrapper.select("pid", "tid");
        wrapper.in("pid", recentAcPids);
        List<ProblemTag> list = problemTagEntityService.list(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        return list.stream()
                .filter(pt -> pt.getTid() != null)
                .collect(Collectors.toMap(ProblemTag::getTid, item -> 1, Integer::sum));
    }

    private Map<Long, Tag> loadTagMap(Set<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return Collections.emptyMap();
        }
        Collection<Tag> tags = tagEntityService.listByIds(tagIds);
        if (CollectionUtils.isEmpty(tags)) {
            return Collections.emptyMap();
        }
        return tags.stream()
                .filter(tag -> tag.getId() != null)
                .collect(Collectors.toMap(Tag::getId, tag -> tag, (a, b) -> a, LinkedHashMap::new));
    }

    private List<String> resolveTagNames(Set<Long> tagIds, Map<Long, Tag> tagMap) {
        if (CollectionUtils.isEmpty(tagIds) || CollectionUtils.isEmpty(tagMap)) {
            return Collections.emptyList();
        }
        return tagIds.stream()
                .map(tagMap::get)
                .filter(Objects::nonNull)
                .map(Tag::getName)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private int compareProblem(Problem left,
                               Problem right,
                               Map<Long, Set<Long>> pidToTagIds,
                               Map<Long, Integer> tagSolvedCount,
                               int targetDifficultyRating,
                               boolean isPublic) {
        int leftScore = scoreProblem(left, pidToTagIds, tagSolvedCount, targetDifficultyRating, isPublic);
        int rightScore = scoreProblem(right, pidToTagIds, tagSolvedCount, targetDifficultyRating, isPublic);
        if (leftScore != rightScore) {
            return Integer.compare(rightScore, leftScore);
        }

        int leftDistance = Math.abs(RatingUtils.normalizeProblemDifficultyRating(left.getDifficultyRating()) - targetDifficultyRating);
        int rightDistance = Math.abs(RatingUtils.normalizeProblemDifficultyRating(right.getDifficultyRating()) - targetDifficultyRating);
        if (leftDistance != rightDistance) {
            return Integer.compare(leftDistance, rightDistance);
        }

        return Long.compare(right.getId(), left.getId());
    }

    private int scoreProblem(Problem problem,
                             Map<Long, Set<Long>> pidToTagIds,
                             Map<Long, Integer> tagSolvedCount,
                             int targetDifficultyRating,
                             boolean isPublic) {
        int difficultyRating = RatingUtils.normalizeProblemDifficultyRating(problem.getDifficultyRating());
        int distancePenalty = Math.abs(difficultyRating - targetDifficultyRating);
        if (isPublic) {
            return -distancePenalty;
        }

        Set<Long> tagIds = pidToTagIds.getOrDefault(problem.getId(), Collections.emptySet());
        int weakTagScore = 0;
        for (Long tagId : tagIds) {
            int solvedCount = tagSolvedCount.getOrDefault(tagId, 0);
            weakTagScore += solvedCount == 0 ? 12 : Math.max(1, 8 - Math.min(solvedCount, 7));
        }
        return weakTagScore * 100 - distancePenalty;
    }

    private long buildSeed(String uid, int index) {
        String date = DateUtil.format(new Date(), "yyyy-MM-dd");
        return (uid + ":" + date + ":" + index).hashCode();
    }

    private String buildReason(boolean isPublic,
                               int targetDifficultyRating,
                               DifficultyRatingRange ratingRange,
                               int chosenDifficultyRating,
                               Set<Long> chosenTagIds,
                               Map<Long, Integer> tagSolvedCount,
                               Map<Long, Tag> tagMap) {
        if (isPublic) {
            return "默认推荐一道 " + chosenDifficultyRating + " 难度分的公开题，适合先热身。";
        }

        if (CollectionUtils.isEmpty(tagSolvedCount)) {
            return String.format("你的当前做题 Rating 约为 %d，系统先推荐一道难度分 %d 的题目作为起步训练。",
                    targetDifficultyRating, chosenDifficultyRating);
        }

        List<String> weakTags = chosenTagIds.stream()
                .sorted(Comparator.comparingInt(tagId -> tagSolvedCount.getOrDefault(tagId, 0)))
                .map(tagMap::get)
                .filter(Objects::nonNull)
                .map(Tag::getName)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .limit(MAX_REASON_TAGS)
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(weakTags)) {
            return String.format("你的当前做题 Rating 约为 %d，最近较少练习「%s」，因此推荐一道难度分 %d 的题目帮助补齐这些标签。",
                    targetDifficultyRating, String.join("、", weakTags), chosenDifficultyRating);
        }

        return String.format("你的当前做题 Rating 约为 %d，适合继续练习 %s 区间（%d~%d）的题目，这道题的难度分为 %d。",
                targetDifficultyRating, ratingRange.label, ratingRange.min, ratingRange.max, chosenDifficultyRating);
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
}
