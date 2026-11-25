package com.grad.parcel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.parcel.entity.StocktakeTask;
import org.apache.ibatis.annotations.Mapper;

/** 盘点任务 Mapper */
@Mapper
public interface StocktakeTaskMapper extends BaseMapper<StocktakeTask> {
}
