package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退件交接
 */
@Data
@TableName("return_handover")
public class ReturnHandover {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退件申请 ID */
    private Long applyId;

    /** 交接快递员 */
    private Long courierId;

    /** 应退件数 */
    private Integer planCount;

    /** 实收件数 */
    private Integer actualCount;

    /** 交接时间 */
    private LocalDateTime handoverTime;

    /** 接收人签字 */
    private String receiverName;

    /** 凭证图片路径 */
    private String voucherFile;

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
