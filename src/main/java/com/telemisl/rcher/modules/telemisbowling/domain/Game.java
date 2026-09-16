package com.telemisl.rcher.modules.telemisbowling.domain;

import java.util.ArrayList;
import java.util.List;

//Our Gmaing class
public final class Game {

    private List<Frame> frames;

    public Game() {
        List<Frame> initial = new ArrayList<>(GameRules.FRAMES_PER_GAME);
        for (int number = 1; number <= GameRules.FRAMES_PER_GAME; number++) {
            initial.add(Frame.empty(number));
        }
        this.frames = List.copyOf(initial);
    }

    /**.
     * Gives the result of the next roll for this player.
     */
    public void lancer(int quilles) {
        int frameIndex = currentFrameIndex();
        Frame updatedFrame = frames.get(frameIndex).withRoll(new Roll(quilles));

        List<Frame> updatedFrames = new ArrayList<>(frames);
        updatedFrames.set(frameIndex, updatedFrame);
        this.frames = List.copyOf(updatedFrames);
    }

    public boolean isComplete() {
        return frames.get(GameRules.FRAMES_PER_GAME - 1).isComplete();
    }

    /**
     * Returns the current frame for this game.
     */
    public Frame currentFrame() {
        return frames.get(currentFrameIndex());
    }

    /** Total score of the game (incomplete frames count as 0). */
    public int score() {
        return ScoreCalculator.totalScore(frames);
    }

    /** Score of each frame, in order (incomplete frame = 0). */
    public List<Integer> frameScores() {
        return ScoreCalculator.frameScores(frames);
    }

    public List<Frame> frames() {
        return frames;
    }

    private int currentFrameIndex() {
        for (int i = 0; i < frames.size(); i++) {
            if (!frames.get(i).isComplete()) {
                return i;
            }
        }
        throw new GameAlreadyCompleteException(
                "La partie est terminée, aucun lancer supplémentaire n'est accepté");
    }
}
