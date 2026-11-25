package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 快件
 */
@Data
@TableName("parcel")
public class Parcel {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 运单号（站内唯一） */
    private String waybillNo;

    /** 快递公司 */
    private Long companyId;

    /** 收件人 */
    private String receiverName;

    /** 收件人手机号 */
    private String receiverPhone;

    /** 手机号后四位 */
    private String phoneTail;

    /** 件型 小件/中件/大件/异形件 */
    private String itemType;

    /** 尺寸描述 */
    private String sizeDesc;

    /** 特殊件标记 无/贵重件/生鲜冷藏/易碎品 */
    private String specialFlag;

    /** 所属站点 */
    private Long stationId;

    /** 交接批次 */
    private Long batchId;

    /** 货架 ID */
    private Long shelfId;

    /** 货架编号（冗余） */
    private String shelfNo;

    /** 取件码 */
    private String pickupCode;

    /** 状态 待上架/在库/已通知/已取件/待退回/已退件/异常件 */
    private String status;

    /** 到站入库时间 */
    private LocalDateTime arriveTime;

    /** 上架时间 */
    private LocalDateTime shelfTime;

    /** 已通知次数 */
    private Integer notifyCount;

    /** 最后通知时间 */
    private LocalDateTime lastNotifyTime;

    /** 取件时间 */
    private LocalDateTime pickupTime;

    /** 在库天数 */
    private Integer retentionDays;

    /** 经办人 */
    private Long handlerId;

    /** 包裹照片（相对路径 /uploads/seed/parcel/NN.jpg） */
    private String coverImage;

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
