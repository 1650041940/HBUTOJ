package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "RatingRankVO", description = "rating排行榜条目")
public class RatingRankVO implements Serializable {

    @ApiModelProperty(value = "用户uuid")
    private String uid;

    private String username;

    private String nickname;

    private Integer rating;

    @ApiModelProperty(value = "比赛场次/做题AC数")
    private Integer count;
}
