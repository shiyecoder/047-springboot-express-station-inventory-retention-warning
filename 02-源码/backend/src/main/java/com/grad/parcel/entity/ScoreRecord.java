package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评分留痕
 */
@Data
@TableName("score_record")
public class ScoreRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 快件 ID */
    private Long parcelId;

    /** 规则 ID */
    private Long ruleId;

    /** 规则版本 */
    private String ruleVersion;

    /** 输入快照 JSON */
    private String inputJson;

    /** 各项得分 JSON */
    private String scoresJson;

    /** 总分 */
    private BigDecimal totalScore;

    /** 等级 优选/可用/受限 */
    private String grade;

    /** 推荐货架 */
    private String recommendShelf;

    /** 结果解释文本 */
    private String recommendText;

    /** 人工调整结果 */
    private String manualAdjust;

    /** 调整原因 */
    private String adjustReason;

    /** 调整人 */
    private Long adjustUserId;

    /** 调整时间 */
    private LocalDateTime adjustTime;

    /** 创建人 */
    private Long createBy;

    /** 版本号（并发校验） */
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
