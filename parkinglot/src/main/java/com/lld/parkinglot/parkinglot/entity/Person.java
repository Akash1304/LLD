package com.lld.parkinglot.parkinglot.entity;

import com.lld.parkinglot.parkinglot.enums.AccessType;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "persons")
public class Person extends AbstractEntity {

    @Column(name = "user_name")
    private String name;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "access_types", nullable = false)
    private AccessType accessType;
}
