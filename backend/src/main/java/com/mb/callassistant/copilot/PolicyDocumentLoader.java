package com.mb.callassistant.copilot;

import com.mb.callassistant.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Nạp và lập chỉ mục tài liệu chính sách khi khởi động (WF1, BR1.1/BR1.2/BR1.3).
 *
 * <p>Đọc {@code app.copilot.docs-dir}; với mỗi file {@code .md}/{@code .txt}: đọc
 * khối metadata (header {@code --- ... ---} với {@code title} + {@code effectiveDate}
 * bắt buộc, {@code sourcePdf} tuỳ chọn); thiếu/không đọc được/rỗng ⇒ ghi log tên
 * file + bỏ qua, không chặn file khác. File hợp lệ ⇒ tạo {@link PolicyDocument},
 * cắt {@link DocumentChunk} theo dấu {@code [[page:n]]} / {@code ## Mục}, sinh
 * {@code normalizedText} (BR1.3). Chỉ mục rỗng ⇒ mọi câu hỏi trả "không tìm thấy".
 *
 * <p>Múi giờ {@code +07:00} cho {@code loadedAt} (hợp đồng chung).
 */
@Component
public class PolicyDocumentLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicyDocumentLoader.class);
    private static final ZoneOffset VN_OFFSET = ZoneOffset.ofHours(7);

    /** Dấu phân trang trên một dòng riêng: {@code [[page:3]]}. */
    private static final Pattern PAGE_MARKER = Pattern.compile("^\\s*\\[\\[page:(\\d+)]]\\s*$");
    /** Dấu mục kiểu Markdown trên một dòng riêng: {@code ## Mục 2.1}. */
    private static final Pattern SECTION_MARKER = Pattern.compile("^\\s*#{1,6}\\s+(.*\\S)\\s*$");
    /** Dòng ngăn khối metadata YAML tối giản. */
    private static final Pattern FRONT_MATTER_FENCE = Pattern.compile("^\\s*---\\s*$");

    private final AppProperties props;
    private final PolicyDocumentRepository repository;

    public PolicyDocumentLoader(AppProperties props, PolicyDocumentRepository repository) {
        this.props = props;
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        loadAll();
    }

    /**
     * Nạp mọi file text/md trong thư mục cấu hình. Trả về số tài liệu nạp thành
     * công (hữu ích cho kiểm thử); số file bị bỏ qua được ghi log.
     */
    public int loadAll() {
        String dir = props.getCopilot().getDocsDir();
        Path docsDir = Paths.get(dir);
        if (!Files.isDirectory(docsDir)) {
            log.warn("Copilot: thư mục tài liệu không tồn tại hoặc không phải thư mục: {} — chỉ mục rỗng", dir);
            return 0;
        }

        int loaded = 0;
        int skipped = 0;
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(docsDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(PolicyDocumentLoader::isTextOrMarkdown)
                    .sorted()
                    .forEach(files::add);
        } catch (IOException e) {
            log.warn("Copilot: không liệt kê được thư mục tài liệu {} ({}) — chỉ mục rỗng", dir, e.getMessage());
            return 0;
        }

        for (Path file : files) {
            if (loadOne(file)) {
                loaded++;
            } else {
                skipped++;
            }
        }
        log.info("Copilot: nạp xong tài liệu chính sách — thành công={}, bỏ qua={}, tổng đoạn={}",
                loaded, skipped, repository.allChunks().size());
        return loaded;
    }

    private static boolean isTextOrMarkdown(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".md") || name.endsWith(".txt");
    }

    /** @return {@code true} nếu file hợp lệ và đã nạp; {@code false} nếu bị bỏ qua. */
    private boolean loadOne(Path file) {
        String fileName = file.getFileName().toString();
        String content;
        try {
            content = Files.readString(file);
        } catch (IOException e) {
            log.warn("Copilot: bỏ qua file không đọc được: {} ({})", fileName, e.getMessage());
            return false;
        }
        if (content == null || content.isBlank()) {
            log.warn("Copilot: bỏ qua file rỗng/không có nội dung chữ: {}", fileName);
            return false;
        }

        Metadata meta = parseMetadata(content);
        if (meta == null || meta.title == null || meta.title.isBlank() || meta.effectiveDate == null) {
            log.warn("Copilot: bỏ qua file thiếu metadata title/effectiveDate: {}", fileName);
            return false;
        }

        String body = meta.body;
        List<ChunkDraft> drafts = splitChunks(body);
        if (drafts.isEmpty()) {
            log.warn("Copilot: bỏ qua file không có nội dung chữ sau metadata: {}", fileName);
            return false;
        }

        String documentId = deriveDocumentId(file);
        Integer pageCount = countPages(drafts);
        PolicyDocument document = new PolicyDocument(
                documentId, meta.title, meta.effectiveDate, meta.sourcePdf,
                file.toString(), pageCount, OffsetDateTime.now(VN_OFFSET));

        List<DocumentChunk> chunks = new ArrayList<>();
        int index = 0;
        for (ChunkDraft draft : drafts) {
            chunks.add(new DocumentChunk(documentId, index++, draft.page, draft.section,
                    draft.text, TextNormalizer.normalize(draft.text)));
        }
        repository.add(document, chunks);
        return true;
    }

    /** Id tài liệu ổn định suy từ tên file (không phần mở rộng). */
    private static String deriveDocumentId(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static Integer countPages(List<ChunkDraft> drafts) {
        long pages = drafts.stream().filter(d -> d.page != null).map(d -> d.page).distinct().count();
        return pages == 0 ? null : (int) pages;
    }

    /** Đọc khối metadata {@code --- ... ---} ở đầu file; trả về cả phần thân còn lại. */
    private static Metadata parseMetadata(String content) {
        String[] lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        Metadata meta = new Metadata();
        int bodyStart = 0;

        if (lines.length > 0 && FRONT_MATTER_FENCE.matcher(lines[0]).matches()) {
            int i = 1;
            boolean closed = false;
            for (; i < lines.length; i++) {
                if (FRONT_MATTER_FENCE.matcher(lines[i]).matches()) {
                    closed = true;
                    i++;
                    break;
                }
                parseMetaLine(lines[i], meta);
            }
            if (closed) {
                bodyStart = i;
            } else {
                // Không có fence đóng ⇒ coi như không có metadata hợp lệ.
                meta.title = null;
                meta.effectiveDate = null;
                bodyStart = 0;
            }
        } else {
            return null; // Không có khối metadata ⇒ bỏ qua (BR1.1).
        }

        StringBuilder body = new StringBuilder();
        for (int i = bodyStart; i < lines.length; i++) {
            body.append(lines[i]).append('\n');
        }
        meta.body = body.toString();
        return meta;
    }

    private static void parseMetaLine(String line, Metadata meta) {
        int colon = line.indexOf(':');
        if (colon <= 0) {
            return;
        }
        String key = line.substring(0, colon).trim().toLowerCase();
        String value = line.substring(colon + 1).trim();
        switch (key) {
            case "title" -> meta.title = value.isBlank() ? null : value;
            case "effectivedate" -> meta.effectiveDate = parseDate(value);
            case "sourcepdf" -> meta.sourcePdf = value.isBlank() ? null : value;
            default -> {
                // Bỏ qua khoá không hiểu — quy ước metadata tối giản.
            }
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Cắt thân tài liệu thành các đoạn theo dấu {@code [[page:n]]} hoặc tiêu đề
     * {@code ## ...}. Mỗi đoạn giữ page (ưu tiên) hoặc section để trích dẫn. Khi
     * tài liệu không có dấu nào, cả thân là một đoạn với {@code section} mặc định.
     */
    private static List<ChunkDraft> splitChunks(String body) {
        String[] lines = body.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        List<ChunkDraft> drafts = new ArrayList<>();

        Integer currentPage = null;
        String currentSection = null;
        boolean sawAnyMarker = false;
        StringBuilder buffer = new StringBuilder();

        for (String line : lines) {
            Matcher pageM = PAGE_MARKER.matcher(line);
            Matcher sectionM = SECTION_MARKER.matcher(line);
            if (pageM.matches()) {
                flush(drafts, currentPage, currentSection, buffer);
                buffer.setLength(0);
                currentPage = Integer.valueOf(pageM.group(1));
                sawAnyMarker = true;
            } else if (sectionM.matches()) {
                flush(drafts, currentPage, currentSection, buffer);
                buffer.setLength(0);
                currentSection = sectionM.group(1).trim();
                sawAnyMarker = true;
            } else {
                buffer.append(line).append('\n');
            }
        }
        flush(drafts, currentPage, currentSection, buffer);

        if (drafts.isEmpty() && !sawAnyMarker) {
            String text = body.strip();
            if (!text.isBlank()) {
                drafts.add(new ChunkDraft(1, null, text));
            }
        }
        return drafts;
    }

    private static void flush(List<ChunkDraft> drafts, Integer page, String section, StringBuilder buffer) {
        String text = buffer.toString().strip();
        if (text.isBlank()) {
            return;
        }
        Integer page0 = page;
        String section0 = section;
        if (page0 == null && (section0 == null || section0.isBlank())) {
            // Nội dung trước dấu đầu tiên: gán page 1 để luôn trích dẫn được (BR1.3).
            page0 = 1;
        }
        drafts.add(new ChunkDraft(page0, section0, text));
    }

    private static final class Metadata {
        private String title;
        private LocalDate effectiveDate;
        private String sourcePdf;
        private String body = "";
    }

    private record ChunkDraft(Integer page, String section, String text) {
    }
}
