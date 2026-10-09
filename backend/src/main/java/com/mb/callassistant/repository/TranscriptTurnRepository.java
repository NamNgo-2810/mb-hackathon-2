package com.mb.callassistant.repository;

import com.mb.callassistant.domain.TranscriptTurn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranscriptTurnRepository extends JpaRepository<TranscriptTurn, Long> {

    /** All turns for a call, in time order. */
    List<TranscriptTurn> findByCallIdOrderByStartMsAsc(Long callId);

    void deleteByCallId(Long callId);
}
