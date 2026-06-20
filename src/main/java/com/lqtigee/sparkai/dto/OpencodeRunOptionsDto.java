package com.lqtigee.sparkai.dto;

import java.util.List;

public record OpencodeRunOptionsDto(
        String agent,
        Boolean fork,
        Boolean share,
        String variant,
        Boolean thinking,
        Boolean replay,
        Integer replayLimit,
        List<String> fileAttachmentIds,
        Boolean dangerouslySkipPermissions
) {
}
