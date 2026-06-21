package com.lqtigee.sparkai.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lqtigee.sparkai.dto.AttachmentDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.service.AttachmentService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest(properties = "lqtigee.security.api-token=test-token")
@AutoConfigureMockMvc
class AttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttachmentService attachmentService;

    @Test
    void uploadWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/attachments")
                        .file(file()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_MISSING"));

        verifyNoInteractions(attachmentService);
    }

    @Test
    void uploadWithValidTokenReturnsAttachmentDto() throws Exception {
        when(attachmentService.upload(any(MultipartFile.class))).thenReturn(new AttachmentDto(
                "att_file_01",
                "context.txt",
                "text/plain",
                5,
                Instant.parse("2026-06-20T00:00:00Z")
        ));

        mockMvc.perform(multipart("/api/attachments")
                        .file(file())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("att_file_01"))
                .andExpect(jsonPath("$.filename").value("context.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.sizeBytes").value(5))
                .andExpect(jsonPath("$.createdAt").value("2026-06-20T00:00:00Z"));

        verify(attachmentService).upload(any(MultipartFile.class));
    }

    @Test
    void uploadMissingFileWithValidTokenReturnsTypedError() throws Exception {
        when(attachmentService.upload(isNull())).thenThrow(new ApiException(
                ErrorCode.ATTACHMENT_MISSING,
                HttpStatus.BAD_REQUEST,
                "Attachment file is required",
                "file"
        ));

        mockMvc.perform(multipart("/api/attachments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ATTACHMENT_MISSING"))
                .andExpect(jsonPath("$.detail").value("file"));

        verify(attachmentService).upload(isNull());
    }

    @Test
    void uploadServiceFailureReturnsTypedError() throws Exception {
        when(attachmentService.upload(any(MultipartFile.class))).thenThrow(new ApiException(
                ErrorCode.ATTACHMENT_TOO_LARGE,
                HttpStatus.BAD_REQUEST,
                "Attachment is too large",
                "maxUploadBytes=1"
        ));

        mockMvc.perform(multipart("/api/attachments")
                        .file(file())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ATTACHMENT_TOO_LARGE"))
                .andExpect(jsonPath("$.detail").value("maxUploadBytes=1"));

        verify(attachmentService).upload(any(MultipartFile.class));
    }

    @Test
    void deleteWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/attachments/att_00000000000000000000000000000000"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_MISSING"));

        verifyNoInteractions(attachmentService);
    }

    @Test
    void deleteWithValidTokenReturnsTypedSuccess() throws Exception {
        mockMvc.perform(delete("/api/attachments/att_00000000000000000000000000000000")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("att_00000000000000000000000000000000"))
                .andExpect(jsonPath("$.deleted").value(true));

        verify(attachmentService).delete("att_00000000000000000000000000000000");
    }

    @Test
    void deleteMissingAttachmentReturnsTypedError() throws Exception {
        doThrow(new ApiException(
                ErrorCode.ATTACHMENT_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Attachment was not found",
                "att_00000000000000000000000000000000"
        )).when(attachmentService).delete("att_00000000000000000000000000000000");

        mockMvc.perform(delete("/api/attachments/att_00000000000000000000000000000000")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ATTACHMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("att_00000000000000000000000000000000"));

        verify(attachmentService).delete("att_00000000000000000000000000000000");
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "context.txt", "text/plain", "hello".getBytes());
    }
}
