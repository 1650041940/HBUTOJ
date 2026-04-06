package top.hcode.hoj.pojo.entity.rating;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "ProblemDifficultyHistory对象", description = "题目难度(月度调整)历史")
@TableName("problem_difficulty_history")
public class ProblemDifficultyHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long pid;

    @ApiModelProperty(value = "月份，格式yyyy-MM")
    private String month;

    private Integer oldDifficulty;

    private Integer delta;

    private Integer newDifficulty;

    private Integer attemptedUsers;

    private Integer acceptedUsers;

    @ApiModelProperty(value = "AC用户平均提交次数")
    private Double avgAttempts;

    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;
}
