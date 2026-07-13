package com.probuild.platform.data.repo;

import com.probuild.platform.data.entity.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, String> {
}
