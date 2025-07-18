package com.advanced.personservice.controller;

import com.advanced.contract.model.UserDto;
import com.advanced.personservice.service.UserService;
import io.micrometer.core.annotation.Timed;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/users")
public class UserRestControllerV1 {

    private final UserService userService;

    @Timed
    @WithSpan
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@NotNull @Valid @RequestBody UserDto userDto) {
        return userService.createUser(userDto);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserDto getUserById(@NotNull @PathVariable UUID id) {
        return userService.getUserById(id);
    }

    @Timed
    @WithSpan
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserDto updateUserById(
            @NotNull @PathVariable UUID id,
            @NotNull @Valid @RequestBody UserDto userDto
    ) {
        return userService.updateUser(id, userDto);
    }

    @Timed
    @WithSpan
    @DeleteMapping("/{id}/compensate")
    @ResponseStatus(HttpStatus.OK)
    public void compensateCreateUser(@NotNull @PathVariable UUID id) {
        userService.deleteUser(id);
    }

}
