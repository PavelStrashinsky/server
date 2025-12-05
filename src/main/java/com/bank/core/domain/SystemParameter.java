package com.bank.core.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "system_parameters")
@AllArgsConstructor
@NoArgsConstructor
public class SystemParameter {
    @Id
    @Column(name = "param_key", unique = true, nullable = false)
    private String paramKey;

    @Column(name = "param_value", nullable = false)
    private String paramValue;

    @Column(name = "description")
    private String description;
}