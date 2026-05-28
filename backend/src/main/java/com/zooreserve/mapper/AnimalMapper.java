package com.zooreserve.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zooreserve.domain.entity.Animal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnimalMapper extends BaseMapper<Animal> {
}
