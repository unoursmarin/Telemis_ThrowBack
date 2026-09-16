package com.telemisl.rcher.modules.telemisbowling.lobby;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LobbyRepository extends JpaRepository<Lobby, UUID> {
}
