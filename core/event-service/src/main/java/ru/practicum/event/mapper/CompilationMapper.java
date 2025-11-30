package ru.practicum.event.mapper;

import org.mapstruct.*;
import ru.practicum.event.dal.entity.Compilation;
import ru.practicum.event.dal.entity.Event;
import ru.practicum.event.dto.compilation.CompilationDto;
import ru.practicum.event.dto.compilation.NewCompilationDto;
import ru.practicum.event.dto.compilation.UpdateCompilationRequest;

import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {EventMapper.class})
public interface CompilationMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", source = "events")
    Compilation toEntity(NewCompilationDto dto, Set<Event> events);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", source = "events")
    void updateEntity(@MappingTarget Compilation compilation, UpdateCompilationRequest request, Set<Event> events);

    CompilationDto toDto(Compilation compilation);
}
