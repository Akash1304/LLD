package com.lld.parkinglot.parkinglot.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "person")
@Data
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(insertable = false, nullable = false)
    private Integer id;

    @Column(name = "name")
    private String name;

}
