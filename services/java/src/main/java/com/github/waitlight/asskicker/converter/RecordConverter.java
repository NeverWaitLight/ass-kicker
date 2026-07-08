package com.github.waitlight.asskicker.converter;

import com.github.waitlight.asskicker.dto.record.RecordVO;
import com.github.waitlight.asskicker.model.RecordEntity;
import org.mapstruct.Mapper;

/**
 * Record 转换器，仅用于 Service 层 Entity 与 VO 之间的转换
 */
@Mapper(componentModel = "spring")
public interface RecordConverter {

    RecordVO toVO(RecordEntity entity);
}
