package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "UserSolvedProblemStatVO", description = "用户已AC题目统计(含尝试次数)")
public class UserSolvedProblemStatVO implements Serializable {

    private String uid;

    private Long pid;

    @ApiModelProperty(value = "题目难度分")
    private Integer difficultyRating;

    @ApiModelProperty(value = "从首次提交到AC的提交次数(近似)")
    private Integer attempts;
}
