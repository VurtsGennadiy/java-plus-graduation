package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.practicum.interaction.dto.event.LocationDto;
import ru.practicum.dal.entity.Location;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LocationMapper {
    LocationDto toDto(Location location);

    Location toEntity(LocationDto dto);
}
