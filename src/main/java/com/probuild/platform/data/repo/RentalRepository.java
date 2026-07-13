package com.probuild.platform.data.repo;

import com.probuild.platform.data.entity.Rental;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalRepository extends JpaRepository<Rental, String> {
}
