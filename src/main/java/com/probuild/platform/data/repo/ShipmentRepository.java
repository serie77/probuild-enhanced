package com.probuild.platform.data.repo;

import com.probuild.platform.data.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, String> {
}
