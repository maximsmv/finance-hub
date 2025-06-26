package com.advanced.personservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "countries", schema = "person")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"id"})
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime created;

    @UpdateTimestamp
    @Column
    private OffsetDateTime updated;

    @Column(length = 32)
    private String name;

    @Column(length = 2)
    private String alpha2;

    @Column(length = 3)
    private String alpha3;

    @Column(length = 32)
    private String status;

}
