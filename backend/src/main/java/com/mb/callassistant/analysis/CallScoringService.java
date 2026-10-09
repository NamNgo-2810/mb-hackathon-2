package com.mb.callassistant.analysis;

import com.mb.callassistant.config.AppProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Chấm điểm & kiểm kết quả AI (lớp nghiệp vụ thuần, U4).
 *
 * <ul>
 *   <li>Kiểm {@code criteriaScores} đúng 5 phần tử, mỗi điểm ∈ [0,10] bước 0,5;
 *       sai → ném {@link AnalysisInvalidException} (coi là AI thất bại), KHÔNG
 *       bịa điểm (BR2.1/BR2.2).</li>
 *   <li>Tính {@code totalScore = round(tổng 5 tiêu chí × 2)} ∈ [0,100]; BỎ QUA
 *       mọi tổng do LLM đưa (BR3.1/BR3.2).</li>
 *   <li>Suy {@code grade} theo ngưỡng cấu hình A≥85/B70–84/C50–69/D&lt;50; biên
 *       85→A, 84→B (BR3.3, AC2.2.4).</li>
 * </ul>
 */
@Service
public class CallScoringService {

    private static final BigDecimal STEP = new BigDecimal("0.5");
    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = BigDecimal.TEN;
    private static final int REQUIRED_CRITERIA = 5;

    private final AppProperties props;

    public CallScoringService(AppProperties props) {
        this.props = props;
    }

    /**
     * Kiểm {@code criteriaScores} và tính điểm tổng (BR2.2/BR3.1).
     *
     * @throws AnalysisInvalidException khi số tiêu chí ≠ 5 hoặc một điểm ngoài
     *                                  [0,10] / không chia hết 0,5
     */
    public int totalScore(List<CriterionScore> criteriaScores) {
        validateCriteria(criteriaScores);
        BigDecimal sum = BigDecimal.ZERO;
        for (CriterionScore cs : criteriaScores) {
            sum = sum.add(cs.score());
        }
        // round(tổng × 2); tổng ∈ [0,50] nên kết quả ∈ [0,100].
        BigDecimal total = sum.multiply(new BigDecimal("2"))
                .setScale(0, RoundingMode.HALF_UP);
        int value = total.intValueExact();
        return Math.max(0, Math.min(100, value));
    }

    /** Suy hạng từ điểm tổng theo ngưỡng cấu hình (BR3.3). */
    public AnalysisGrade grade(int totalScore) {
        AppProperties.Analysis cfg = props.getAnalysis();
        if (totalScore >= cfg.getGradeThresholdA()) {
            return AnalysisGrade.A;
        }
        if (totalScore >= cfg.getGradeThresholdB()) {
            return AnalysisGrade.B;
        }
        if (totalScore >= cfg.getGradeThresholdC()) {
            return AnalysisGrade.C;
        }
        return AnalysisGrade.D;
    }

    /** Kiểm bất biến của {@code criteriaScores} (BR2.1/BR2.2); không sửa/bịa điểm. */
    public void validateCriteria(List<CriterionScore> criteriaScores) {
        if (criteriaScores == null || criteriaScores.size() != REQUIRED_CRITERIA) {
            throw new AnalysisInvalidException(
                    "criteriaScores phải có đúng " + REQUIRED_CRITERIA + " phần tử");
        }
        for (CriterionScore cs : criteriaScores) {
            if (cs == null || cs.code() == null || cs.score() == null) {
                throw new AnalysisInvalidException("Thiếu trường bắt buộc trong một tiêu chí chấm điểm");
            }
            BigDecimal score = cs.score();
            if (score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
                throw new AnalysisInvalidException("Điểm tiêu chí ngoài khoảng [0,10]");
            }
            // Chia hết cho 0,5: score / 0,5 phải là số nguyên.
            BigDecimal quotient = score.divide(STEP, 0, RoundingMode.DOWN);
            if (quotient.multiply(STEP).compareTo(score) != 0) {
                throw new AnalysisInvalidException("Điểm tiêu chí không chia hết cho 0,5");
            }
        }
    }
}
