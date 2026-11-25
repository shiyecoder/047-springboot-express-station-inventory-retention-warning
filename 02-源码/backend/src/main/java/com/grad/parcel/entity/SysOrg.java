package com.grad.parcel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 组织站点
 */
@Data
@TableName("sys_org")
public class SysOrg {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 组织/站点名称 */
    private String orgName;

    /** 类型 总部/站点 */
    private String orgType;

    /** 上级 ID */
    private Long parentId;

    /** 联系人 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
