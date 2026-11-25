package com.grad.parcel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.parcel.entity.Courier;
import org.apache.ibatis.annotations.Mapper;

/** 快递员 Mapper */
@Mapper
public interface CourierMapper extends BaseMapper<Courier> {
}
