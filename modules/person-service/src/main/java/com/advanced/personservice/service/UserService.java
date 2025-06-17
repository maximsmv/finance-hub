package com.advanced.personservice.service;

import com.advanced.personservice.dto.UserDto;
import com.advanced.personservice.mapper.AddressMapper;
import com.advanced.personservice.mapper.IndividualMapper;
import com.advanced.personservice.mapper.UserMapper;
import com.advanced.personservice.model.Address;
import com.advanced.personservice.model.Country;
import com.advanced.personservice.model.Individual;
import com.advanced.personservice.model.User;
import com.advanced.personservice.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final CountryService countryService;

    private final AddressService addressService;

    private final IndividualService individualService;

    private final UserMapper userMapper;

    private final IndividualMapper individualMapper;
    private final AddressMapper addressMapper;

    @Transactional
    public UserDto createUser(UserDto userDto) {
        Country country = countryService.getCountry(userDto.getAddress().getCountry());

        Address address = addressService.createAddress(userDto.getAddress(), country);

        User user = userMapper.toEntity(userDto);
        user.setAddress(address);
        User savedUser = userRepository.save(user);

        Individual individual = individualService.createIndividual(userDto.getIndividual(), savedUser);

        UserDto result = userMapper.toDto(savedUser);

        result.setAddress(addressMapper.toDto(address));
        result.setIndividual(individualMapper.toDto(individual));

        return result;
    }

    @Transactional
    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return userMapper.toDto(user);
    }

    //TODO Пагинация нужна, так не оставлять
    @Transactional
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .toList();
    }

//    @Transactional
//    public UserDto updateUser(UUID id, UserDto userDto) {
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
//
//        userMapper.updateEntityFromDto(userDto, user);
//
//        if (userDto.getAddress() != null && userDto.getAddress().getId() != null) {
//            Address address = addressRepository.findById(userDto.getAddress().getId())
//                    .orElseThrow(() -> new EntityNotFoundException("Address not found with id: " + userDto.getAddress().getId()));
//            user.setAddress(address);
//        } else {
//            user.setAddress(null);
//        }
//
//        return userMapper.toDto(user);
//    }

    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }
}
