package ru.practicum.user.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.user.dal.User;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User toEntity(NewUserRequest dto);

    UserDto toDto(User user);

    List<UserDto> toDto(List<User> users);

    UserShortDto toShortDto(User user);

    List<UserShortDto> toShortDto(List<User> users);
}
