package com.ems.modules.notice.service;

import com.ems.modules.common.enums.NoticeStatus;
import com.ems.modules.notice.dto.NoticeAuditRequest;
import com.ems.modules.notice.dto.NoticeCreateRequest;
import com.ems.modules.notice.dto.NoticeResponse;
import com.ems.modules.notice.entity.NoticeEntity;
import com.ems.modules.notice.repository.NoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    public List<NoticeResponse> list() {
        return noticeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public NoticeResponse create(NoticeCreateRequest request) {
        NoticeEntity e = new NoticeEntity();
        e.setNoticeType(request.noticeType().toUpperCase());
        e.setTargetType(request.targetType().toUpperCase());
        e.setTargetId(request.targetId());
        e.setTitle(request.title());
        e.setContent(request.content());
        e.setPublisherId(request.publisherId());
        e.setPublishTime(LocalDateTime.now());
        e.setStatus(NoticeStatus.DRAFT.name());
        return toResponse(noticeRepository.save(e));
    }

    @Transactional
    public NoticeResponse publish(Long id, NoticeAuditRequest request) {
        NoticeEntity e = getById(id);
        e.setStatus(NoticeStatus.PUBLISHED.name());
        e.setPublishTime(LocalDateTime.now());
        return toResponse(noticeRepository.save(e));
    }

    @Transactional
    public NoticeResponse archive(Long id, NoticeAuditRequest request) {
        NoticeEntity e = getById(id);
        e.setStatus(NoticeStatus.ARCHIVED.name());
        return toResponse(noticeRepository.save(e));
    }

    private NoticeEntity getById(Long id) {
        return noticeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("公告不存在"));
    }

    private NoticeResponse toResponse(NoticeEntity e) {
        return new NoticeResponse(
                e.getId(),
                e.getNoticeType(),
                e.getTargetType(),
                e.getTargetId(),
                e.getTitle(),
                e.getContent(),
                e.getPublisherId(),
                e.getPublishTime(),
                e.getStatus()
        );
    }
}

