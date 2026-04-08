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
@ApiModel(value = "ContestRatingEvent对象", description = "比赛rating处理事件(避免重复计算)")
@TableName("contest_rating_event")
public class ContestRatingEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "cid", type = IdType.INPUT)
    private Long cid;

    @ApiModelProperty(value = "是否已处理")
    private Boolean processed;

    @ApiModelProperty(value = "参赛人数")
    private Integer participants;

    @ApiModelProperty(value = "处理时间")
    private Date processedTime;

    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date gmtModified;
}
