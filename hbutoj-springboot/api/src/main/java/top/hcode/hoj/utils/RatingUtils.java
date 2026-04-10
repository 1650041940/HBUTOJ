package top.hcode.hoj.utils;

import org.springframework.util.CollectionUtils;
import top.hcode.hoj.pojo.vo.UserSolvedProblemStatVO;

import java.util.List;

public final class RatingUtils {

    public static final int DEFAULT_PRACTICE_RATING = 1200;
    public static final int DEFAULT_CONTEST_RATING = 1500;
    public static final int DEFAULT_PROBLEM_DIFFICULTY_RATING = 1200;

    public static final int MIN_PROBLEM_DIFFICULTY_RATING = 800;
    public static final int MAX_PROBLEM_DIFFICULTY_RATING = 3500;
    public static final int MIN_PRACTICE_RATING = 0;
    public static final int MAX_PRACTICE_RATING = 4000;

    private RatingUtils() {
    }

    public static int normalizeProblemDifficultyRating(Integer difficultyRating) {
        if (difficultyRating == null || difficultyRating <= 0) {
            return DEFAULT_PROBLEM_DIFFICULTY_RATING;
        }

        int clamped = Math.max(MIN_PROBLEM_DIFFICULTY_RATING,
                Math.min(MAX_PROBLEM_DIFFICULTY_RATING, difficultyRating));

        int rounded = Math.round(clamped / 100.0f) * 100;
        if (rounded < MIN_PROBLEM_DIFFICULTY_RATING) {
            return MIN_PROBLEM_DIFFICULTY_RATING;
        }
        if (rounded > MAX_PROBLEM_DIFFICULTY_RATING) {
            return MAX_PROBLEM_DIFFICULTY_RATING;
        }
        return rounded;
    }

    public static boolean isProblemDifficultyRatingValid(Integer difficultyRating) {
        if (difficultyRating == null) {
            return true;
        }
        return difficultyRating >= MIN_PROBLEM_DIFFICULTY_RATING
                && difficultyRating <= MAX_PROBLEM_DIFFICULTY_RATING
                && difficultyRating % 100 == 0;
    }

    public static int clampPracticeRating(int rating) {
        return Math.max(MIN_PRACTICE_RATING, Math.min(MAX_PRACTICE_RATING, rating));
    }

    public static int calculatePracticeAcceptedDelta(int currentRating,
                                                     Integer problemDifficultyRating,
                                                     Integer attempts,
                                                     Integer solvedCount) {
        int userRating = clampPracticeRating(currentRating <= 0 ? DEFAULT_PRACTICE_RATING : currentRating);
        int problemRating = normalizeProblemDifficultyRating(problemDifficultyRating);
        int safeAttempts = attempts == null || attempts < 1 ? 1 : attempts;
        int safeSolvedCount = solvedCount == null || solvedCount < 0 ? 0 : solvedCount;

        int kFactor = safeSolvedCount < 30 ? 40
                : safeSolvedCount < 100 ? 32
                : safeSolvedCount < 250 ? 24
                : 16;

        double expected = 1.0 / (1.0 + Math.pow(10.0, (problemRating - userRating) / 400.0));
        double penalty = Math.min(0.45, 0.08 * Math.max(0, safeAttempts - 1));
        double actual = Math.max(0.45, 1.0 - penalty);

        int delta = (int) Math.round(kFactor * (actual - expected));
        if (delta == 0) {
            delta = actual >= expected ? 1 : -1;
        }
        return delta;
    }

    public static int calculatePracticeRating(List<UserSolvedProblemStatVO> stats) {
        if (CollectionUtils.isEmpty(stats)) {
            return DEFAULT_PRACTICE_RATING;
        }

        int currentRating = DEFAULT_PRACTICE_RATING;
        int solvedCount = 0;
        for (UserSolvedProblemStatVO stat : stats) {
            currentRating = clampPracticeRating(currentRating + calculatePracticeAcceptedDelta(
                    currentRating,
                    stat.getDifficultyRating(),
                    stat.getAttempts(),
                    solvedCount));
            solvedCount++;
        }
        return currentRating;
    }
}
