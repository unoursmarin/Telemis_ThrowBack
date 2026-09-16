package com.telemisl.rcher.modules.telemisbowling.lobby;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//Aggregate root repreting a lobbu
@Entity
@Table(name = "lobbies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lobby implements Persistable<UUID> {

    @Id
    private UUID id = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    private LobbyStatus status;

    private Instant createdAt;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "lobby", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("joinOrder ASC")
    private List<LobbyMember> members = new ArrayList<>();

    private UUID hostMemberId;

    private UUID gameSessionId;


    @Getter(AccessLevel.NONE)
    @Transient
    private boolean isNew = true;

    public static Lobby create() {
        Lobby lobby = new Lobby();
        lobby.status = LobbyStatus.OPEN;
        lobby.createdAt = Instant.now();
        return lobby;
    }

    public List<LobbyMember> getMembers() {
        return List.copyOf(members);
    }

    public LobbyMember addMember(String displayName) {
        requireStatus(LobbyStatus.OPEN);
        LobbyMember member = new LobbyMember(this, displayName, members.size());
        members.add(member);
        if (hostMemberId == null) {
            hostMemberId = member.getId();
        }
        return member;
    }

    public void removeMember(UUID sessionToken) {
        LobbyMember member = findMember(sessionToken);
        members.remove(member);
        if (members.isEmpty()) {
            status = LobbyStatus.ABANDONED;
        } else {
            assert member.getId() != null;
            if (member.getId().equals(hostMemberId)) {
                hostMemberId = members.getFirst().getId();
            }
        }
    }

    public void setMemberReady(UUID sessionToken, boolean ready) {
        findMember(sessionToken).setReady(ready);
    }

    /** Vérifie que {@code requesterSessionToken} peut démarrer la partie, sans changer l'état. */
    public void assertCanStart(UUID requesterSessionToken) {
        requireStatus(LobbyStatus.OPEN);
        LobbyMember requester = findMember(requesterSessionToken);
        if(requester.getId() == null) {
            throw new InvalidSessionTokenException("Le membre n'a pas d'ID valide");
        }
        if (!requester.getId().equals(hostMemberId)) {
            throw new NotLobbyHostException(id, requester.getId());
        }
        if (members.stream().anyMatch(m -> !m.isReady())) {
            throw new LobbyNotReadyToStartException(id);
        }
    }

    public void markInProgress(UUID gameSessionId) {
        status = LobbyStatus.IN_PROGRESS;
        this.gameSessionId = gameSessionId;
    }

    /** Vue publique réutilisée pour la réponse REST et la diffusion WebSocket. */
    public LobbySnapshot toSnapshot() {
        List<LobbyMemberSnapshot> memberSnapshots = members.stream()
                .map(m -> new LobbyMemberSnapshot(m.getId(), m.getDisplayName(), m.isReady()))
                .toList();
        return new LobbySnapshot(id, status, hostMemberId, gameSessionId, memberSnapshots);
    }

    private LobbyMember findMember(UUID sessionToken) {
        return members.stream()
                .filter(m -> m.getSessionToken().equals(sessionToken))
                .findFirst()
                .orElseThrow(() -> new InvalidSessionTokenException(
                        "Aucun membre de ce lobby ne correspond à ce jeton de session"));
    }

    private void requireStatus(LobbyStatus expected) {
        if (status != expected) {
            throw new LobbyNotOpenException(id);
        }
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PrePersist
    @PostLoad
    void markNotNew() {
        isNew = false;
    }
}
