package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 上架记录
 */
@Data
@TableName("shelf_record")
public class ShelfRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 快件 ID */
    private Long parcelId;

    /** 动作 上架/移库/下架 */
    private String actionType;

    /** 目标货架 */
    private Long shelfId;

    /** 目标货架编号 */
    private String shelfNo;

    /** 原货架编号 */
    private String fromShelfNo;

    /** 操作人 */
    private Long operatorId;

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
