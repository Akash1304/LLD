package com.interview.udaan.interviewUdaan.entities;

import com.interview.udaan.interviewUdaan.enums.HealthStatus;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "Instances")
@Data
public class Instances extends AbstractEntity {

    @Column(name = "instance_name",unique = true)
    private String name;

    @Column(name = "service_id_fk")
    private Integer serviceIdFk;

}
