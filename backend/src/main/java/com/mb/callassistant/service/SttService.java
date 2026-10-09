package com.mb.callassistant.service;

import com.mb.callassistant.config.AppProperties;
import com.mb.callassistant.domain.Speaker;
import com.mb.callassistant.domain.TranscriptTurn;
import com.mb.callassistant.repository.TranscriptTurnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Produces a Vietnamese transcript with RM/KH speaker labels (FR4) from the
 * single stereo recording {@code .wav}, and persists the resulting
 * {@link TranscriptTurn}s.
 *
 * <p><b>Stereo split (FR4.2):</b> the one stereo file carries the two call legs
 * on separate channels. {@link WavStereoSplitter} de-interleaves it into a left
 * and a right mono stream; each is transcribed independently and labeled with a
 * speaker according to {@code app.stt.rm-channel} ({@code left}|{@code right}),
 * then the two channels' turns are merged in start-time order. The side→speaker
 * mapping is configurable so it can be flipped without a code change.
 *
 * <p><b>Fallback:</b> when the file is not 16-bit PCM stereo (so it cannot be
 * split), it is transcribed as a single stream and all turns are labeled RM — a
 * documented limitation for the demo.
 */
@Service
public class SttService {

    private static final Logger log = LoggerFactory.getLogger(SttService.class);
    private static final String LANGUAGE_VI = "vi";

    private final SttClient sttClient;
    private final TranscriptTurnRepository turns;
    private final AppProperties props;

    public SttService(SttClient sttClient, TranscriptTurnRepository turns, AppProperties props) {
        this.sttClient = sttClient;
        this.turns = turns;
        this.props = props;
    }

    /**
     * Split the stereo recording, transcribe each channel, build RM/KH-labeled
     * turns, and persist them (replacing any prior turns for the call so a re-run
     * is idempotent).
     *
     * @param callId   the call being transcribed
     * @param stereoWav the single stereo {@code .wav} bytes for the call
     * @return the persisted turns in time order
     */
    @Transactional
    public List<TranscriptTurn> transcribe(Long callId, byte[] stereoWav) {
        if (stereoWav == null || stereoWav.length == 0) {
            throw new SttException("No recording audio to transcribe for call " + callId);
        }

        List<TranscriptTurn> built = new ArrayList<>();
        WavStereoSplitter.Split split = WavStereoSplitter.split(stereoWav);

        if (split.isStereo()) {
            Speaker leftSpeaker = rmIsLeft() ? Speaker.RM : Speaker.KH;
            Speaker rightSpeaker = rmIsLeft() ? Speaker.KH : Speaker.RM;
            transcribeChannel(callId, split.left(), leftSpeaker, built);
            transcribeChannel(callId, split.right(), rightSpeaker, built);
            log.info("Transcribed stereo recording for call {} (RM channel = {}).",
                    callId, rmIsLeft() ? "left" : "right");
        } else {
            // Not splittable (not 16-bit PCM stereo) — transcribe as one stream, label RM.
            List<SttSegment> segments = sttClient.transcribe(split.single(), LANGUAGE_VI, false);
            for (SttSegment s : segments) {
                built.add(new TranscriptTurn(callId, Speaker.RM, s.startMs(), s.endMs(), s.textVi()));
            }
            log.warn("Recording for call {} was not 16-bit PCM stereo; transcribed as a single stream "
                    + "and labeled RM (speaker separation unavailable for this file).", callId);
        }

        built.sort(Comparator.comparingLong(TranscriptTurn::getStartMs));

        // Idempotent persist: clear prior turns, then save the fresh set.
        turns.deleteByCallId(callId);
        List<TranscriptTurn> saved = turns.saveAll(built);
        log.info("Persisted {} transcript turns for call {} (mode={}).",
                saved.size(), callId, split.isStereo() ? "stereo-split" : "single-stream");
        return saved;
    }

    private void transcribeChannel(Long callId, byte[] monoWav, Speaker speaker, List<TranscriptTurn> out) {
        List<SttSegment> segments = sttClient.transcribe(monoWav, LANGUAGE_VI, false);
        for (SttSegment s : segments) {
            out.add(new TranscriptTurn(callId, speaker, s.startMs(), s.endMs(), s.textVi()));
        }
    }

    /** True when {@code app.stt.rm-channel} is the left channel (the default). */
    private boolean rmIsLeft() {
        String ch = props.getStt().getRmChannel();
        // Anything other than an explicit "right" means left (safe default).
        return ch == null || !ch.trim().equalsIgnoreCase("right");
    }
}
