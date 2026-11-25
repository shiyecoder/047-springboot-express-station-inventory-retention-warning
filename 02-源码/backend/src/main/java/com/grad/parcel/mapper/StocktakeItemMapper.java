package com.grad.parcel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.parcel.entity.StocktakeItem;
import org.apache.ibatis.annotations.Mapper;

/** 盘点明细 Mapper */
@Mapper
public interface StocktakeItemMapper extends BaseMapper<StocktakeItem> {
}
