package com.mb.callassistant.lead;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.regex.Pattern;

/**
 * Khách hàng cá nhân (KHCN) gắn với lead. Dữ liệu demo (seed). {@code phoneValid}
 * là trường dẫn xuất (không lưu cột) kiểm số điện thoại VN hợp lệ trước khi tạo
 * cuộc gọi (FR1.5, BR3.1).
 */
@Entity
@Table(name = "customer")
public class Customer {

    /**
     * Số VN hợp lệ: tùy chọn +84 hoặc 0 đầu, rồi 9 chữ số, cho phép khoảng trắng
     * phân tách khi nhập; chuẩn hóa trước khi so khớp.
     */
    private static final Pattern VN_PHONE = Pattern.compile("^(?:\\+84|0)\\d{9}$");

    @Id
    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "segment")
    private String segment;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "age")
    private Integer age;

    @Column(name = "occupation")
    private String occupation;

    /** Thu nhập ước tính: số nguyên VND (BR6.2). */
    @Column(name = "estimated_income")
    private Long estimatedIncome;

    @Column(name = "address")
    private String address;

    protected Customer() {
        // JPA
    }

    public Customer(String customerId, String fullName, String phoneNumber) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    /** Số điện thoại hợp lệ để gọi được (BR3.1). Dẫn xuất, không lưu. */
    @Transient
    public boolean isPhoneValid() {
        return isValidPhone(phoneNumber);
    }

    /** Kiểm một chuỗi số điện thoại VN; chuẩn hóa khoảng trắng/dấu gạch trước. */
    public static boolean isValidPhone(String raw) {
        if (raw == null) {
            return false;
        }
        String normalized = raw.replaceAll("[\\s.\\-]", "");
        return VN_PHONE.matcher(normalized).matches();
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public Long getEstimatedIncome() {
        return estimatedIncome;
    }

    public void setEstimatedIncome(Long estimatedIncome) {
        this.estimatedIncome = estimatedIncome;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
