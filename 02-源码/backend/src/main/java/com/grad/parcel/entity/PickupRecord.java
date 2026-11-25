package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 取件记录
 */
@Data
@TableName("pickup_record")
public class PickupRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 快件 ID */
    private Long parcelId;

    /** 取件方式 本人/代取 */
    private String pickupType;

    /** 核验方式 */
    private String verifyType;

    /** 取件人 */
    private String pickupPerson;

    /** 取件人手机号 */
    private String pickupPhone;

    /** 代取授权 ID */
    private Long proxyId;

    /** 经办员工 */
    private Long operatorId;

    /** 取件时间 */
    private LocalDateTime pickupTime;

    /** 备注 */
    private String remark;

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
