package com.factcheck.repository;

import com.factcheck.domain.FactCheckResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FactCheckResultRepository extends JpaRepository<FactCheckResult, Long> {
}
