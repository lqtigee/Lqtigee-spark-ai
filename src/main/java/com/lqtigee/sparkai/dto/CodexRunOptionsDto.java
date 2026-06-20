package com.lqtigee.sparkai.dto;

import java.util.List;

public record CodexRunOptionsDto(
        List<String> imageAttachmentIds,
        String profile,
        String sandbox,
        String approvalPolicy,
        Boolean searchEnabled,
        List<String> addDirAttachmentIds,
        List<ConfigOverrideDto> configOverrides,
        String outputSchemaAttachmentId
) {
    public record ConfigOverrideDto(
            String key,
            String value
    ) {
    }
}
