package com.probuild.platform.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** A FixPro maintenance record for a serviced batch. */
@Entity
@Table(name = "SERVICE_RECORDS")
public class ServiceRecord {
    @Id @Column(name = "SERVICE_REFERENCE") public String serviceReference;
    @Column(name = "JOURNEY_ID") public String journeyId;
    @Column(name = "TOOL_COUNT") public int toolCount;
    @Column(name = "ROUTINE_COUNT") public int routineCount;
    @Column(name = "REPAIR_COUNT") public int repairCount;
    @Column(name = "OUT_OF_SERVICE_COUNT") public int outOfServiceCount;
    @Column(name = "STATUS") public String status;
    @Column(name = "CREATED_AT") public LocalDateTime createdAt;
    @Column(name = "UPDATED_AT") public LocalDateTime updatedAt;
}
