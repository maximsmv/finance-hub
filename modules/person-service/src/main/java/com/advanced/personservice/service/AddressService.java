package com.advanced.personservice.service;

import com.advanced.contract.model.AddressDto;
import com.advanced.personservice.mapper.AddressMapper;
import com.advanced.personservice.model.Address;
import com.advanced.personservice.model.Country;
import com.advanced.personservice.repository.AddressRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final CountryService countryService;

    private final AddressRepository addressRepository;

    private final AddressMapper addressMapper;

    public Address createAddress(AddressDto addressDto) {
        if (Objects.isNull(addressDto)) {
            return null;
        }

        Address address = addressMapper.toEntity(addressDto);
        Country country = countryService.getCountry(addressDto.getCountry());
        address.setCountry(country);

        addressRepository.save(address);
        return address;
    }

    public Address updateAddress(AddressDto addressDto) {
        if (Objects.isNull(addressDto)) {
            return null;
        }

        if (Objects.isNull(addressDto.getId())) {
            return createAddress(addressDto);
        }

        Address address = addressRepository.findById(addressDto.getId())
                .orElseThrow(EntityNotFoundException::new);
        addressMapper.updateAddressFromDto(addressDto, address);

        if (Objects.nonNull(addressDto.getCountry())) {
            address.setCountry(countryService.getCountry(addressDto.getCountry()));
        }

        return addressRepository.save(address);
    }

    public void deleteById(@NotNull UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(EntityNotFoundException::new);

        address.setArchived(LocalDateTime.now());
        addressRepository.save(address);
    }

    public AddressDto toDto(@NotNull Address address) {
        return addressMapper.toDto(address);
    }

    public Address toEntity(@NotNull AddressDto addressDto) {
        return addressMapper.toEntity(addressDto);
    }
}
