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
@ApiModel(value = "UserPracticeRating对象", description = "用户做题正确率/尝试次数 rating")
@TableName("user_practice_rating")
public class UserPracticeRating implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "uid", type = IdType.INPUT)
    private String uid;

    @ApiModelProperty(value = "做题rating")
    private Integer rating;

    @ApiModelProperty(value = "已AC题目数")
    private Integer solvedCount;

    @ApiModelProperty(value = "最近一次计算月份，格式yyyy-MM")
    private String lastCalcMonth;

    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date gmtModified;
}
