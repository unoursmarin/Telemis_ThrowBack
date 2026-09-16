package com.telemisl.rcher.modules.telemisbowling.lobby;

import com.telemisl.rcher.modules.telemisbowling.ws.GameEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LobbyService {

    private final LobbyRepository lobbyRepository;
    private final GameEventPublisher eventPublisher;

    public LobbyService(LobbyRepository lobbyRepository, GameEventPublisher eventPublisher) {
        this.lobbyRepository = lobbyRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public LobbyMember createLobby(String hostDisplayName) {
        Lobby lobby = Lobby.create();
        LobbyMember host = lobby.addMember(hostDisplayName);
        lobbyRepository.save(lobby);
        return host;
    }

    @Transactional
    public LobbyMember join(UUID lobbyId, String displayName) {
        Lobby lobby = findLobby(lobbyId);
        LobbyMember member = lobby.addMember(displayName);
        lobbyRepository.save(lobby);
        eventPublisher.publishLobbyEvent(lobbyId, "playerJoined", lobby.toSnapshot());
        return member;
    }

    @Transactional
    public void leave(UUID lobbyId, UUID sessionToken) {
        Lobby lobby = findLobby(lobbyId);
        lobby.removeMember(sessionToken);
        lobbyRepository.save(lobby);
        eventPublisher.publishLobbyEvent(lobbyId, "playerLeft", lobby.toSnapshot());
    }

    @Transactional
    public LobbySnapshot setReady(UUID lobbyId, UUID sessionToken, boolean ready) {
        Lobby lobby = findLobby(lobbyId);
        lobby.setMemberReady(sessionToken, ready);
        lobbyRepository.save(lobby);
        LobbySnapshot snapshot = lobby.toSnapshot();
        eventPublisher.publishLobbyEvent(lobbyId, "playerReadyChanged", snapshot);
        return snapshot;
    }

    @Transactional(readOnly = true)
    public LobbySnapshot get(UUID lobbyId) {
        return findLobby(lobbyId).toSnapshot();
    }

    private Lobby findLobby(UUID lobbyId) {
        return lobbyRepository.findById(lobbyId).orElseThrow(() -> new LobbyNotFoundException(lobbyId));
    }
}
