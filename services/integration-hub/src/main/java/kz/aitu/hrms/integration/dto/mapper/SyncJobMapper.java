package kz.aitu.hrms.integration.dto.mapper;

import kz.aitu.hrms.integration.domain.SyncJob;
import kz.aitu.hrms.integration.dto.sync.SyncJobDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SyncJobMapper {

    SyncJobDto toDto(SyncJob entity);
}
