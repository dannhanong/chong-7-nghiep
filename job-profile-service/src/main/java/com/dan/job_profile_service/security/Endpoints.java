package com.dan.job_profile_service.security;

public class Endpoints {
    private static final String BASE_SKILL_URL = "/jp/skills";
    private static final String BASE_EXPERIENCE_URL = "/jp/experiences";

    public static final String[] PUBLIC_GET_ENDPOINTS = {
            BASE_SKILL_URL + "/public/**",
            BASE_EXPERIENCE_URL + "/public/**",
    };

    public static final String[] ADMIN_GET_ENDPOINTS = {
    };

    public static final String[] ADMIN_POST_ENDPOINTS = {
    };

    public static final String[] ADMIN_PUT_ENDPOINTS = {
    };

    public static final String[] ADMIN_DELETE_ENDPOINTS = {
    };

    public static final String[] RECRUITER_GET_ENDPOINTS = {
    };

    public static final String[] RECRUITER_POST_ENDPOINTS = {
    };

    public static final String[] RECRUITER_PUT_ENDPOINTS = {
    };

    public static final String[] RECRUITER_DELETE_ENDPOINTS = {

    };
}
