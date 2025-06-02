package com.dan.job_service.security;

public class Endpoints {
        private static final String BASE_JOB_URL = "/job/jobs";
        private static final String BASE_CATEGORY_URL = "/job/categories";
        private static final String BASE_COMPANY_URL = "/job/companies";

        public static final String[] PUBLIC_GET_ENDPOINTS = {
                BASE_JOB_URL + "/public/**",
                BASE_COMPANY_URL + "/public/**",
                BASE_CATEGORY_URL + "/public/**",
        };

        public static final String[] ADMIN_GET_ENDPOINTS = {
        };

        public static final String[] ADMIN_POST_ENDPOINTS = {
                BASE_JOB_URL + "/categories/admin/**",
                BASE_COMPANY_URL + "/admin/**",
        };

        public static final String[] ADMIN_PUT_ENDPOINTS = {
                BASE_JOB_URL + "/categories/admin/**",
        };

        public static final String[] ADMIN_DELETE_ENDPOINTS = {
                BASE_JOB_URL + "/categories/admin/**",
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
