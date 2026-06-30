package com.apigateway.tenant.constants;

public class Appconstants {
    private Appconstants() {
    }
        // JWT
        public static final String JWT_TOKEN_PREFIX = "Bearer ";
        public static final String JWT_HEADER_NAME = "Authorization ";
        public static final long JWT_ACCESS_TOKEN_EXPIRY_MS = 15 * 60 * 1000L;
        public static final long JWT_REFRESH_TOKEN_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000L;

        // API KEY
        public static final String API_KEY_PREFIX = "gw_";
        public static final int API_KEY_PREFIX_LENGTH = 8;
        public static final String API_KEY_HEADER = "X-API-Key";

        // pagination
        public static final int DEFAULT_PAGE_NUMBER = 0;
        public static final int DEFAULT_PAGE_SIZE = 20;
        public static final int MAX_PAGE_SIZE = 100;

        // Roles
        public static final String ROLE_OWNER = "OWNER";
        public static final String ROLE_ADMIN = "ADMIN";
        public static final String ROLE_DEVELOPER = "DEVELOPER";
        public static final String ROLE_VIEWER = "VIEWER";

    }
