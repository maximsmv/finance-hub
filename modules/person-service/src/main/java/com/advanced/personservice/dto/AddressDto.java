package com.advanced.personservice.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {

    private UUID id;

    private LocalDateTime created;

    private LocalDateTime updated;

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
