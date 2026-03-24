package com.ems.modules.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NoticeCreateRequest(
        @NotBlank String noticeType,
        @NotBlank String targetType,
        Long targetId,
        @NotBlank String title,
        @NotBlank String content,
        @NotNull Long publisherId
) {
}

