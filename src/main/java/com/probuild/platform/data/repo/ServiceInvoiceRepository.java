package com.probuild.platform.data.repo;

import com.probuild.platform.data.entity.ServiceInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceInvoiceRepository extends JpaRepository<ServiceInvoice, String> {
}
