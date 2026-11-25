package com.grad.parcel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.parcel.entity.PickupRecord;
import org.apache.ibatis.annotations.Mapper;

/** 取件记录 Mapper */
@Mapper
public interface PickupRecordMapper extends BaseMapper<PickupRecord> {
}
