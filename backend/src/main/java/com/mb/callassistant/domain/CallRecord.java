package com.mb.callassistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One customer call. Carries the {@code recordingName} — the filename of the
 * recording this call maps to, obtained from the FreeSWITCH recordings server's
 * "latest" endpoint when the call ends — and the status of the download → STT
 * pipeline.
 *
 * <p><b>Mapping (demo-simple, FR2.2):</b> there is no UUID resolution and no
 * dialplan key. On hang up, the backend asks the recording server for the most
 * recent recording filename ({@code GET /api/recordings/latest}) and stores it
 * here; that filename IS the call↔recording link.
 */
@Entity
@Table(name = "call_record")
public class CallRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Lead this call was placed from (U1 ownership invariant, FR1.4). Null on legacy demo rows. */
    @Column(name = "lead_id")
    private String leadId;

    /** RM who placed the call (ownership basis, FR1.4). Null on legacy demo rows. */
    @Column(name = "rm_id")
    private String rmId;

    /** Branch/TNB code supplied by the frontend. */
    @Column(name = "tnb_code")
    private String tnbCode;

    /** Dialed customer phone number / extension. */
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    /** Relationship Manager name (optional in the core slice). */
    @Column(name = "rm_name")
    private String rmName;

    @Column(name = "started_at")
    private Instant startedAt;

    /** Set once when the call ends (idempotent, AC1.1.3). Null while in progress. */
    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * Filename of the recording this call maps to (FR2.2). Set on
     * {@code POST /api/calls/{id}/ended} from the recording server's
     * {@code GET /api/recordings/latest} response — the single stereo {@code .wav}
     * produced for the call. Null until the call ends (status PENDING).
     */
    @Column(name = "recording_name")
    private String recordingName;

    /** Local path where a downloaded recording was stored, once fetched (FR3). */
    @Column(name = "recording_path")
    private String recordingPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "stt_status", nullable = false)
    private SttStatus sttStatus = SttStatus.PENDING;

    /** Pipeline state (SM2, BR4.3). Only U1 writes this (BR4.2). */
    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_status", nullable = false)
    private PipelineStatus pipelineStatus = PipelineStatus.IN_CALL;

    /** Current pipeline step while PROCESSING; null at terminal states (BR4.3). */
    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_step")
    private PipelineStep pipelineStep;

    /** Human-readable error detail when the pipeline fails (not surfaced to the DB as a secret). */
    @Column(name = "error_detail")
    private String errorDetail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected CallRecord() {
        // JPA
    }

    public CallRecord(String tnbCode, String phoneNumber, String recordingName) {
        this.tnbCode = tnbCode;
        this.phoneNumber = phoneNumber;
        this.recordingName = recordingName;
        this.sttStatus = SttStatus.PENDING;
        this.createdAt = Instant.now();
    }

    /** Open a call record before any recording exists (FR2.2); recordingName is set on /ended. */
    public CallRecord(String tnbCode, String phoneNumber) {
        this(tnbCode, phoneNumber, null);
    }

    /**
     * Open a call record from a lead (FR1.5): binds {@code leadId}+{@code rmId},
     * starts {@link PipelineStatus#IN_CALL} with {@code startedAt=now}.
     */
    public CallRecord(String leadId, String rmId, String phoneNumber, Instant startedAt) {
        this.leadId = leadId;
        this.rmId = rmId;
        this.phoneNumber = phoneNumber;
        this.startedAt = startedAt;
        this.pipelineStatus = PipelineStatus.IN_CALL;
        this.sttStatus = SttStatus.PENDING;
        this.createdAt = Instant.now();
    }

    /**
     * Idempotent end (AC1.1.3, BR4.1). The first call sets {@code endedAt},
     * computes {@code durationSeconds} and moves {@code IN_CALL -> PROCESSING},
     * returning {@code true} so the caller runs the pipeline exactly once. A
     * later call is a no-op and returns {@code false}.
     *
     * @param now the end instant
     * @return {@code true} only on the first (effective) end
     */
    public boolean markEnded(Instant now) {
        if (endedAt != null) {
            return false;
        }
        this.endedAt = now;
        if (startedAt != null) {
            long secs = Math.max(0, java.time.temporal.ChronoUnit.SECONDS.between(startedAt, now));
            this.durationSeconds = (int) secs;
        }
        if (pipelineStatus == PipelineStatus.IN_CALL) {
            this.pipelineStatus = PipelineStatus.PROCESSING;
        }
        return true;
    }

    public String getLeadId() {
        return leadId;
    }

    public void setLeadId(String leadId) {
        this.leadId = leadId;
    }

    public String getRmId() {
        return rmId;
    }

    public void setRmId(String rmId) {
        this.rmId = rmId;
    }

    public Long getId() {
        return id;
    }

    public String getTnbCode() {
        return tnbCode;
    }

    public void setTnbCode(String tnbCode) {
        this.tnbCode = tnbCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRmName() {
        return rmName;
    }

    public void setRmName(String rmName) {
        this.rmName = rmName;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public PipelineStatus getPipelineStatus() {
        return pipelineStatus;
    }

    public void setPipelineStatus(PipelineStatus pipelineStatus) {
        this.pipelineStatus = pipelineStatus;
    }

    public PipelineStep getPipelineStep() {
        return pipelineStep;
    }

    public void setPipelineStep(PipelineStep pipelineStep) {
        this.pipelineStep = pipelineStep;
    }

    public String getRecordingName() {
        return recordingName;
    }

    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
    }

    public String getRecordingPath() {
        return recordingPath;
    }

    public void setRecordingPath(String recordingPath) {
        this.recordingPath = recordingPath;
    }

    public SttStatus getSttStatus() {
        return sttStatus;
    }

    public void setSttStatus(SttStatus sttStatus) {
        this.sttStatus = sttStatus;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
