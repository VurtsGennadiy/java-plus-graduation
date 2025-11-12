package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.dal.entity.Category;
import ru.practicum.dal.entity.Event;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.event.UpdateEventRequest;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.dto.event.EventShortDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {CategoryMapper.class, LocationMapper.class})
public interface EventMapper {

    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "category", source = "event.category")
    EventShortDto toShortDto(Event event);

    @Mapping(target = "category", source = "event.category")
    EventShortDto toShortDto(Event event, Long views, Long confirmedRequests);

    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "location", source = "event.location")
    EventFullDto toFullDto(Event event);

    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "location", source = "event.location")
    EventFullDto toFullDto(Event event, Long views, Long confirmedRequests);

    @Mapping(target = "location", source = "dto.location")
    @Mapping(target = "initiator", source = "userId")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "state", expression = "java(ru.practicum.interaction.dto.event.EventState.PENDING)")
    Event toEntity(NewEventDto dto, Long userId, Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "category", source = "category")
    void updateEntity(@MappingTarget Event event, UpdateEventRequest request, Category category);
}
