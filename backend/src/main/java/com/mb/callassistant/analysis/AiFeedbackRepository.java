package com.mb.callassistant.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Truy cập {@link AiFeedback}; tối đa một đánh giá / cuộc gọi. */
public interface AiFeedbackRepository extends JpaRepository<AiFeedback, String> {

    Optional<AiFeedback> findByCallId(String callId);
}
