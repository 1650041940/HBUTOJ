package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "RecommendProblemVO", description = "推荐题目简要信息")
public class RecommendProblemVO implements Serializable {

    @ApiModelProperty(value = "题目id")
    private Long pid;

    @ApiModelProperty(value = "题目展示id")
    private String problemId;

    private String title;

    @ApiModelProperty(value = "题目难度分")
    private Integer difficulty;
}
