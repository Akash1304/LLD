package com.interview.udaan.interviewUdaan.entities;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;


@Entity
@Table(name = "Pings")
@Data
public class Pings extends AbstractEntity {

    @Column(name = "service_id_fk")
    private int serviceIdFk;

    @Column(name = "instance_id_fk")
    private int instanceIdFk;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "timestamp")
    private Date timestamp;
}
