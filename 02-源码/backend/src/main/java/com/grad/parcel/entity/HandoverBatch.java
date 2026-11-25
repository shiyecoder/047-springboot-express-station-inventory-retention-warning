package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交接批次
 */
@Data
@TableName("handover_batch")
public class HandoverBatch {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次号 JByyyymmdd-序号 */
    private String batchNo;

    /** 快递公司 */
    private Long companyId;

    /** 快递员 */
    private Long courierId;

    /** 站点 */
    private Long stationId;

    /** 到站时间 */
    private LocalDateTime arriveTime;

    /** 交接单总数 */
    private Integer totalCount;

    /** 实际接收数 */
    private Integer receivedCount;

    /** 拒收数 */
    private Integer rejectedCount;

    /** 差异件数 */
    private Integer diffCount;

    /** 差异说明 */
    private String diffNote;

    /** 状态 交接中/已确认/差异待确认 */
    private String status;

    /** 双方确认时间 */
    private LocalDateTime confirmTime;

    /** 驿站经办人 */
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
