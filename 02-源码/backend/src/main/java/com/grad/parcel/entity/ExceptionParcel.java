package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 异常件
 */
@Data
@TableName("exception_parcel")
public class ExceptionParcel {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 异常单号 YCyyyymmdd-序号 */
    private String exceptionNo;

    /** 快件 ID */
    private Long parcelId;

    /** 运单号（冗余） */
    private String waybillNo;

    /** 类型 外包装破损/内件破损/渗漏/错件/丢失/信息不符/疑似重复件 */
    private String exType;

    /** 发现环节 入库扫描/上架核对/取件核验/在库盘点 */
    private String findStage;

    /** 责任方 快递公司/驿站/收件人/待认定 */
    private String dutySide;

    /** 情况说明 */
    private String description;

    /** 现场图片（逗号分隔相对路径） */
    private String images;

    /** 处理过程 */
    private String handleProcess;

    /** 处理结果 */
    private String result;

    /** 状态 待处理/处理中/待负责人审核/已关闭 */
    private String status;

    /** 登记人 */
    private Long registrarId;

    /** 审核人 */
    private Long auditUserId;

    /** 审核时间 */
    private LocalDateTime auditTime;

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
