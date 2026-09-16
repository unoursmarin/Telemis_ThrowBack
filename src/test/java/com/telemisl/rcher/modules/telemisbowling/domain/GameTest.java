package com.telemisl.rcher.modules.telemisbowling.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameTest {

    @Test
    @DisplayName("une partie sans aucune quille abattue vaut 0")
    void gutterGame_scoresZero() {
        Game game = new Game();

        rollMany(game, 15, 0);

        assertThat(game.isComplete()).isTrue();
        assertThat(game.score()).isZero();
    }

    @Test
    @DisplayName("une partie avec un strike sur chaque lancer de base obtient le score parfait de 300")
    void perfectGame_scoresThreeHundred() {
        Game game = new Game();

        rollMany(game, 8, 15);

        assertThat(game.isComplete()).isTrue();
        assertThat(game.score()).isEqualTo(GameRules.PERFECT_SCORE);
    }

    @Test
    @DisplayName("une frame ouverte vaut au maximum 14 (somme des 3 lancers)")
    void openFrame_maxScoreIsFourteen() {
        Game game = new Game();

        game.lancer(5);
        game.lancer(4);
        game.lancer(5);
        rollMany(game, 12, 0);

        assertThat(game.frameScores().getFirst()).isEqualTo(14);
    }

    @Test
    @DisplayName("un spare suivi de deux strikes vaut 45 (15 + 2 lancers suivants)")
    void spareFollowedByTwoStrikes_scoresFortyFive() {
        Game game = new Game();

        game.lancer(8);
        game.lancer(7); // spare, frame 1
        game.lancer(15); // strike, frame 2
        game.lancer(15); // strike, frame 3
        rollMany(game, 6, 0); // termine frames 4 et 5 sans impact sur la frame 1

        assertThat(game.frameScores().getFirst()).isEqualTo(45);
    }

    @Test
    @DisplayName("un strike suivi de deux strikes et d'un lancer plein vaut 60 (15 + 3 lancers suivants)")
    void strikeFollowedByFullBonusRolls_scoresSixty() {
        Game game = new Game();

        game.lancer(15); // strike, frame 1
        game.lancer(15); // strike, frame 2
        game.lancer(15); // strike, frame 3
        game.lancer(15); // strike, frame 4
        rollMany(game, 3, 0); // frame 5 ouverte, sans impact sur la frame 1

        assertThat(game.frameScores().getFirst()).isEqualTo(60);
    }

    @Test
    @DisplayName("lancer() rejette un nombre de quilles hors bornes")
    void lancer_rejectsInvalidPinCount() {
        Game game = new Game();

        assertThatThrownBy(() -> game.lancer(-1)).isInstanceOf(InvalidRollException.class);
        assertThatThrownBy(() -> game.lancer(16)).isInstanceOf(InvalidRollException.class);
    }

    @Test
    @DisplayName("lancer() rejette un nombre de quilles supérieur à ce qu'il reste debout")
    void lancer_rejectsRollExceedingStandingPins() {
        Game game = new Game();
        game.lancer(10);

        assertThatThrownBy(() -> game.lancer(6)).isInstanceOf(InvalidRollException.class);
    }

    @Test
    @DisplayName("lancer() après la fin de la partie lève une exception")
    void lancer_afterGameComplete_throws() {
        Game game = new Game();
        rollMany(game, 15, 0);

        assertThatThrownBy(() -> game.lancer(0)).isInstanceOf(GameAlreadyCompleteException.class);
    }

    //replays the second example from the problem statement)

    @Test
    @DisplayName("reproduit le 2e exemple chiffré de l'énoncé (cumuls 26/37/62/73/101)")
    void enonceExample_matchesGivenCumulativeScores() {
        Game game = new Game();

        game.lancer(15); // frame 1 : X
        game.lancer(8); // frame 2
        game.lancer(1);
        game.lancer(2);
        game.lancer(1); // frame 3
        game.lancer(2);
        game.lancer(12); // '/', complète le spare (1+2+12=15)
        game.lancer(6); // frame 4
        game.lancer(4);
        game.lancer(1);
        game.lancer(15); // frame 5 : X
        game.lancer(8);
        game.lancer(2);
        game.lancer(3);

        assertThat(game.isComplete()).isTrue();
        assertThat(cumulativeScores(game)).containsExactly(26, 37, 62, 73, 101);
        assertThat(game.score()).isEqualTo(101);
    }

    private static java.util.List<Integer> cumulativeScores(Game game) {
        java.util.List<Integer> cumulative = new java.util.ArrayList<>();
        int running = 0;
        for (int frameScore : game.frameScores()) {
            running += frameScore;
            cumulative.add(running);
        }
        return cumulative;
    }

    private static void rollMany(Game game, int times, int pins) {
        for (int i = 0; i < times; i++) {
            game.lancer(pins);
        }
    }
}
