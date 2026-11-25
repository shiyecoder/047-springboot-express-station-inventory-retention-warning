package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代取授权
 */
@Data
@TableName("proxy_auth")
public class ProxyAuth {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 快件 ID（0 表示按手机号全量授权） */
    private Long parcelId;

    /** 收件人 */
    private String receiverName;

    /** 收件人手机号 */
    private String receiverPhone;

    /** 代取人姓名 */
    private String proxyName;

    /** 代取人手机号 */
    private String proxyPhone;

    /** 授权方式 线上登记/现场登记 */
    private String authType;

    /** 有效期至 */
    private LocalDateTime validUntil;

    /** 状态 有效/已使用/已失效 */
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
