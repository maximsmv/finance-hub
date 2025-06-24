package com.advanced.personservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private UUID id;

    @Size(max = 32)
    private String secretKey;

    @Email
    @Size(max = 1024)
    private String email;

    private LocalDateTime created;

    private LocalDateTime updated;

    @Size(max = 32)
    private String firstName;

    @Size(max = 32)
    private String lastName;

    private Boolean filled;

    private AddressDto address;

    private IndividualDto individual;
}
