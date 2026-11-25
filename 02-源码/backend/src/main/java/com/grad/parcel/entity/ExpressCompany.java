package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 快递公司
 */
@Data
@TableName("express_company")
public class ExpressCompany {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司编码 SF/JD/ZTO */
    private String companyCode;

    /** 公司名称 */
    private String companyName;

    /** 对接人 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /** 约定分区 A/B/C/D */
    private String areaCode;

    /** 状态 合作中/已停用 */
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
