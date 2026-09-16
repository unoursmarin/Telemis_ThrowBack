package com.telemisl.rcher.modules.telemisbowling.domain;

import java.util.ArrayList;
import java.util.List;

// Score calculation according to frames and rolls
final class ScoreCalculator {

    private ScoreCalculator() {
    }

    static int totalScore(List<Frame> frames) {
        return frameScores(frames).stream().mapToInt(Integer::intValue).sum();
    }

    static List<Integer> frameScores(List<Frame> frames) {
        List<Integer> scores = new ArrayList<>(frames.size());
        for (int i = 0; i < frames.size(); i++) {
            scores.add(frameScore(frames, i));
        }
        return List.copyOf(scores);
    }

    private static int frameScore(List<Frame> frames, int frameIndex) {
        Frame frame = frames.get(frameIndex);
        if (!frame.isComplete()) {
            return 0;
        }
        if (frame.isLastFrame()) {
            return frame.totalPins();
        }
        if (frame.isStrike()) {
            return GameRules.PINS_PER_FRAME + sumNextRolls(frames, frameIndex + 1, 3);
        }
        if (frame.isSpare()) {
            return GameRules.PINS_PER_FRAME + sumNextRolls(frames, frameIndex + 1, 2);
        }
        return frame.totalPins();
    }

    private static int sumNextRolls(List<Frame> frames, int fromFrameIndex, int count) {
        int sum = 0;
        int remaining = count;
        for (int frameIndex = fromFrameIndex; remaining > 0 && frameIndex < frames.size(); frameIndex++) {
            for (Roll roll : frames.get(frameIndex).rolls()) {
                if (remaining == 0) {
                    break;
                }
                sum += roll.pins();
                remaining--;
            }
        }
        return sum;
    }
}
