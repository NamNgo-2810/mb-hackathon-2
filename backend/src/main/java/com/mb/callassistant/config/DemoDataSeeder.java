package com.mb.callassistant.config;

import com.mb.callassistant.lead.Activity;
import com.mb.callassistant.lead.ActivityRepository;
import com.mb.callassistant.lead.ActivityType;
import com.mb.callassistant.lead.Customer;
import com.mb.callassistant.lead.CustomerRepository;
import com.mb.callassistant.lead.DemoCurrentRmProvider;
import com.mb.callassistant.lead.Lead;
import com.mb.callassistant.lead.LeadPriority;
import com.mb.callassistant.lead.LeadRepository;
import com.mb.callassistant.lead.LeadStage;
import com.mb.callassistant.lead.RmUser;
import com.mb.callassistant.lead.RmUserRepository;
import com.mb.callassistant.task.Task;
import com.mb.callassistant.task.TaskOrigin;
import com.mb.callassistant.task.TaskPriority;
import com.mb.callassistant.task.TaskRepository;
import com.mb.callassistant.task.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Seed dữ liệu demo (profile {@code demo}, ADR-007/A2): một RM mẫu, một khách
 * hàng + lead (khớp nội dung mockup S1) và vài hoạt động, để
 * {@code GET /api/leads/demo} mở thẳng S1 mà không cần U6. Idempotent: không
 * seed lại nếu đã có RM.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final ZoneOffset VN = ZoneOffset.of("+07:00");

    private final RmUserRepository rmUsers;
    private final CustomerRepository customers;
    private final LeadRepository leads;
    private final ActivityRepository activities;
    private final TaskRepository tasks;

    public DemoDataSeeder(RmUserRepository rmUsers,
                          CustomerRepository customers,
                          LeadRepository leads,
                          ActivityRepository activities,
                          TaskRepository tasks) {
        this.rmUsers = rmUsers;
        this.customers = customers;
        this.leads = leads;
        this.activities = activities;
        this.tasks = tasks;
    }

    @Override
    public void run(String... args) {
        if (rmUsers.existsById(DemoCurrentRmProvider.DEMO_RM_ID)) {
            return;
        }

        RmUser rm = new RmUser(
                DemoCurrentRmProvider.DEMO_RM_ID,
                DemoCurrentRmProvider.DEMO_RM_USERNAME,
                DemoCurrentRmProvider.DEMO_RM_DISPLAY_NAME);
        rmUsers.save(rm);

        Customer customer = new Customer("cust-demo-001", "TRAN THI PHUONG HIEN", "0964040001");
        customer.setSegment("INDIV-MASS POTENTIAL");
        customer.setAge(34);
        customer.setOccupation("Nhân viên văn phòng");
        customer.setEstimatedIncome(40_000_000L); // VND, số nguyên (BR6.2)
        customer.setAddress("Hai Bà Trưng, Hà Nội");
        customers.save(customer);

        Lead lead = new Lead("lead-demo-001", "L120825003485", customer.getCustomerId(), rm.getRmId());
        lead.setProductInterest("Tiền gửi tiết kiệm dài hạn");
        lead.setSource("Trigger");
        lead.setPriority(LeadPriority.HOT_LEAD);
        lead.setRankScore(9);
        lead.setSlaDueAt(OffsetDateTime.now(VN).plusDays(2));
        lead.setStage(LeadStage.DANG_TIEP_CAN);
        lead.setContent("Loại Trigger: NHAN_TIEN; Giá trị: 500 tr.đ; "
                + "Nội dung: Thanh toán tiền chuyển nhượng. Số lượng sản phẩm: 3; "
                + "CASA BQ 3 tháng: 8 tr.đ; Tiết kiệm BQ 3 tháng: 176 tr.đ; "
                + "Dư nợ tại MB: 0 tr.đ.");
        lead.setSalesScript("Chào anh/chị, em là chuyên viên chăm sóc khách hàng VIP tại MB. "
                + "Nhân dịp tri ân, MB gửi tặng ưu đãi cộng thêm lãi suất tiết kiệm tới 1%/năm.");
        leads.save(lead);

        Activity a1 = new Activity(UUID.randomUUID().toString(), lead.getLeadId(),
                ActivityType.HEN_GAP, OffsetDateTime.now(VN).minusDays(1));
        a1.setProduct("Cho vay thẻ tín dụng quốc tế VISA");
        a1.setResult("KH không có nhu cầu mua SPDV");
        activities.save(a1);

        Activity a2 = new Activity(UUID.randomUUID().toString(), lead.getLeadId(),
                ActivityType.SMS, OffsetDateTime.now(VN).minusDays(3));
        a2.setProduct("Cho vay thẻ tín dụng quốc tế VISA");
        activities.save(a2);

        // Công việc demo cho tuần hiện tại (U5, A2/BR3.2/BR4.3). Idempotent cùng
        // guard existsById(RM) ở đầu phương thức.
        OffsetDateTime base = OffsetDateTime.now(VN).withHour(9).withMinute(0)
                .withSecond(0).withNano(0);

        Task t1 = new Task(UUID.randomUUID().toString(), rm.getRmId(),
                "Chốt deal mở sổ tiết kiệm dài hạn", TaskType.LICH_HEN,
                base.plusDays(1), TaskOrigin.SEED, OffsetDateTime.now(VN));
        t1.setLeadId(lead.getLeadId());
        t1.setPriority(TaskPriority.CAO);
        t1.setDurationMinutes(60);
        tasks.save(t1);

        Task t2 = new Task(UUID.randomUUID().toString(), rm.getRmId(),
                "Nhắc gọi xác nhận hồ sơ KH Hiền", TaskType.NHAC_GOI,
                base.plusDays(2).withHour(14), TaskOrigin.SEED, OffsetDateTime.now(VN));
        t2.setLeadId(lead.getLeadId());
        t2.setPriority(TaskPriority.THUONG);
        tasks.save(t2);

        // Một việc đã xong (DONE) để minh hoạ thẻ gạch ngang (BR4.4).
        Task t3 = new Task(UUID.randomUUID().toString(), rm.getRmId(),
                "Gửi bảng tính lãi qua Zalo", TaskType.TASK_ZALO,
                base.minusDays(1).withHour(10), TaskOrigin.SEED, OffsetDateTime.now(VN));
        t3.setLeadId(lead.getLeadId());
        t3.markDone(OffsetDateTime.now(VN).minusHours(2));
        tasks.save(t3);

        // Ít nhất một việc KHAC không gắn lead (origin SEED, leadId=null) để minh
        // hoạ bộ lọc "Tất cả" (A2, BR3.2/BR4.3).
        Task t4 = new Task(UUID.randomUUID().toString(), rm.getRmId(),
                "Họp giao ban đầu tuần KHCN", TaskType.KHAC,
                base.plusDays(1).withHour(15).withMinute(30), TaskOrigin.SEED, OffsetDateTime.now(VN));
        t4.setDurationMinutes(60);
        tasks.save(t4);

        log.info("Seeded demo RM {}, 1 customer, 1 lead ({}), 2 activities, 4 tasks.",
                rm.getRmId(), lead.getLeadCode());
    }
}
