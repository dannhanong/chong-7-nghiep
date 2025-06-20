package com.dan.job_service.dtos.responses;
import java.time.LocalDate;
import java.util.List;

import com.dan.job_service.dtos.enums.WorkingForm;
import com.dan.job_service.dtos.enums.WorkingType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class HashtagResponse {
    String tag;

}
