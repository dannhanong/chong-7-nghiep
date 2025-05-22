package com.dan.identity_service.security;

public class Endpoints {
        private static final String BASE_URL_AUTH = "/identity/auth";
        private static final String BASE_URL_USER = "/identity/user";
        private static final String BASE_URL_EDUCATION = "/identity/education";
        private static final String BASE_URL_EXPERIENCE = "/identity/experiences";
        private static final String BASE_URL_SKILL = "/identity/skills";

        public static final String[] PUBLIC_POST_ENDPOINTS = {
                        BASE_URL_AUTH + "/login",
                        BASE_URL_AUTH + "/signup",
                        BASE_URL_AUTH + "/forgot-password",
                        BASE_URL_AUTH + "/refresh",
                        BASE_URL_AUTH + "/oauth2/**"
        };

        public static final String[] ADMIN_POST_ENDPOINTS = {
                        BASE_URL_AUTH + "/admin/**",
                        BASE_URL_USER + "/admin/**",
        };

        public static final String[] RECRUITER_POST_ENDPOINTS = {
                        BASE_URL_USER + "/recruiter/**",
        };

        public static final String[] ADMIN_PUT_ENDPOINTS = {
                        BASE_URL_AUTH + "/admin/**",
        };

        public static final String[] ADMIN_GET_ENDPOINTS = {
                        BASE_URL_AUTH + "/admin/**"
        };

        public static final String[] ADMIN_DELETE_ENDPOINTS = {
                        BASE_URL_AUTH + "/admin/delete/**"
        };

        public static final String[] PUBLIC_GET_ENDPOINTS = {
                        BASE_URL_AUTH + "/verify",
                        BASE_URL_AUTH + "/profile",
                        BASE_URL_AUTH + "/validate",
                        BASE_URL_AUTH + "/user/**",
                        BASE_URL_AUTH + "/get-name/**",
                        BASE_URL_AUTH + "/user/profile/**",
                        BASE_URL_AUTH + "/public/**"
        };

        public static final String[] PUBLIC_PUT_ENDPOINTS = {
                        BASE_URL_AUTH + "/reset-password",
        };
}
