package com.advanced.personservice.service;

import com.advanced.contract.model.AddressDto;
import com.advanced.contract.model.CountryDto;
import com.advanced.contract.model.IndividualDto;
import com.advanced.contract.model.UserDto;
import com.advanced.personservice.mapper.UserMapper;
import com.advanced.personservice.model.Address;
import com.advanced.personservice.model.Individual;
import com.advanced.personservice.model.User;
import com.advanced.personservice.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressService addressService;

    @Mock
    private IndividualService individualService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private UserDto userDto;
    private User user;
    private Address address;
    private AddressDto addressDto;
    private Individual individual;
    private IndividualDto individualDto;
    private CountryDto countryDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        countryDto = new CountryDto();
        countryDto.setId(1);
        countryDto.setName("Russia");
        countryDto.setAlpha2("RU");
        countryDto.setAlpha3("RUS");
        countryDto.setStatus("ACTIVE");

        addressDto = new AddressDto();
        addressDto.setId(UUID.randomUUID());
        addressDto.setCreated(OffsetDateTime.now());
        addressDto.setUpdated(OffsetDateTime.now());
        addressDto.setCountry(countryDto);
        addressDto.setAddress("Lenin St, 123");
        addressDto.setZipCode("123456");
        addressDto.setCity("Moscow");
        addressDto.setState("Moscow Oblast");
        addressDto.setArchived(null);

        individualDto = new IndividualDto();
        individualDto.setId(UUID.randomUUID());
        individualDto.setPassportNumber("1234 567890");
        individualDto.setPhoneNumber("+79991234567");
        individualDto.setVerifiedAt(OffsetDateTime.now());
        individualDto.setStatus("VERIFIED");
        individualDto.setArchivedAt(null);

        userDto = new UserDto();
        userDto.setId(userId);
        userDto.setSecretKey("secret123");
        userDto.setEmail("test@example.com");
        userDto.setCreated(OffsetDateTime.now());
        userDto.setUpdated(OffsetDateTime.now());
        userDto.setFirstName("John");
        userDto.setLastName("Doe");
        userDto.setFilled(true);
        userDto.setAddress(addressDto);
        userDto.setIndividual(individualDto);

        user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");

        address = new Address();
        address.setId(addressDto.getId());
        address.setAddress(addressDto.getAddress());
        address.setZipCode(addressDto.getZipCode());
        address.setCity(addressDto.getCity());
        address.setState(addressDto.getState());

        individual = new Individual();
        individual.setId(individualDto.getId());
        individual.setPassportNumber(individualDto.getPassportNumber());
        individual.setPhoneNumber(individualDto.getPhoneNumber());
        individual.setVerifiedAt(individualDto.getVerifiedAt());
    }

    @Test
    void createUser_shouldCreateUserSuccessfully() {
        when(addressService.createAddress(any(AddressDto.class))).thenReturn(address);
        when(userMapper.toEntity(any(UserDto.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(individualService.createIndividual(any(IndividualDto.class), any(User.class))).thenReturn(individual);
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);
        when(individualService.toDto(any(Individual.class))).thenReturn(individualDto);

        UserDto result = userService.createUser(userDto);

        assertNotNull(result);
        assertEquals(userDto.getEmail(), result.getEmail());
        assertEquals(userDto.getAddress(), result.getAddress());
        assertEquals(userDto.getIndividual(), result.getIndividual());
        verify(addressService).createAddress(userDto.getAddress());
        verify(userMapper).toEntity(userDto);
        verify(userRepository).save(user);
        verify(individualService).createIndividual(userDto.getIndividual(), user);
        verify(userRepository).flush();
        verify(userMapper).toDto(user);
        verify(individualService).toDto(individual);
    }

    @Test
    void getUserById_shouldReturnUserDtoWithAddressAndIndividual_whenBothExist() {
        user.setAddress(address);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);
        when(addressService.toDto(address)).thenReturn(addressDto);
        when(individualService.getByUserId(userId)).thenReturn(individual);
        when(individualService.toDto(individual)).thenReturn(individualDto);

        UserDto result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userDto.getEmail(), result.getEmail());
        assertNotNull(result.getAddress());
        assertEquals(addressDto.getId(), result.getAddress().getId());
        assertNotNull(result.getIndividual());
        assertEquals(individualDto.getId(), result.getIndividual().getId());
        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
        verify(addressService).toDto(address);
        verify(individualService).getByUserId(userId);
        verify(individualService).toDto(individual);
    }

    @Test
    void getUserById_shouldReturnUserDtoWithNullAddressAndIndividual_whenBothNull() {
        user.setAddress(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);
        when(individualService.getByUserId(userId)).thenReturn(null);

        UserDto result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userDto.getEmail(), result.getEmail());
        assertNull(result.getAddress());
        assertNull(result.getIndividual());
        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
        verify(individualService).getByUserId(userId);
        verifyNoInteractions(addressService);
        verifyNoMoreInteractions(individualService);
    }

    @Test
    void getUserById_shouldThrowEntityNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.getUserById(userId));
        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository).findById(userId);
        verifyNoInteractions(userMapper, addressService, individualService);
    }

    @Test
    void updateUser_shouldUpdateUserSuccessfully() {
        user.setAddress(address);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(addressService.updateAddress(any(AddressDto.class))).thenReturn(address);
        when(individualService.updateIndividual(any(IndividualDto.class), any(User.class))).thenReturn(individual);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);
        when(addressService.toDto(any(Address.class))).thenReturn(addressDto);
        when(individualService.toDto(any(Individual.class))).thenReturn(individualDto);

        UserDto result = userService.updateUser(userId, userDto);

        assertNotNull(result);
        assertEquals(userDto.getEmail(), result.getEmail());
        assertEquals(addressDto.getId(), result.getAddress().getId());
        assertEquals(individualDto.getId(), result.getIndividual().getId());
        verify(userRepository).findById(userId);
        verify(userMapper).updateUserFromDto(userDto, user);
        verify(addressService).updateAddress(userDto.getAddress());
        verify(individualService).updateIndividual(userDto.getIndividual(), user);
        verify(userRepository).save(user);
        verify(userMapper).toDto(user);
        verify(addressService).toDto(address);
        verify(individualService).toDto(individual);
    }

    @Test
    void updateUser_shouldThrowEntityNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.updateUser(userId, userDto));
        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository).findById(userId);
        verifyNoInteractions(userMapper, addressService, individualService);
    }

    @Test
    void deleteUser_shouldDeleteUserSuccessfully_whenAddressExists() {
        user.setAddress(address);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUser(userId);

        verify(userRepository).findById(userId);
        verify(addressService).deleteById(address.getId());
        verify(individualService).deleteByUserId(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUser_shouldDeleteUserSuccessfully_whenAddressIsNull() {
        user.setAddress(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUser(userId);

        verify(userRepository).findById(userId);
        verifyNoInteractions(addressService);
        verify(individualService).deleteByUserId(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUser_shouldThrowEntityNotFoundException_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.deleteUser(userId));
        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository).findById(userId);
        verifyNoInteractions(addressService, individualService);
    }
}