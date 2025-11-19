package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dal.entity.Compilation;
import ru.practicum.dal.entity.Event;
import ru.practicum.dal.repository.CompilationRepository;
import ru.practicum.dal.repository.EventRepository;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.interaction.exception.EntityNotFoundException;
import ru.practicum.interaction.logging.Loggable;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.service.CompilationService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    @Transactional
    @Loggable
    public CompilationDto addCompilation(NewCompilationDto newCompilationDto) {
        List<Event> eventList = eventRepository.findAllById(newCompilationDto.getEvents());
        Compilation compilation = compilationMapper.toEntity(newCompilationDto, new LinkedHashSet<>(eventList));
        compilationRepository.save(compilation);
        log.info("Сохранена новая подборка событий {} id = {}", compilation.getTitle(), compilation.getId());
        return compilationMapper.toDto(compilation);
    }

    @Override
    @Transactional
    @Loggable
    public void deleteCompilation(Long compId) {
        compilationRepository.deleteById(compId);
        log.info("Удалена подборка событий id {}", compId);
    }

    @Override
    @Transactional
    @Loggable
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        Compilation compilation = getCompilationByIdOrElseThrow(compId);
        Set<Event> updateEvents = new LinkedHashSet<>();
        if (request.getEvents() != null) {
            List<Event> eventList = eventRepository.findAllById(request.getEvents());
            updateEvents.addAll(eventList);
        }
        compilationMapper.updateEntity(compilation, request, updateEvents);
        compilationRepository.save(compilation);
        log.info("Обновлена подборка событий {} id = {}", compilation.getTitle(), compilation.getId());
        return compilationMapper.toDto(compilation);
    }

    @Override
    @Transactional
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        return compilationRepository.findByPinned(pinned, PageRequest.of(from / size, size)).stream()
                .map(compilationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Loggable
    public CompilationDto getCompilation(Long compId) {
        Compilation compilation = getCompilationByIdOrElseThrow(compId);
        return compilationMapper.toDto(compilation);
    }

    private Compilation getCompilationByIdOrElseThrow(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new EntityNotFoundException("Compilation", String.valueOf(compId)));
    }
}
