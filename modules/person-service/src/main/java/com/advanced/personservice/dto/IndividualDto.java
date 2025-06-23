package com.advanced.personservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualDto {

    private UUID id;

    @Size(max = 32)
    private String passportNumber;

    @Size(max = 32)
    private String phoneNumber;

    @Email
    @Size(max = 32)
    private String email;

    @NotNull
    private LocalDateTime verifiedAt;

    @NotNull
    private LocalDateTime archivedAt;

    @Size(max = 32)
    private String status;

}
