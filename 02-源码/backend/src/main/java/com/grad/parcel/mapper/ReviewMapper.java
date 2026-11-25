package com.grad.parcel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.parcel.entity.Review;
import org.apache.ibatis.annotations.Mapper;

/** 评价反馈 Mapper */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
}
