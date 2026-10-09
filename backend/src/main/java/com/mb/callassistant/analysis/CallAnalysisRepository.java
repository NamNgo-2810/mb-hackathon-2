package com.mb.callassistant.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Truy cập {@link CallAnalysis}; upsert tại chỗ theo callId (BR5.2). */
public interface CallAnalysisRepository extends JpaRepository<CallAnalysis, String> {

    Optional<CallAnalysis> findByCallId(String callId);
}
