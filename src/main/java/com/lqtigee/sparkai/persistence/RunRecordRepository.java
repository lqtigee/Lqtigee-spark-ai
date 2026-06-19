package com.lqtigee.sparkai.persistence;

public class RunRecordRepository {

    private final PostgresConnectionFactory connectionFactory;

    public RunRecordRepository(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void saveStarted(String runId, String source, String sessionId, String modelId) {
        throw new UnsupportedOperationException("Run record persistence is not implemented yet");
    }
}
