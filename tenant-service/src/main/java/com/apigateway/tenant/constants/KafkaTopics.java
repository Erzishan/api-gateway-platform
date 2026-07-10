package com.apigateway.tenant.constants;

public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String TENANT_REGISTERED = "tenant.registered";
    public static final String API_KEY_CREATED = "apikey.created";
    public static final String API_KEY_REVOKED = "apikey.revoked";
    public static final String AUDIT_EVENTS = "audit.events";
}