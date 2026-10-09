package com.mb.callassistant.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Truy cập {@link CallKeyin}; nền idempotent theo callId (BR5.2). */
public interface CallKeyinRepository extends JpaRepository<CallKeyin, String> {

    Optional<CallKeyin> findByCallId(String callId);
}
