package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 快递员
 */
@Data
@TableName("courier")
public class Courier {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 快递员工号 */
    private String courierNo;

    /** 姓名 */
    private String courierName;

    /** 所属快递公司 */
    private Long companyId;

    /** 手机号 */
    private String phone;

    /** 服务站点 */
    private Long stationId;

    /** 关联登录账号 */
    private Long userId;

    /** 状态 在职/离职 */
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
