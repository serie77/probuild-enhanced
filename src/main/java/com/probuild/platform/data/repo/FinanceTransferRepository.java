package com.probuild.platform.data.repo;

import com.probuild.platform.data.entity.FinanceTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceTransferRepository extends JpaRepository<FinanceTransfer, String> {
}
