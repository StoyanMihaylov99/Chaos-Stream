package com.portfolio.chaosstream.auth;

class SecurityConfigConstants {
    private SecurityConfigConstants() {
        /* This utility class should not be instantiated */
    }

    static final String LOCALHOST = "http://127.0.0.1:8080" ;

    static final String AUTH_KEYS_PATH = "/auth/v1/keys";
    static final String AUTH_TOKEN_PATH = "/auth/v1/token";
    static final String AUTH_AUTHORIZED_PATH = LOCALHOST + "/authorized";
    static final String AUTH_CLIENT_OIDC_PATH = LOCALHOST + "/login/oauth2/code/messaging-client-oidc";

    static final String CLIENT_ID = "transaction-producer-01";
    static final String CLIENT_SECRET = "{noop}secret";

    static final String MESSAGE_READ_VALUE = "message.read";
    static final String MESSAGE_READ_WRITE = "message.write";
}
