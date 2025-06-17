package com.advanced.personservice.service;

import com.advanced.personservice.dto.AddressDto;
import com.advanced.personservice.mapper.AddressMapper;
import com.advanced.personservice.model.Address;
import com.advanced.personservice.model.Country;
import com.advanced.personservice.repository.AddressRepository;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    private final AddressMapper addressMapper;

    public Address createAddress(@Nullable AddressDto addressDto, @Nullable Country country) {
        if (Objects.isNull(addressDto)) {
            return null;
        }

        Address address = addressMapper.toEntity(addressDto);
        address.setCountry(country);

        addressRepository.save(address);
        return address;
    }

}
