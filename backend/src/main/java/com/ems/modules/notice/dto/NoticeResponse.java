package com.ems.modules.notice.dto;

import java.time.LocalDateTime;

public record NoticeResponse(
        Long id,
        String noticeType,
        String targetType,
        Long targetId,
        String title,
        String content,
        Long publisherId,
        LocalDateTime publishTime,
        String status
) {
}

