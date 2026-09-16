package com.telemisl.rcher.modules.telemisbowling.domain;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;


public final class Frame {

    private final int number;
    @Getter
    private final boolean lastFrame;
    private final List<Roll> rolls;

    private Frame(int number, boolean lastFrame, List<Roll> rolls) {
        this.number = number;
        this.lastFrame = lastFrame;
        this.rolls = rolls;
    }

    static Frame empty(int number) {
        return new Frame(number, number == GameRules.FRAMES_PER_GAME, List.of());
    }

    public int number() {
        return number;
    }

    public List<Roll> rolls() {
        return rolls;
    }

    public int totalPins() {
        return rolls.stream().mapToInt(Roll::pins).sum();
    }

    // Pin remaining ?
    public int pinsRemaining() {
        return pinsStandingBefore(rolls.size());
    }

    public boolean isStrike() {
        return !rolls.isEmpty() && clearsRack(0);
    }

    public boolean isSpare() {
        if (isStrike() || rolls.size() < 2) {
            return false;
        }
        return clearsRack(1) || (rolls.size() >= 3 && clearsRack(2));
    }

    public FrameStatus status() {
        if (!isComplete()) {
            return FrameStatus.IN_PROGRESS;
        }
        if (isStrike()) {
            return FrameStatus.STRIKE;
        }
        if (isSpare()) {
            return FrameStatus.SPARE;
        }
        return FrameStatus.OPEN;
    }

    public boolean isComplete() {
        if (rolls.isEmpty()) {
            return false;
        }
        if (!lastFrame) {
            return clearsRack(rolls.size() - 1) || rolls.size() >= GameRules.BASE_ROLLS_PER_FRAME;
        }
        if (rolls.size() >= GameRules.LAST_FRAME_MAX_ROLLS) {
            return true;
        }
        if (rolls.size() < GameRules.BASE_ROLLS_PER_FRAME) {
            return false;
        }
        return !bonusOwedAfterBaseRolls();
    }

    private boolean bonusOwedAfterBaseRolls() {
        for (int i = 0; i < rolls.size(); i++) {
            if (clearsRack(i)) {
                return true;
            }
        }
        return false;
    }

    Frame withRoll(Roll roll) {
        if (isComplete()) {
            throw new IllegalStateException("La frame " + number + " est déjà complète");
        }
        int standing = pinsStandingBefore(rolls.size());
        if (roll.pins() > standing) {
            throw new InvalidRollException(
                    "Il ne reste que %d quille(s) debout, impossible d'en abattre %d"
                            .formatted(standing, roll.pins()));
        }
        List<Roll> updated = new ArrayList<>(rolls);
        updated.add(roll);
        return new Frame(number, lastFrame, List.copyOf(updated));
    }

    private int pinsStandingBefore(int rollIndex) {
        int standing = GameRules.PINS_PER_FRAME;
        for (int i = 0; i < rollIndex; i++) {
            standing = rackStandingAfter(standing, rolls.get(i).pins());
        }
        return standing;
    }

    private static int rackStandingAfter(int standingBefore, int pinsKnockedDown) {
        return (pinsKnockedDown == standingBefore) ? GameRules.PINS_PER_FRAME : standingBefore - pinsKnockedDown;
    }

    private boolean clearsRack(int rollIndex) {
        return rolls.get(rollIndex).pins() == pinsStandingBefore(rollIndex);
    }
}
