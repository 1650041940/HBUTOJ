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
@ApiModel(value = "UserContestRating对象", description = "用户比赛 rating")
@TableName("user_contest_rating")
public class UserContestRating implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "uid", type = IdType.INPUT)
    private String uid;

    @ApiModelProperty(value = "比赛rating")
    private Integer rating;

    @ApiModelProperty(value = "参与比赛场次")
    private Integer contestCount;

    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date gmtModified;
}
