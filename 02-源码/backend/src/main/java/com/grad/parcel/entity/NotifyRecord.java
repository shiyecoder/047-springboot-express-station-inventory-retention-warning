package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 通知记录
 */
@Data
@TableName("notify_record")
public class NotifyRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 快件 ID */
    private Long parcelId;

    /** 方式 电话/站内消息/短信 */
    private String notifyType;

    /** 通知内容 */
    private String content;

    /** 结果 已通知/无人接听/失败 */
    private String result;

    /** 经办人 */
    private Long operatorId;

    /** 通知时间 */
    private LocalDateTime notifyTime;

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
