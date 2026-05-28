package com.zooreserve.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zooreserve.domain.entity.TicketType;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketTypeMapper extends BaseMapper<TicketType> {
}
