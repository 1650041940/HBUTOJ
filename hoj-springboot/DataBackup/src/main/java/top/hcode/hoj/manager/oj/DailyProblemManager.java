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
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.problem.ProblemTag;
import top.hcode.hoj.pojo.entity.problem.Tag;
import top.hcode.hoj.pojo.entity.user.UserAcproblem;
import top.hcode.hoj.pojo.vo.DailyProblemVO;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DailyProblemManager {

    private static final int USER_PROFILE_AC_LIMIT = 200;
    private static final int MAX_TOP_TAGS = 5;
    private static final int MAX_PICK_POOL = 50;

    @Autowired
    private UserAcproblemEntityService userAcproblemEntityService;

    @Autowired
    private ProblemEntityService problemEntityService;

    @Autowired
    private ProblemTagEntityService problemTagEntityService;

    @Autowired
    private TagEntityService tagEntityService;

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

        int targetDifficulty = estimateTargetDifficulty(recentAcPids);

        List<Long> topTagIds = loadTopTagIds(recentAcPids);

        List<Problem> candidates = loadCandidateProblems(topTagIds, solvedSet, excludePid, targetDifficulty);
        if (CollectionUtils.isEmpty(candidates)) {
            candidates = loadFallbackProblems(solvedSet, excludePid, targetDifficulty);
        }
        if (CollectionUtils.isEmpty(candidates)) {
            return null;
        }

        Map<Long, Set<Long>> pidToTagIds = loadPidToTagIds(
                candidates.stream().map(Problem::getId).collect(Collectors.toSet())
        );

        List<Problem> sorted = candidates.stream()
                .sorted((a, b) -> Integer.compare(score(b, pidToTagIds, topTagIds, targetDifficulty), score(a, pidToTagIds, topTagIds, targetDifficulty)))
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

        String reason = isPublic ? "今日推荐（未登录）" : buildReason(topTagIds, targetDifficulty);

        return DailyProblemVO.builder()
                .id(chosen.getId())
                .problemId(chosen.getProblemId())
                .title(chosen.getTitle())
                .difficulty(chosen.getDifficulty())
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
        return acList.stream().map(UserAcproblem::getPid).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private int estimateTargetDifficulty(List<Long> recentAcPids) {
        if (CollectionUtils.isEmpty(recentAcPids)) {
            return 1;
        }
        Collection<Problem> problems = problemEntityService.listByIds(recentAcPids);
        if (CollectionUtils.isEmpty(problems)) {
            return 1;
        }
        IntSummaryStatistics stats = problems.stream()
                .map(Problem::getDifficulty)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .summaryStatistics();
        if (stats.getCount() == 0) {
            return 1;
        }
        int avg = (int) Math.round(stats.getAverage());
        return Math.max(1, avg + 1);
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

    private List<Problem> loadCandidateProblems(List<Long> topTagIds, Set<Long> solvedSet, Long excludePid, int targetDifficulty) {
        if (CollectionUtils.isEmpty(topTagIds)) {
            return Collections.emptyList();
        }

        QueryWrapper<ProblemTag> ptWrapper = new QueryWrapper<>();
        ptWrapper.select("pid");
        ptWrapper.in("tid", topTagIds);
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
        wrapper.select("id", "problem_id", "title", "difficulty");
        wrapper.eq("auth", 1);
        wrapper.eq("is_group", false);
        wrapper.in("id", candidatePids);

        // 先尝试难度附近
        wrapper.between("difficulty", Math.max(1, targetDifficulty - 1), targetDifficulty + 1);
        wrapper.orderByDesc("gmt_modified");
        wrapper.last("limit 500");
        List<Problem> list = problemEntityService.list(wrapper);
        if (!CollectionUtils.isEmpty(list)) {
            return list;
        }

        // 放宽难度限制
        QueryWrapper<Problem> wrapper2 = new QueryWrapper<>();
        wrapper2.select("id", "problem_id", "title", "difficulty");
        wrapper2.eq("auth", 1);
        wrapper2.eq("is_group", false);
        wrapper2.in("id", candidatePids);
        wrapper2.orderByDesc("gmt_modified");
        wrapper2.last("limit 500");
        return problemEntityService.list(wrapper2);
    }

    private List<Problem> loadFallbackProblems(Set<Long> solvedSet, Long excludePid, int targetDifficulty) {
        QueryWrapper<Problem> wrapper = new QueryWrapper<>();
        wrapper.select("id", "problem_id", "title", "difficulty");
        wrapper.eq("auth", 1);
        wrapper.eq("is_group", false);
        if (!CollectionUtils.isEmpty(solvedSet)) {
            wrapper.notIn("id", solvedSet);
        }
        if (excludePid != null) {
            wrapper.ne("id", excludePid);
        }
        wrapper.between("difficulty", Math.max(1, targetDifficulty - 1), targetDifficulty + 1);
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

    private int score(Problem problem, Map<Long, Set<Long>> pidToTagIds, List<Long> topTagIds, int targetDifficulty) {
        int tagMatch = 0;
        if (!CollectionUtils.isEmpty(topTagIds)) {
            Set<Long> tagSet = pidToTagIds.getOrDefault(problem.getId(), Collections.emptySet());
            for (Long tid : topTagIds) {
                if (tagSet.contains(tid)) {
                    tagMatch++;
                }
            }
        }
        Integer diff = problem.getDifficulty();
        int difficultyPenalty = diff == null ? 0 : Math.abs(diff - targetDifficulty);
        return tagMatch * 10 - difficultyPenalty * 2;
    }

    private long buildSeed(String uid, int index) {
        String date = DateUtil.format(new Date(), "yyyy-MM-dd");
        String key = uid + ":" + date + ":" + index;
        return (long) key.hashCode();
    }

    private String buildReason(List<Long> topTagIds, int targetDifficulty) {
        if (CollectionUtils.isEmpty(topTagIds)) {
            return "难度≈" + targetDifficulty;
        }
        // 取前2个标签名字
        Collection<Tag> tags = tagEntityService.listByIds(topTagIds.stream().limit(2).collect(Collectors.toList()));
        String tagText = tags.stream().map(Tag::getName).filter(Objects::nonNull).collect(Collectors.joining("、"));
        if (StrUtil.isBlank(tagText)) {
            return "难度≈" + targetDifficulty;
        }
        return "根据你常做的标签：" + tagText + "（难度≈" + targetDifficulty + "）";
    }
}
