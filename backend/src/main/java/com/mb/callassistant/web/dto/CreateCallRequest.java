package com.mb.callassistant.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body cho {@code POST /api/calls} (C1): tạo cuộc gọi từ một lead (FR1.5). */
public record CreateCallRequest(

        @NotBlank(message = "leadId là bắt buộc")
        @Size(max = 64, message = "leadId quá dài")
        String leadId
) {
}
