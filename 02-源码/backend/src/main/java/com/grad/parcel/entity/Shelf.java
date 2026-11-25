package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 货架
 */
@Data
@TableName("shelf")
public class Shelf {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 货架编号 A-01 */
    private String shelfNo;

    /** 所属站点 */
    private Long stationId;

    /** 区域 A/B/C/D */
    private String areaCode;

    /** 适用件型 小件/中件/大件/异形件 */
    private String shelfType;

    /** 容量（位） */
    private Integer capacity;

    /** 已占用位数 */
    private Integer usedCount;

    /** 层位数 */
    private Integer levelCount;

    /** 状态 可用/满载/停用 */
    private String status;

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
