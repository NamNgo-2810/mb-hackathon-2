package com.mb.callassistant.web.dto;

import com.mb.callassistant.lead.RmUser;

/** DTO RM hiện tại (C1 /api/me). Không bao giờ chứa passwordHash (BR6.1). */
public record Rm(String rmId, String displayName) {

    public static Rm from(RmUser rm) {
        return new Rm(rm.getRmId(), rm.getDisplayName());
    }
}
