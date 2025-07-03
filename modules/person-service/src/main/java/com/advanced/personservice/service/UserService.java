package com.advanced.personservice.service;

import com.advanced.contract.model.UserDto;
import com.advanced.personservice.mapper.UserMapper;
import com.advanced.personservice.model.Address;
import com.advanced.personservice.model.Individual;
import com.advanced.personservice.model.User;
import com.advanced.personservice.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final AddressService addressService;

    private final IndividualService individualService;

    private final UserMapper userMapper;

    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.debug("Creating user: {}", userDto);
        Address address = addressService.createAddress(userDto.getAddress());

        User user = userMapper.toEntity(userDto);
        user.setAddress(address);
        userRepository.save(user);

        Individual individual = individualService.createIndividual(userDto.getIndividual(), user);

        userRepository.flush();
        UserDto result = userMapper.toDto(user);
        result.setIndividual(individualService.toDto(individual));
        return result;
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        UserDto result = userMapper.toDto(user);
        result.setAddress(Objects.nonNull(user.getAddress()) ? addressService.toDto(user.getAddress()) : null);
        Individual individual = individualService.getByUserId(user.getId());
        result.setIndividual(Objects.nonNull(individual) ? individualService.toDto(individual) : null);
        return result;
    }

    @Transactional
    public UserDto updateUser(
            @NotNull UUID id,
            @NotNull @Valid UserDto userDto) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        userMapper.updateUserFromDto(userDto, user);

        Address address = addressService.updateAddress(userDto.getAddress());
        Individual individual = individualService.updateIndividual(userDto.getIndividual(), user);

        userRepository.save(user);
        return mapUserToDto(user, address, individual);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (Objects.nonNull(user.getAddress())) {
            addressService.deleteById(user.getAddress().getId());
        }

        individualService.deleteByUserId(id);
        userRepository.deleteById(id);
    }

    private UserDto mapUserToDto(User user, Address address, Individual individual) {
        UserDto result = userMapper.toDto(user);
        result.setAddress(addressService.toDto(address));
        result.setIndividual(individualService.toDto(individual));
        return result;
    }
}
