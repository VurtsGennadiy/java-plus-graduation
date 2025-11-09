package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction.exception.EntityNotFoundException;
import ru.practicum.interaction.logging.Loggable;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.interaction.dto.user.UserDto;
import ru.practicum.user.dal.User;
import ru.practicum.interaction.exception.BadRequestException;
import ru.practicum.interaction.exception.ConflictException;
import ru.practicum.user.dal.UserRepository;
import ru.practicum.user.dto.UserMapper;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Loggable
    @Transactional
    public UserDto addUser(NewUserRequest newUserRequest) {
        validateEmail(newUserRequest.getEmail());
        User user = userMapper.toEntity(newUserRequest);
        userRepository.save(user);
        log.info("Сохранён новый пользователь: {}", user);
        return userMapper.toDto(user);
    }

    @Override
    @Loggable
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId.toString()));
        userRepository.delete(user);
        log.info("Удалён пользователь id = '{}'", userId);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        PageRequest pageRequest = PageRequest.of(from / size, size);
        Page<User> usersPage;
        if (ids == null || ids.isEmpty()) {
            usersPage = userRepository.findAll(pageRequest);
        } else {
            usersPage =  userRepository.findAllByIdIn(ids, pageRequest);
        }
        return userMapper.toDto(usersPage.getContent());
    }

    private void validateEmail(String email) {
        int separator = email.indexOf("@");
        if (email.substring(0, separator).length() > 64) {
            throw new BadRequestException("Локальная часть адреса электронной почты не может быть больше 64 символов");
        }
        if (email.substring(separator).length() > 64 && email.length() != 254) {
            throw new BadRequestException("Доменная часть адреса электронной почты не может быть больше 63 символов");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Указанный адрес электронной почты уже зарегистрирован");
        }
    }
}
