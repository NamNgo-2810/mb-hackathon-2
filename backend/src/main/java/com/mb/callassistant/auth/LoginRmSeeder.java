package com.mb.callassistant.auth;

import com.mb.callassistant.lead.Customer;
import com.mb.callassistant.lead.CustomerRepository;
import com.mb.callassistant.lead.Lead;
import com.mb.callassistant.lead.LeadPriority;
import com.mb.callassistant.lead.LeadRepository;
import com.mb.callassistant.lead.LeadStage;
import com.mb.callassistant.lead.RmUser;
import com.mb.callassistant.lead.RmUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Seed tài khoản RM mẫu (và vài lead minh hoạ) cho chế độ {@code login} (U6).
 * Chỉ chạy khi {@code app.auth.mode=login}; chế độ {@code demo} (mặc định) dùng
 * {@code DemoDataSeeder} của U1, không đổi (BR3.1). Idempotent theo
 * {@code username}: không seed lại nếu tài khoản đã tồn tại.
 *
 * <p><b>Bảo mật (BR5.1):</b> mật khẩu seed KHÔNG hard-code bí mật thật — lấy từ
 * biến môi trường {@code AUTH_DEMO_PASSWORD}, mặc định một giá trị rõ-ràng-là-demo
 * ({@value #DEFAULT_DEMO_PASSWORD}) được nêu trong README. Chỉ bản băm BCrypt
 * được lưu; mật khẩu thô không bao giờ log.
 */
@Component
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "login")
public class LoginRmSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LoginRmSeeder.class);
    private static final ZoneOffset VN = ZoneOffset.of("+07:00");

    /** Giá trị demo rõ ràng; ghi đè qua env {@code AUTH_DEMO_PASSWORD} (README). */
    static final String DEFAULT_DEMO_PASSWORD = "demo1234";

    private final RmUserRepository rmUsers;
    private final CustomerRepository customers;
    private final LeadRepository leads;
    private final PasswordHasher passwordHasher;

    public LoginRmSeeder(RmUserRepository rmUsers, CustomerRepository customers,
                         LeadRepository leads, PasswordHasher passwordHasher) {
        this.rmUsers = rmUsers;
        this.customers = customers;
        this.leads = leads;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void run(String... args) {
        String demoPassword = System.getenv().getOrDefault("AUTH_DEMO_PASSWORD", DEFAULT_DEMO_PASSWORD);

        RmUser rmA = ensureRm("rm-login-001", "rm.a", "RM An", demoPassword);
        RmUser rmB = ensureRm("rm-login-002", "rm.b", "RM Bình", demoPassword);

        // Vài lead minh hoạ cho danh sách lead ở chế độ login (U1 sở hữu bảng;
        // U6 chỉ bổ sung dữ liệu seed khi chạy login). Idempotent theo leadId.
        seedLeadsFor(rmA);
        seedLeadsFor(rmB);

        log.info("Login mode seeded sample RM accounts (rm.a, rm.b) and demo leads. "
                + "Demo password source: {} (never logged).",
                System.getenv("AUTH_DEMO_PASSWORD") != null ? "env AUTH_DEMO_PASSWORD" : "default demo value");
    }

    private RmUser ensureRm(String rmId, String username, String displayName, String rawPassword) {
        RmUser existing = rmUsers.findByUsername(username).orElse(null);
        if (existing != null) {
            if (existing.getPasswordHash() == null || existing.getPasswordHash().isBlank()) {
                existing.setPasswordHash(passwordHasher.hash(rawPassword));
                rmUsers.save(existing);
            }
            return existing;
        }
        RmUser rm = new RmUser(rmId, username, displayName);
        rm.setPasswordHash(passwordHasher.hash(rawPassword));
        rmUsers.save(rm);
        return rm;
    }

    private void seedLeadsFor(RmUser rm) {
        String custId = "cust-" + rm.getRmId();
        if (!customers.existsById(custId)) {
            Customer c = new Customer(custId, "KH " + rm.getDisplayName(), "0900000000");
            c.setSegment("INDIV-MASS POTENTIAL");
            customers.save(c);
        }

        String lead1 = "lead-" + rm.getRmId() + "-1";
        if (!leads.existsById(lead1)) {
            Lead l = new Lead(lead1, "L" + rm.getRmId().toUpperCase() + "001", custId, rm.getRmId());
            l.setProductInterest("Tiền gửi tiết kiệm");
            l.setSource("Trigger");
            l.setPriority(LeadPriority.KHAI_THAC_NGAY);
            l.setRankScore(9);
            l.setSlaDueAt(OffsetDateTime.now(VN).plusDays(1));
            l.setStage(LeadStage.CHUA_TIEP_CAN);
            leads.save(l);
        }

        String lead2 = "lead-" + rm.getRmId() + "-2";
        if (!leads.existsById(lead2)) {
            Lead l = new Lead(lead2, "L" + rm.getRmId().toUpperCase() + "002", custId, rm.getRmId());
            l.setProductInterest("Vay tiêu dùng");
            l.setSource("Hotline");
            l.setPriority(LeadPriority.HOT_LEAD);
            l.setRankScore(7);
            l.setSlaDueAt(OffsetDateTime.now(VN).plusDays(2));
            l.setStage(LeadStage.DANG_TIEP_CAN);
            leads.save(l);
        }
    }
}
