package com.advanced.personservice.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"id"})
public class CountryDto {

    private Integer id;

    @Size(max = 32)
    private String name;

    @Size(max = 2)
    private String alpha2;

    @Size(max = 3)
    private String alpha3;

    @Size(max = 32)
    private String status;

}
