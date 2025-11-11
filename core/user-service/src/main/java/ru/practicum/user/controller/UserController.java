package ru.practicum.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.client.UserClient;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.interaction.dto.user.UserDto;
import ru.practicum.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Validated
public class UserController implements UserClient {
    private final UserService userService;

    /**
     * Добавление нового пользователя
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto addUser(@RequestBody @Valid NewUserRequest request) {
        return userService.addUser(request);
    }

    /**
     * Удаление пользователя
     */
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
    }

    /**
     * Получение информации о пользователях
     */
    @GetMapping()
    public List<UserDto> getUsers(@RequestParam(value = "ids", required = false) List<Long> ids,
                                  @RequestParam(defaultValue = "0") @Min(0) Integer from,
                                  @RequestParam(defaultValue = "10") @Positive Integer size) {
        return userService.getUsers(ids, from, size);
    }
}
