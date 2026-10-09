package com.mb.callassistant.repository;

import com.mb.callassistant.domain.CallRecord;
import com.mb.callassistant.domain.PipelineStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {

    Optional<CallRecord> findByRecordingName(String recordingName);

    /** Cuộc gọi của hệ thống chưa xử lý xong — giới hạn một cuộc gọi (BR3.2). */
    List<CallRecord> findByPipelineStatusIn(List<PipelineStatus> statuses);
}
