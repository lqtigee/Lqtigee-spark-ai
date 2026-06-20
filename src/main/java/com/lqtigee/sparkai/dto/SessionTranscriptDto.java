package com.lqtigee.sparkai.dto;

import java.util.List;

public record SessionTranscriptDto(
        RemoteSessionDto session,
        List<SessionMessageDto> messages,
        TranscriptPageInfoDto pageInfo
) {

    public SessionTranscriptDto(RemoteSessionDto session, List<SessionMessageDto> messages) {
        this(session, messages, TranscriptPageInfoDto.fromMessages(messages, false));
    }

    public record TranscriptPageInfoDto(
            String oldestCursor,
            String newestCursor,
            boolean hasMoreBefore
    ) {
        public static TranscriptPageInfoDto fromMessages(List<SessionMessageDto> messages, boolean hasMoreBefore) {
            if (messages == null || messages.isEmpty()) {
                return new TranscriptPageInfoDto(null, null, hasMoreBefore);
            }
            return new TranscriptPageInfoDto(
                    messages.getFirst().id(),
                    messages.getLast().id(),
                    hasMoreBefore
            );
        }
    }
}
