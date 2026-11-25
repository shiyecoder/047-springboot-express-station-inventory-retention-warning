package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 盘点明细
 */
@Data
@TableName("stocktake_item")
public class StocktakeItem {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 盘点任务 ID */
    private Long taskId;

    /** 快件 ID（盘盈时可为 0） */
    private Long parcelId;

    /** 运单号 */
    private String waybillNo;

    /** 货架编号 */
    private String shelfNo;

    /** 系统件数 */
    private Integer systemCount;

    /** 实盘件数 */
    private Integer actualCount;

    /** 差异类型 无/盘盈/盘亏/错位 */
    private String diffType;

    /** 处理状态 待复核/已调整/已转异常 */
    private String handleStatus;

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
