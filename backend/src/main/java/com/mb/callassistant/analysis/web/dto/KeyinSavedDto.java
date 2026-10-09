package com.mb.callassistant.analysis.web.dto;

import com.mb.callassistant.analysis.KeyinSaveResult;

import java.util.List;

/** DTO phản hồi lưu keyin thành công (C3, PUT /keyin → 200). Mở hộp thoại S4 (BR6.1). */
public record KeyinSavedDto(
        String callId,
        String interactionCode,
        List<String> createdTaskIds
) {

    public static KeyinSavedDto from(KeyinSaveResult r) {
        return new KeyinSavedDto(r.callId(), r.interactionCode(), r.createdTaskIds());
    }
}
