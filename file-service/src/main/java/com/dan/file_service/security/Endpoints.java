package com.dan.file_service.security;

public class Endpoints {
    public static final String[] PRIVATE_POST_ENDPOINTS = {
    };

    public static final String[] PUBLIC_POST_ENDPOINTS = {
        "/files/upload",
    };

    public static final String[] PRIVATE_DELETE_ENDPOINTS = {
            "/files/delete/**",
    };

    public static final String[] ADMIN_POST_ENDPOINTS = {
    };

    public static final String[] PRIVATE_GET_ENDPOINTS = {

    };

    public static final String[] PUBLIC_GET_ENDPOINTS = {
            "/files/preview/**",
    };

    public static final String[] ADMIN_DELETE_ENDPOINTS = {
    };
}
