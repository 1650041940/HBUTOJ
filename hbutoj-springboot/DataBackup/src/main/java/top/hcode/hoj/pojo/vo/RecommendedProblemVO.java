package top.hcode.hoj.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedProblemVO {

    private Long id;

    private String problemId;

    private String title;

    private Integer difficulty;

    private List<String> tags;

    /**
     * Explainable reason for recommendation (used for thesis/feature highlight)
     */
    private String reason;
}
