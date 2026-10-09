package com.mb.callassistant.service;

import com.mb.callassistant.config.AppProperties;
import com.mb.callassistant.domain.CallRecord;
import com.mb.callassistant.repository.CallRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Fetches the single stereo recording {@code .wav} for a call from the
 * FreeSWITCH/EC2 recording server (FR3). The recording is located by the call's
 * {@link CallRecord#getRecordingName()} — the filename returned by the server's
 * {@code GET /api/recordings/latest} when the call ended — which is downloaded
 * via {@link RecordingClient} ({@code GET /api/recordings/download/:name}).
 *
 * <p>There is exactly ONE stereo {@code .wav} per call; the two call legs are on
 * separate stereo channels. {@link SttService} splits that stereo file into the
 * RM and KH mono channels.
 *
 * <p>A recording can be a moment late after hangup, so the download is retried
 * with bounded backoff (FR1.3, FR3.2). When nothing can be fetched within the
 * budget, {@link RecordingNotReadyException} is thrown so the caller can mark
 * the call {@code RECORDING_NOT_READY} (non-terminal) rather than failing hard.
 */
@Service
public class RecordingService {

    private static final Logger log = LoggerFactory.getLogger(RecordingService.class);

    private final RecordingClient recordingClient;
    private final AppProperties props;
    private final CallRecordRepository callRecords;

    public RecordingService(RecordingClient recordingClient,
                            AppProperties props,
                            CallRecordRepository callRecords) {
        this.recordingClient = recordingClient;
        this.props = props;
        this.callRecords = callRecords;
    }

    /**
     * Download the single stereo {@code .wav} for a call, retrying with bounded
     * backoff so a file that is a second late is retried rather than failed on
     * first miss. Throws {@link RecordingNotReadyException} when the recording
     * cannot be fetched within the retry budget (FR3.2).
     *
     * @return the raw stereo {@code .wav} bytes
     */
    public byte[] downloadRecording(Long callId) {
        CallRecord call = callRecords.findById(callId)
                .orElseThrow(() -> new RecordingNotReadyException("No call record for id " + callId));
        return downloadRecording(call);
    }

    /**
     * Download the stereo recording for the given record (FR3). Re-reads the
     * latest filename once if the record has none yet (e.g. the call ended before
     * the server flushed the file), so a short delay between hangup and the
     * recording appearing still resolves.
     */
    public byte[] downloadRecording(CallRecord call) {
        String base = props.getRecording().getBaseUrl();
        if (base == null || base.isBlank()) {
            throw new RecordingNotReadyException(
                    "RECORDING_BASE_URL is not configured; cannot fetch the recording for call " + call.getId() + ".");
        }

        int attempts = Math.max(1, props.getRecording().getRetryMaxAttempts());
        long baseDelay = Math.max(0, props.getRecording().getRetryDelayMs());

        for (int attempt = 1; attempt <= attempts; attempt++) {
            String name = resolveName(call);
            if (name != null && !name.isBlank()) {
                byte[] wav = recordingClient.download(name);
                if (wav != null && wav.length > 0) {
                    return wav;
                }
            }
            if (attempt < attempts) {
                long delay = baseDelay * attempt; // simple linear backoff (demo-reasonable)
                log.info("Recording '{}' for call {} not ready (attempt {}/{}); retrying in {} ms.",
                        name, call.getId(), attempt, attempts, delay);
                sleep(delay);
            }
        }

        String name = call.getRecordingName();
        throw new RecordingNotReadyException(
                "Recording for call " + call.getId() + " was not found after " + attempts
                        + " attempts" + (name == null || name.isBlank() ? "" : " (filename '" + name + "')")
                        + ". The call may not have been recorded yet, or the .wav has not flushed to the "
                        + "recording server. Confirm the call ended and GET /api/recordings/latest returns a file.");
    }

    /**
     * The recording filename to download: the one stored on the record, or — if
     * absent — a fresh read of {@code GET /api/recordings/latest}, which is then
     * persisted so later requests reuse it.
     */
    private String resolveName(CallRecord call) {
        String name = call.getRecordingName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return recordingClient.latestFilename().map(fresh -> {
            call.setRecordingName(fresh);
            callRecords.save(call);
            log.info("Backfilled recordingName '{}' for call {} from latest.", fresh, call.getId());
            return fresh;
        }).orElse(null);
    }

    private void sleep(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
