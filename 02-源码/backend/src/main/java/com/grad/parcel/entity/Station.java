package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 站点
 */
@Data
@TableName("station")
public class Station {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 站点编号 */
    private String stationNo;

    /** 站点名称 */
    private String stationName;

    /** 详细地址 */
    private String address;

    /** 营业时间 */
    private String businessHours;

    /** 负责人用户 ID */
    private Long managerId;

    /** 联系电话 */
    private String phone;

    /** 总容量（位） */
    private Integer capacityTotal;

    /** 状态 启用/停用 */
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
