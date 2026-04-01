package com.travelai.domain.ai;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record DayPlan(
        int dayNumber,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        String title,
        List<Activity> activities
) {}
