package com.telemisl.rcher.modules.telemisbowling.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {
}
