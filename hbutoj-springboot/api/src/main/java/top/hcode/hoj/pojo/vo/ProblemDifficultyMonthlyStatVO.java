package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "ProblemDifficultyMonthlyStatVO", description = "题目月度表现统计")
public class ProblemDifficultyMonthlyStatVO implements Serializable {

    private Long pid;

    @ApiModelProperty(value = "尝试用户数")
    private Integer attemptedUsers;

    @ApiModelProperty(value = "通过用户数")
    private Integer acceptedUsers;

    @ApiModelProperty(value = "AC用户平均提交次数")
    private Double avgAttempts;
}
