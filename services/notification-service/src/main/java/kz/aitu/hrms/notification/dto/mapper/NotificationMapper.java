package kz.aitu.hrms.notification.dto.mapper;

import kz.aitu.hrms.notification.domain.Notification;
import kz.aitu.hrms.notification.dto.NotificationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "isRead", source = "read")
    NotificationDto toDto(Notification notification);
}
