package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.interaction.dto.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.entity.Event;
import ru.practicum.user.dto.UserMapper;

import java.util.Collection;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {CategoryMapperStruct.class, UserMapper.class, LocationMapper.class})
public interface EventMapper {

    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "initiator", source = "event.initiator")
    EventShortDto toShortDto(Event event);

    List<EventShortDto> toShortDto(Collection<Event> events);

    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "initiator", source = "event.initiator")
    @Mapping(target = "location", source = "event.location")
    EventFullDto toFullDto(Event event);

    @Mapping(target = "location", source = "dto.location")
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "state", expression = "java(ru.practicum.interaction.dto.EventState.PENDING)")
    Event toEntity(NewEventDto dto, Long userId);
}
