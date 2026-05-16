package kz.aitu.hrms.integration.dto.mapper;

import kz.aitu.hrms.integration.domain.CompanySetting;
import kz.aitu.hrms.integration.dto.settings.SettingDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SettingsMapper {

    SettingDto toDto(CompanySetting entity);
}
