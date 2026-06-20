package com.lqtigee.sparkai.dto;

import java.util.List;

public record SessionTranscriptDto(
        RemoteSessionDto session,
        List<SessionMessageDto> messages
) {
}
