package com.advanced.personservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {

    @NotNull
    private CountryDto country;

    @Size(max = 128)
    private String address;

    @Size(max = 32)
    private String zipCode;

    @NotNull
    private LocalDateTime archived;

    @Size(max = 32)
    private String city;

    @Size(max = 32)
    private String state;

}
