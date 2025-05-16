package com.dan.job_service.dtos.enums;

public enum WorkingType {
    FULL_TIME("Full Time"),
    PART_TIME("Part Time"),
    INTERNSHIP("Internship"),
    FREELANCE("Freelance"),
    CONTRACT("Contract"),
    TEMPORARY("Temporary"),
    VOLUNTEER("Volunteer");

    private final String value;

    WorkingType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static WorkingType fromValue(String value) {
        for (WorkingType type : WorkingType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown working type: " + value);
    }
}
