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
@ApiModel(value = "UserPracticeRatingHistory对象", description = "用户做题rating月度历史")
@TableName("user_practice_rating_history")
public class UserPracticeRatingHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String uid;

    @ApiModelProperty(value = "月份，格式yyyy-MM")
    private String month;

    private Integer oldRating;

    private Integer delta;

    private Integer newRating;

    private Integer solvedCount;

    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;
}
