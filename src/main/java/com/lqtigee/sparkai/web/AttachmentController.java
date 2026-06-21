package com.lqtigee.sparkai.web;

import com.lqtigee.sparkai.dto.AttachmentDto;
import com.lqtigee.sparkai.service.AttachmentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping("/api/attachments")
    public AttachmentDto upload(@RequestParam(value = "file", required = false) MultipartFile file) {
        return attachmentService.upload(file);
    }

    @DeleteMapping("/api/attachments/{id}")
    public DeleteAttachmentResponse delete(@PathVariable String id) {
        attachmentService.delete(id);
        return new DeleteAttachmentResponse(id, true);
    }

    public record DeleteAttachmentResponse(String id, boolean deleted) {
    }
}
