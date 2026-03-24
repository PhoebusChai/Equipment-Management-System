package com.ems.modules.notice.dto;

import jakarta.validation.constraints.NotNull;

public record NoticeAuditRequest(
        @NotNull Long operatorId
) {
}

