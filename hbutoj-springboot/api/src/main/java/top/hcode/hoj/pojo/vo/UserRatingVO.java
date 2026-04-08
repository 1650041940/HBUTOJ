package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "UserRatingVO", description = "用户双rating信息")
public class UserRatingVO implements Serializable {

    @ApiModelProperty(value = "用户uuid")
    private String uid;

    private String username;

    private String nickname;

    private Integer contestRating;

    private Integer practiceRating;

    private Integer contestCount;

    private Integer solvedCount;
}
