package ru.practicum.controller.adminAPI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.service.CompilationService;

@Validated
@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
public class AdminCompilationController {
    private final CompilationService compilationService;
    private final String id = "/{compId}";

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto addCompilation(@RequestBody @Valid NewCompilationDto newCompilationDto) {
        return compilationService.addCompilation(newCompilationDto);
    }

    @DeleteMapping(id)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable @Positive Long compId) {
        compilationService.deleteCompilation(compId);
    }

    @PatchMapping(id)
    public CompilationDto updateCompilation(@PathVariable @Positive Long compId,
                                            @RequestBody @Valid UpdateCompilationRequest request) {
        return compilationService.updateCompilation(compId, request);
    }
}
