package com.factcheck.youtube.repository;

import com.factcheck.youtube.entity.YoutubeAnalysisRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface YoutubeAnalysisRequestRepository extends JpaRepository<YoutubeAnalysisRequest, Long> {

    Optional<YoutubeAnalysisRequest> findByRequestId(String requestId);
}
