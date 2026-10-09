package com.mb.callassistant.web;

import com.mb.callassistant.domain.CallRecord;
import com.mb.callassistant.repository.TranscriptTurnRepository;
import com.mb.callassistant.service.CallService;
import com.mb.callassistant.service.RecordingService;
import com.mb.callassistant.web.dto.CallDto;
import com.mb.callassistant.web.dto.CreateCallRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Call API (FR1, FR3, FR4):
 * <ul>
 *   <li>{@code POST /api/calls/initiate} — open a call record (frontend dials via MicroSIP).</li>
 *   <li>{@code POST /api/calls/{id}/ended} — close the call, start the pipeline.</li>
 *   <li>{@code GET  /api/calls/{id}} — record + pipeline status.</li>
 *   <li>{@code GET  /api/calls/{id}/recording} — stream the downloaded .wav.</li>
 *   <li>{@code GET  /api/calls/{id}/transcript} — RM/KH-labeled turns.</li>
 * </ul>
 * Errors are surfaced at the boundary via {@link ApiExceptionHandler}; input is
 * validated with {@code @Valid}.
 */
@RestController
@RequestMapping("/api/calls")
public class CallController {

    private final CallService callService;
    private final RecordingService recordingService;
    private final TranscriptTurnRepository turns;

    public CallController(CallService callService,
                          RecordingService recordingService,
                          TranscriptTurnRepository turns) {
        this.callService = callService;
        this.recordingService = recordingService;
        this.turns = turns;
    }

    @PostMapping("/initiate")
    public ResponseEntity<CallResponse> initiate(@Valid @RequestBody InitiateCallRequest req) {
        CallRecord cr = callService.initiate(req.phoneNumber(), req.tnbCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(CallResponse.from(cr));
    }

    /**
     * Tạo cuộc gọi từ lead (C1, FR1.5). 201 kèm Call; 400 số không hợp lệ; 403
     * lead khác RM; 409 đang có cuộc gọi chưa xử lý xong.
     */
    @PostMapping
    public ResponseEntity<CallDto> create(@Valid @RequestBody CreateCallRequest req) {
        CallRecord cr = callService.createFromLead(req.leadId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CallDto.from(cr));
    }

    /**
     * Kết thúc cuộc gọi, idempotent (C1, AC1.1.3). Kiểm quyền (403). Giữ đường
     * dẫn cũ, nhưng nay trả Call (vẫn chứa sttStatus để tương thích).
     */
    @PostMapping("/{id}/ended")
    public CallDto ended(@PathVariable Long id) {
        return CallDto.from(callService.endCall(id));
    }

    @GetMapping("/{id}")
    public CallDto get(@PathVariable Long id) {
        return CallDto.from(callService.getOwnedCall(id));
    }

    @GetMapping("/{id}/transcript")
    public List<TranscriptTurnResponse> transcript(@PathVariable Long id) {
        // 404 if the call itself is unknown; an empty list means "not ready yet".
        callService.find(id).orElseThrow(() -> notFound(id));
        return turns.findByCallIdOrderByStartMsAsc(id).stream()
                .map(TranscriptTurnResponse::from)
                .toList();
    }

    /**
     * Stream the single stereo recording {@code .wav} as audio/wav (both call
     * legs on separate channels). {@link RecordingService} retries with bounded
     * backoff and throws when nothing is ready, which the handler maps to 404 so
     * the frontend can keep polling (FR3.2).
     */
    @GetMapping("/{id}/recording")
    public ResponseEntity<byte[]> recording(@PathVariable Long id) {
        callService.find(id).orElseThrow(() -> notFound(id));
        byte[] wav = recordingService.downloadRecording(id);
        if (wav == null || wav.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording not ready for call " + id);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"call-" + id + ".wav\"")
                .contentType(MediaType.parseMediaType("audio/wav"))
                .body(wav);
    }

    private ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No call record for id " + id);
    }
}
