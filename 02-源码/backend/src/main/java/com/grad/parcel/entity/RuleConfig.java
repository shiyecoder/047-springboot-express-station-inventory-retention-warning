package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 规则配置
 */
@Data
@TableName("rule_config")
public class RuleConfig {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 规则版本号 v1/v2/v3 */
    private String ruleVersion;

    /** 六因子权重 JSON */
    private String weightsJson;

    /** 分级阈值 JSON */
    private String thresholdJson;

    /** 状态 生效中/已归档 */
    private String status;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 操作人 */
    private Long operatorId;

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
