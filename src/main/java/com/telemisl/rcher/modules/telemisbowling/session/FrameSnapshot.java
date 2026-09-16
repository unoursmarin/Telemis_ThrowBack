package com.telemisl.rcher.modules.telemisbowling.session;

import com.telemisl.rcher.modules.telemisbowling.domain.FrameStatus;

import java.util.List;

public record FrameSnapshot(int number, List<Integer> rolls, FrameStatus status, int score) {
}
