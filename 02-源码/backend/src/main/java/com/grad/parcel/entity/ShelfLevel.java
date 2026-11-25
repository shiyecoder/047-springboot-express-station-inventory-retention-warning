package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 货架层位
 */
@Data
@TableName("shelf_level")
public class ShelfLevel {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 货架 ID */
    private Long shelfId;

    /** 层号 */
    private Integer levelNo;

    /** 本层容量 */
    private Integer capacity;

    /** 本层已用 */
    private Integer usedCount;

    /** 状态 可用/停用 */
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
