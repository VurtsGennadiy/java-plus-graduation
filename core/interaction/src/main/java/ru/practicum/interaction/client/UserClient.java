package ru.practicum.interaction.client;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.interaction.dto.user.UserDto;

import java.util.List;

@FeignClient(name = "user")
public interface UserClient {
    @GetMapping()
    List<UserDto> getUsers(@RequestParam(value = "ids", required = false) List<Long> ids,
                           @RequestParam(defaultValue = "0") @Min(0) Integer from,
                           @RequestParam(defaultValue = "10") @Positive Integer size);
}
