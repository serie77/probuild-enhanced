package com.probuild.platform.data.repo;

import com.probuild.platform.data.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, String> {
}
