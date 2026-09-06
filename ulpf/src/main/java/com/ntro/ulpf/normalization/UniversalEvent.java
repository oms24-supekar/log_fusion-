package com.ntro.ulpf.normalization;

import java.time.LocalDateTime;
import java.util.UUID;

public record UniversalEvent(

        UUID eventId,

        String timestamp,

        Source source,

        Destination destination,

        User user,

        Event event,

        Metadata metadata,

        RawReference rawReference

) {

    public record Source(
            String ip,
            String host,
            String deviceType
    ) {
    }

    public record Destination(
            String ip,
            String host,
            Integer port
    ) {
    }

    public record User(
            String name
    ) {
    }

    public record Event(
            String category,
            String action,
            String outcome,
            String severity
    ) {
    }

    public record Metadata(
            String sourceFormat,
            String parserUsed,
            LocalDateTime processedAt
    ) {
    }

    public record RawReference(
            UUID rawLogId,
            String sha256Hash
    ) {
    }
}