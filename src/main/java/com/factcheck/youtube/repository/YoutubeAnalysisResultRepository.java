package com.factcheck.youtube.repository;

import com.factcheck.youtube.entity.YoutubeAnalysisRequest;
import com.factcheck.youtube.entity.YoutubeAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface YoutubeAnalysisResultRepository extends JpaRepository<YoutubeAnalysisResult, Long> {

    Optional<YoutubeAnalysisResult> findByAnalysisRequest(YoutubeAnalysisRequest analysisRequest);
}
