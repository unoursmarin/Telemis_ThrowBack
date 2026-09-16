package com.telemisl.rcher.modules.telemisbowling.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FrameTest {

    @Test
    @DisplayName("une frame vide n'est pas complète")
    void emptyFrame_isNotComplete() {
        Frame frame = Frame.empty(1);

        assertThat(frame.isComplete()).isFalse();
        assertThat(frame.status()).isEqualTo(FrameStatus.IN_PROGRESS);
        assertThat(frame.pinsRemaining()).isEqualTo(15);
    }

    @Test
    @DisplayName("un strike au premier lancer termine la frame immédiatement")
    void strikeOnFirstRoll_endsFrame() {
        Frame frame = Frame.empty(1).withRoll(new Roll(15));

        assertThat(frame.isComplete()).isTrue();
        assertThat(frame.isStrike()).isTrue();
        assertThat(frame.status()).isEqualTo(FrameStatus.STRIKE);
        assertThat(frame.rolls()).hasSize(1);
    }

    @Test
    @DisplayName("un spare au 2e lancer termine la frame")
    void spareOnSecondRoll_endsFrame() {
        Frame frame = Frame.empty(1).withRoll(new Roll(8)).withRoll(new Roll(7));

        assertThat(frame.isComplete()).isTrue();
        assertThat(frame.isSpare()).isTrue();
        assertThat(frame.status()).isEqualTo(FrameStatus.SPARE);
    }

    @Test
    @DisplayName("un spare au 3e lancer termine la frame")
    void spareOnThirdRoll_endsFrame() {
        Frame frame = Frame.empty(1).withRoll(new Roll(5)).withRoll(new Roll(4)).withRoll(new Roll(6));

        assertThat(frame.isComplete()).isTrue();
        assertThat(frame.isSpare()).isTrue();
    }

    @Test
    @DisplayName("une frame ouverte se termine après 3 lancers, 14 quilles au maximum")
    void openFrame_endsAfterThreeRolls() {
        Frame frame = Frame.empty(1).withRoll(new Roll(5)).withRoll(new Roll(4)).withRoll(new Roll(5));

        assertThat(frame.isComplete()).isTrue();
        assertThat(frame.isStrike()).isFalse();
        assertThat(frame.isSpare()).isFalse();
        assertThat(frame.totalPins()).isEqualTo(14);
    }

    @Test
    @DisplayName("un lancer ne peut pas abattre plus de quilles qu'il n'en reste debout")
    void rollExceedingStandingPins_isRejected() {
        Frame frame = Frame.empty(1).withRoll(new Roll(10));

        assertThatThrownBy(() -> frame.withRoll(new Roll(6))).isInstanceOf(InvalidRollException.class);
    }

    @Test
    @DisplayName("un lancer négatif est rejeté")
    void negativeRoll_isRejected() {
        assertThatThrownBy(() -> new Roll(-1)).isInstanceOf(InvalidRollException.class);
    }

    @Test
    @DisplayName("un lancer de plus de 15 quilles est rejeté")
    void rollAboveFifteen_isRejected() {
        assertThatThrownBy(() -> new Roll(16)).isInstanceOf(InvalidRollException.class);
    }

    @Test
    @DisplayName("on ne peut pas ajouter de lancer à une frame déjà complète")
    void addingRollToCompleteFrame_throws() {
        Frame frame = Frame.empty(1).withRoll(new Roll(15));

        assertThatThrownBy(() -> frame.withRoll(new Roll(3))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("dernière frame : un strike au 1er lancer force 4 lancers au total (3 bonus)")
    void lastFrame_strikeForcesFourRolls() {
        Frame frame = Frame.empty(5).withRoll(new Roll(15));

        assertThat(frame.isComplete()).isFalse();

        frame = frame.withRoll(new Roll(8)).withRoll(new Roll(2));

        assertThat(frame.isComplete()).isFalse();

        frame = frame.withRoll(new Roll(3));

        assertThat(frame.isComplete()).isTrue();
        assertThat(frame.rolls()).hasSize(4);
        assertThat(frame.totalPins()).isEqualTo(15 + 8 + 2 + 3);
    }

    @Test
    @DisplayName("dernière frame : un spare au 2e lancer force 4 lancers au total (2 bonus)")
    void lastFrame_spareOnSecondRollForcesFourRolls() {
        Frame frame = Frame.empty(5).withRoll(new Roll(8)).withRoll(new Roll(7));

        assertThat(frame.isComplete()).isFalse();

        frame = frame.withRoll(new Roll(9)).withRoll(new Roll(6));

        assertThat(frame.isComplete()).isTrue();
        assertThat(frame.rolls()).hasSize(4);
        assertThat(frame.totalPins()).isEqualTo(8 + 7 + 9 + 6);
    }

    @Test
    @DisplayName("dernière frame ouverte : pas de lancer bonus, la frame s'arrête à 3 lancers")
    void lastFrame_openFrame_stopsAtThreeRolls() {
        Frame frame = Frame.empty(5).withRoll(new Roll(5)).withRoll(new Roll(4)).withRoll(new Roll(3));

        assertThat(frame.isComplete()).isTrue();
        assertThat(frame.rolls()).hasSize(3);
    }

    @Test
    @DisplayName("dernière frame : après un strike, le râtelier est redressé pour chaque lancer bonus")
    void lastFrame_afterStrike_rackResetsForBonusRolls() {
        Frame frame = Frame.empty(5).withRoll(new Roll(15));

        assertThat(frame.pinsRemaining()).isEqualTo(15);

        frame = frame.withRoll(new Roll(15));

        assertThat(frame.pinsRemaining()).isEqualTo(15);
    }
}
