package com.lqtigee.sparkai.dto;

public record SessionActionRequest(
        String action,
        Boolean confirmDestructive
) {
}
