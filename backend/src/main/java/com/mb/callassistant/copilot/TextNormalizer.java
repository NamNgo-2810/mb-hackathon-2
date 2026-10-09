package com.mb.callassistant.copilot;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Chuẩn hoá văn bản tiếng Việt để tìm từ khoá không phụ thuộc dấu (BR2.1):
 * bỏ dấu (NFKD + loại ký tự ghép dấu + đổi đ/Đ → d), hạ thường, và tách từ.
 * Dùng chung cho {@code normalizedText} của đoạn và chuẩn hoá câu hỏi, nên
 * "lai suat" khớp "lãi suất".
 */
public final class TextNormalizer {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    /** Tách theo mọi ký tự không phải chữ/số (sau khi đã bỏ dấu). */
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{Alnum}]+");

    private TextNormalizer() {
    }

    /**
     * Bỏ dấu + hạ thường, giữ nguyên khoảng trắng cấu trúc.
     *
     * @param input văn bản gốc (có thể null)
     * @return bản đã chuẩn hoá; chuỗi rỗng khi {@code input} null/rỗng
     */
    public static String normalize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFKD);
        String noMarks = COMBINING_MARKS.matcher(decomposed).replaceAll("");
        // đ/Đ không mang dấu tổ hợp nên phải thay riêng.
        noMarks = noMarks.replace('đ', 'd').replace('Đ', 'D');
        return noMarks.toLowerCase();
    }

    /**
     * Tách câu hỏi/đoạn đã chuẩn hoá thành danh sách từ khoá (bỏ chuỗi rỗng,
     * khử trùng lặp giữ thứ tự).
     */
    public static List<String> tokens(String input) {
        String normalized = normalize(input);
        if (normalized.isBlank()) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String token : NON_WORD.split(normalized)) {
            if (!token.isBlank()) {
                unique.add(token);
            }
        }
        return new ArrayList<>(unique);
    }
}
