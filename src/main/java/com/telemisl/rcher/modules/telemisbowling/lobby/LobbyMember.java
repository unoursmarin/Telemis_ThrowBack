package com.telemisl.rcher.modules.telemisbowling.lobby;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Entity
@Table(name = "lobby_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LobbyMember implements Persistable<UUID> {

    @Id
    private UUID id = UUID.randomUUID();

    @Getter(AccessLevel.NONE)
    @ManyToOne(optional = false)
    @JoinColumn(name = "lobby_id", nullable = false)
    private Lobby lobby;

    private String displayName;

    private UUID sessionToken;

    private int joinOrder;

    private boolean ready;

    @Getter(AccessLevel.NONE)
    @Transient
    private boolean isNew = true;

    LobbyMember(Lobby lobby, String displayName, int joinOrder) {
        this.lobby = lobby;
        this.displayName = displayName;
        this.joinOrder = joinOrder;
        this.sessionToken = UUID.randomUUID();
        this.ready = false;
    }

    void setReady(boolean ready) {
        this.ready = ready;
    }

    public UUID getLobbyId() {
        return lobby.getId();
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
