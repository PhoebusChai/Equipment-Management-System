package com.ems.modules.lab.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ems.modules.lab.dto.LabResponse;
import com.ems.modules.lab.dto.LabCreateRequest;
import com.ems.modules.lab.entity.LabEntity;
import com.ems.modules.lab.repository.LabRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class LabService {

    private final LabRepository labRepository;
    private final ObjectMapper objectMapper;
    private final Path labImageDir;
    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    public LabService(
            LabRepository labRepository,
            ObjectMapper objectMapper,
            @Value("${app.upload.lab-image-dir:uploads/labs}") String labImageDir
    ) {
        this.labRepository = labRepository;
        this.objectMapper = objectMapper;
        this.labImageDir = Paths.get(labImageDir).toAbsolutePath().normalize();
    }

    public List<LabResponse> listAll() {
        return labRepository.findAll().stream().map(this::toResponse).toList();
    }

    public LabResponse getById(Long id) {
        LabEntity entity = labRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("实验室不存在"));
        return toResponse(entity);
    }

    @Transactional
    public LabResponse create(LabCreateRequest request) {
        return createWithImages(request, Collections.emptyList());
    }

    @Transactional
    public LabResponse createWithImages(LabCreateRequest request, List<MultipartFile> images) {
        String code = request.labCode().trim().toUpperCase();
        if (labRepository.existsByLabCode(code)) {
            throw new IllegalArgumentException("实验室编码已存在");
        }

        List<String> imageUrls = saveImages(images);

        LabEntity entity = new LabEntity();
        entity.setLabCode(code);
        entity.setLabName(request.name().trim());
        entity.setLabType(request.type().trim().toUpperCase());
        entity.setBuilding(request.building().trim());
        entity.setCollege(request.college() == null ? null : request.college().trim());
        entity.setCapacity(request.capacity());
        entity.setStatus("AVAILABLE");
        entity.setDescription(request.description() == null ? "" : request.description().trim());
        entity.setImageUrls(writeImageUrls(imageUrls));

        return toResponse(labRepository.save(entity));
    }

    public Resource loadLabImage(String filename) {
        try {
            String safeName = sanitizeFileName(filename);
            Path path = labImageDir.resolve(safeName).normalize();
            if (!path.startsWith(labImageDir) || !Files.exists(path)) {
                throw new IllegalArgumentException("图片不存在");
            }
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("图片路径非法");
        }
    }

    public MediaType detectMediaType(String filename) {
        try {
            Path path = labImageDir.resolve(sanitizeFileName(filename)).normalize();
            String ct = Files.probeContentType(path);
            return ct == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(ct);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public boolean existsById(Long id) {
        return labRepository.existsById(id);
    }

    public boolean isAvailable(Long id) {
        LabEntity entity = labRepository.findById(id).orElse(null);
        if (entity == null) return false;
        String status = entity.getStatus() == null ? "" : entity.getStatus().toUpperCase();
        return "AVAILABLE".equals(status);
    }

    @Transactional
    public void updateStatusById(Long id, String status) {
        LabEntity entity = labRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("实验室不存在"));
        entity.setStatus(status == null ? "AVAILABLE" : status.toUpperCase());
        labRepository.save(entity);
    }

    @Transactional
    public void markInUseByName(String labName) {
        if (labName == null || labName.isBlank()) return;
        LabEntity entity = labRepository.findFirstByLabNameIgnoreCase(labName.trim());
        if (entity == null) return;
        entity.setStatus("IN_USE");
        labRepository.save(entity);
    }

    private List<String> saveImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return Collections.emptyList();
        try {
            Files.createDirectories(labImageDir);
        } catch (IOException e) {
            throw new IllegalArgumentException("创建图片目录失败");
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : images) {
            if (file == null || file.isEmpty()) continue;
            String original = file.getOriginalFilename();
            String ext = getFileExt(original);
            if (!ALLOWED_IMAGE_EXT.contains(ext.toLowerCase())) {
                throw new IllegalArgumentException("仅支持 jpg/jpeg/png/gif/webp/bmp 格式图片");
            }
            String stored = UUID.randomUUID().toString().replace("-", "") + "." + ext.toLowerCase();
            Path target = labImageDir.resolve(stored).normalize();
            try {
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IllegalArgumentException("保存图片失败");
            }
            urls.add("/api/labs/images/" + stored);
        }
        return urls;
    }

    private String writeImageUrls(List<String> imageUrls) {
        try {
            return objectMapper.writeValueAsString(imageUrls == null ? Collections.emptyList() : imageUrls);
        } catch (Exception e) {
            throw new IllegalArgumentException("图片信息序列化失败");
        }
    }

    private List<String> readImageUrls(String value) {
        if (value == null || value.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String getFileExt(String name) {
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf(".") + 1);
    }

    private String sanitizeFileName(String name) {
        return Paths.get(name).getFileName().toString();
    }

    private LabResponse toResponse(LabEntity e) {
        return new LabResponse(
                e.getId(),
                e.getLabCode(),
                e.getLabName(),
                e.getLabType(),
                e.getBuilding(),
                e.getCollege(),
                e.getCapacity(),
                e.getStatus(),
                e.getDescription(),
                readImageUrls(e.getImageUrls())
        );
    }
}

