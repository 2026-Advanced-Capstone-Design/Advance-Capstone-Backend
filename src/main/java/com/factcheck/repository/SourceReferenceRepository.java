package com.factcheck.repository;

import com.factcheck.domain.SourceReference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceReferenceRepository extends JpaRepository<SourceReference, Long> {
}
