package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 批次明细
 */
@Data
@TableName("handover_item")
public class HandoverItem {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次 ID */
    private Long batchId;

    /** 运单号 */
    private String waybillNo;

    /** 收件人 */
    private String receiverName;

    /** 件型 */
    private String itemType;

    /** 扫描时间 */
    private LocalDateTime scanTime;

    /** 接收结果 已接收/拒收/差异 */
    private String result;

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
