package com.mb.callassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Splits a single stereo {@code .wav} into its two mono channels using plain
 * Java — no external binary, no extra dependency (FR4.2). The customer and the
 * RM are each on a separate stereo channel (confirmed by ear: one leg = left,
 * the other = right), so de-interleaving the PCM frames recovers the two legs.
 *
 * <p>Only the common demo case is handled directly: an uncompressed
 * <b>16-bit PCM stereo</b> WAV ({@code audioFormat=1}, {@code numChannels=2},
 * {@code bitsPerSample=16}). For anything else — mono, non-PCM, 8/24/32-bit, or
 * an unparseable header — {@link #split(byte[])} returns a {@link Split} marked
 * {@link Split#isStereo()} {@code false} carrying the original bytes, and the
 * caller transcribes it as a single stream (documented limitation).
 *
 * <p>WAV is little-endian. We locate the {@code fmt } and {@code data} chunks by
 * walking the RIFF chunk list rather than assuming a fixed 44-byte header.
 */
final class WavStereoSplitter {

    private static final Logger log = LoggerFactory.getLogger(WavStereoSplitter.class);

    private WavStereoSplitter() {
    }

    /** Result of a split: either two mono WAVs, or the original single stream. */
    record Split(boolean isStereo, byte[] left, byte[] right, byte[] single) {

        static Split singleStream(byte[] original) {
            return new Split(false, null, null, original);
        }

        static Split stereo(byte[] left, byte[] right) {
            return new Split(true, left, right, null);
        }
    }

    /**
     * Split a stereo WAV into two mono WAVs. Returns a single-stream result when
     * the input is not 16-bit PCM stereo (the caller then treats it as one leg).
     */
    static Split split(byte[] wav) {
        if (wav == null || wav.length < 12) {
            return Split.singleStream(wav);
        }
        if (!isRiffWave(wav)) {
            log.info("Recording is not a RIFF/WAVE file; treating as a single stream.");
            return Split.singleStream(wav);
        }

        Integer fmtOffset = findChunk(wav, "fmt ");
        Integer dataOffset = findChunk(wav, "data");
        if (fmtOffset == null || dataOffset == null) {
            log.info("WAV missing fmt/data chunk; treating as a single stream.");
            return Split.singleStream(wav);
        }

        int audioFormat = readU16(wav, fmtOffset + 8);
        int numChannels = readU16(wav, fmtOffset + 10);
        int sampleRate = readU32(wav, fmtOffset + 12);
        int bitsPerSample = readU16(wav, fmtOffset + 22);

        if (audioFormat != 1 || numChannels != 2 || bitsPerSample != 16) {
            log.info("WAV is not 16-bit PCM stereo (format={}, channels={}, bits={}); "
                    + "treating as a single stream (limitation).", audioFormat, numChannels, bitsPerSample);
            return Split.singleStream(wav);
        }

        int dataLen = readU32(wav, dataOffset + 4);
        int dataStart = dataOffset + 8;
        // Clamp to the actual array in case the header overstates the data length.
        int dataEnd = Math.min(dataStart + dataLen, wav.length);
        int usable = Math.max(0, dataEnd - dataStart);

        int bytesPerSample = 2;              // 16-bit
        int frameBytes = bytesPerSample * 2; // stereo frame = L + R
        int frames = usable / frameBytes;

        byte[] leftPcm = new byte[frames * bytesPerSample];
        byte[] rightPcm = new byte[frames * bytesPerSample];

        for (int f = 0; f < frames; f++) {
            int src = dataStart + f * frameBytes;
            int dst = f * bytesPerSample;
            // Left sample
            leftPcm[dst] = wav[src];
            leftPcm[dst + 1] = wav[src + 1];
            // Right sample
            rightPcm[dst] = wav[src + 2];
            rightPcm[dst + 1] = wav[src + 3];
        }

        byte[] leftWav = buildMonoWav(leftPcm, sampleRate, bitsPerSample);
        byte[] rightWav = buildMonoWav(rightPcm, sampleRate, bitsPerSample);
        log.info("Split stereo WAV into two mono channels ({} frames @ {} Hz).", frames, sampleRate);
        return Split.stereo(leftWav, rightWav);
    }

    private static boolean isRiffWave(byte[] wav) {
        return matches(wav, 0, "RIFF") && matches(wav, 8, "WAVE");
    }

    /**
     * Walk the chunk list after the 12-byte RIFF header, returning the offset of
     * the named chunk's 4-byte id (so {@code id+4}=size, {@code id+8}=body), or
     * null if not found. Chunk bodies are word-aligned (padded to even length).
     */
    private static Integer findChunk(byte[] wav, String chunkId) {
        int pos = 12;
        while (pos + 8 <= wav.length) {
            int size = readU32(wav, pos + 4);
            if (matches(wav, pos, chunkId)) {
                return pos;
            }
            int advance = 8 + size + (size % 2); // pad to even
            if (advance <= 0) {
                break; // malformed; avoid an infinite loop
            }
            pos += advance;
        }
        return null;
    }

    /** Wrap raw mono PCM with a canonical 44-byte WAV header. */
    private static byte[] buildMonoWav(byte[] pcm, int sampleRate, int bitsPerSample) {
        int channels = 1;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataLen = pcm.length;
        int riffLen = 36 + dataLen;

        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + dataLen);
        writeAscii(out, "RIFF");
        writeU32(out, riffLen);
        writeAscii(out, "WAVE");
        writeAscii(out, "fmt ");
        writeU32(out, 16);               // PCM fmt chunk size
        writeU16(out, 1);                // audioFormat = PCM
        writeU16(out, channels);
        writeU32(out, sampleRate);
        writeU32(out, byteRate);
        writeU16(out, blockAlign);
        writeU16(out, bitsPerSample);
        writeAscii(out, "data");
        writeU32(out, dataLen);
        out.write(pcm, 0, pcm.length);
        return out.toByteArray();
    }

    // --- little-endian readers ---

    private static boolean matches(byte[] wav, int offset, String ascii) {
        if (offset + ascii.length() > wav.length) {
            return false;
        }
        byte[] expected = ascii.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        return Arrays.equals(Arrays.copyOfRange(wav, offset, offset + expected.length), expected);
    }

    private static int readU16(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    private static int readU32(byte[] b, int off) {
        return (b[off] & 0xFF)
                | ((b[off + 1] & 0xFF) << 8)
                | ((b[off + 2] & 0xFF) << 16)
                | ((b[off + 3] & 0xFF) << 24);
    }

    // --- little-endian writers ---

    private static void writeAscii(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private static void writeU16(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
    }

    private static void writeU32(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
        out.write((v >> 16) & 0xFF);
        out.write((v >> 24) & 0xFF);
    }
}
