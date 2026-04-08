package top.hcode.hoj.pojo.entity.common;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @Author: HBUTOJ
 * @Description: 团队获奖记录
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "TeamAward对象", description = "")
public class TeamAward {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "标题")
    private String title;

    @ApiModelProperty(value = "比赛名称")
    private String contestName;

    @ApiModelProperty(value = "奖项/等级")
    private String award;

    @ApiModelProperty(value = "获奖时间")
    private Date awardTime;

    @ApiModelProperty(value = "获奖照片URL")
    private String photo;

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "0可见，1不可见")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date gmtModified;
}
