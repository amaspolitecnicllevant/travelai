package com.travelai.domain.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.math.BigDecimal;

public record Activity(
        String time,
        String name,
        String description,
        String location,
        BigDecimal estimatedCost,
        Category category
) {
    public enum Category {
        CULTURE, FOOD, LEISURE, TRANSPORT;

        @JsonCreator
        public static Category fromString(String value) {
            if (value == null) return LEISURE;
            try {
                return Category.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return LEISURE;
            }
        }

        @JsonValue
        public String toJson() {
            return this.name();
        }
    }
}
