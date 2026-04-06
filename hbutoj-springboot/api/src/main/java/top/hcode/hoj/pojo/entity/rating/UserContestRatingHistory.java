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
@ApiModel(value = "UserContestRatingHistory对象", description = "用户比赛rating按场次历史")
@TableName("user_contest_rating_history")
public class UserContestRatingHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String uid;

    private Long cid;

    private Integer oldRating;

    private Integer delta;

    private Integer newRating;

    @ApiModelProperty(value = "比赛名次")
    private Integer rank;

    @ApiModelProperty(value = "参赛人数")
    private Integer participants;

    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;
}
