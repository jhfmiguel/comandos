package com.comandos.documents.controller;

import com.comandos.documents.dto.ProcessAttachmentContract.*;
import com.comandos.documents.service.ProcessAttachmentService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/erp/documents")
public class ProcessAttachmentController {
    private final ProcessAttachmentService service;

    public ProcessAttachmentController(ProcessAttachmentService service) {
        this.service = service;
    }

    @GetMapping("/policy/{processType}")
    public PolicyView policy(@PathVariable String processType) {
        return service.policy(processType);
    }

    @GetMapping("/{processType}/{recordId}")
    public AttachmentList list(@PathVariable String processType, @PathVariable long recordId,
            @RequestParam long organizationId, @RequestParam(required = false) Long unitId,
            @RequestParam(defaultValue = "false") boolean includeHistory) {
        return service.list(processType, recordId, organizationId, unitId, includeHistory);
    }

    @GetMapping("/{processType}/{recordId}/compliance")
    public ComplianceView compliance(@PathVariable String processType, @PathVariable long recordId,
            @RequestParam long organizationId, @RequestParam(required = false) Long unitId) {
        return service.compliance(processType, recordId, organizationId, unitId);
    }

    @PostMapping(value = "/{processType}/{recordId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentView upload(@PathVariable String processType, @PathVariable long recordId,
            @RequestParam long organizationId, @RequestParam(required = false) Long unitId,
            @RequestParam String documentType, @RequestParam String title,
            @RequestPart("file") MultipartFile file) throws IOException {
        return service.upload(processType, recordId, organizationId, unitId, documentType, title,
            file.getOriginalFilename(), file.getContentType(), file.getBytes());
    }

    @PostMapping(value = "/attachments/{attachmentId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentView replace(@PathVariable long attachmentId,
            @RequestParam(required = false) String title,
            @RequestPart("file") MultipartFile file) throws IOException {
        return service.replace(attachmentId, title, file.getOriginalFilename(), file.getContentType(), file.getBytes());
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> download(@PathVariable long attachmentId) {
        var value = service.download(attachmentId);
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(value.contentType());
        } catch (IllegalArgumentException exception) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
            .contentType(contentType)
            .contentLength(value.content().length)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(value.fileName(), StandardCharsets.UTF_8).build().toString())
            .body(value.content());
    }
}
