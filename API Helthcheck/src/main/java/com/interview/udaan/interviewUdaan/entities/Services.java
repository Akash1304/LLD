package com.interview.udaan.interviewUdaan.entities;

import com.interview.udaan.interviewUdaan.enums.HealthStatus;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Entity
@Table(name = "Services")
@Data
public class Services extends AbstractEntity {

    @Column(name = "service_name",unique = true)
    private String name;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "health_status")
    private HealthStatus healthStatus = HealthStatus.UNDEFINED;
}
