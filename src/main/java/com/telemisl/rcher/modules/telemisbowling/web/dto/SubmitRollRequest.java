package com.telemisl.rcher.modules.telemisbowling.web.dto;

import com.telemisl.rcher.modules.telemisbowling.domain.GameRules;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SubmitRollRequest(@Min(0) @Max(GameRules.PINS_PER_FRAME) int pins) {
}
