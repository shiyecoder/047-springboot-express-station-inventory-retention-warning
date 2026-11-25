package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 滞留预警
 */
@Data
@TableName("retention_warning")
public class RetentionWarning {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 快件 ID */
    private Long parcelId;

    /** 在库天数 */
    private Integer retentionDays;

    /** 等级 正常/即将滞留/已滞留/待退回 */
    private String level;

    /** 已通知次数 */
    private Integer notifyCount;

    /** 处理状态 待处理/已催取/已处理 */
    private String handleStatus;

    /** 处理备注 */
    private String handleNote;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理人 */
    private Long handlerId;

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
