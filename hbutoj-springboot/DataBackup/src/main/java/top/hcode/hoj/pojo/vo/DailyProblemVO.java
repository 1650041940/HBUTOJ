package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DailyProblemVO {

    @ApiModelProperty(value = "题目主键id")
    private Long id;

    @ApiModelProperty(value = "题目的自定义ID 例如（HBUTOJ-1000）")
    private String problemId;

    @ApiModelProperty(value = "题目标题")
    private String title;

    @ApiModelProperty(value = "难度")
    private Integer difficulty;

    @ApiModelProperty(value = "难度分")
    private Integer difficultyRating;

    @ApiModelProperty(value = "标签")
    private List<String> tags;

    @ApiModelProperty(value = "推荐理由（可为空）")
    private String reason;
}
