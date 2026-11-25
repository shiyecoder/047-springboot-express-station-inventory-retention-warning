package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 盘点任务
 */
@Data
@TableName("stocktake_task")
public class StocktakeTask {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务号 PDyyyymmdd-序号 */
    private String taskNo;

    /** 站点 */
    private Long stationId;

    /** 盘点货架 ID 列表 */
    private String shelfIds;

    /** 货架数 */
    private Integer shelfCount;

    /** 系统在库件数 */
    private Integer planCount;

    /** 实盘件数 */
    private Integer actualCount;

    /** 盘盈数 */
    private Integer profitCount;

    /** 盘亏数 */
    private Integer lossCount;

    /** 错位数 */
    private Integer mismatchCount;

    /** 状态 进行中/待复核/已完成 */
    private String status;

    /** 盘点人 */
    private Long operatorId;

    /** 完成时间 */
    private LocalDateTime finishTime;

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
