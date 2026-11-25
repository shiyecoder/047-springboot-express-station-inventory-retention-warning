package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退件申请
 */
@Data
@TableName("return_apply")
public class ReturnApply {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退件单号 TJyyyymmdd-序号 */
    private String applyNo;

    /** 快件 ID */
    private Long parcelId;

    /** 运单号（冗余） */
    private String waybillNo;

    /** 原因 拒收/超期未取/地址错误/破损退件/收件人要求 */
    private String reasonType;

    /** 原因说明 */
    private String reason;

    /** 申请人 */
    private Long applyUserId;

    /** 确认人（负责人） */
    private Long approveUserId;

    /** 确认时间 */
    private LocalDateTime approveTime;

    /** 状态 待负责人确认/待交接/已交接/已驳回 */
    private String status;

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
