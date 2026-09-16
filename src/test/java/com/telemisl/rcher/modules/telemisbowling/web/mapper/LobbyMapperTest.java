package com.telemisl.rcher.modules.telemisbowling.web.mapper;

import com.telemisl.rcher.modules.telemisbowling.lobby.Lobby;
import com.telemisl.rcher.modules.telemisbowling.lobby.LobbyMember;
import com.telemisl.rcher.modules.telemisbowling.web.dto.LobbyMembershipDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class LobbyMapperTest {

    private final LobbyMapper mapper = Mappers.getMapper(LobbyMapper.class);

    @Test
    @DisplayName("toMembershipDto expose l'identité complète du membre, jeton de session inclus")
    void toMembershipDto_mapsAllMemberFields() {
        Lobby lobby = Lobby.create();
        LobbyMember member = lobby.addMember("Alice");

        LobbyMembershipDto dto = mapper.toMembershipDto(member);

        assertThat(dto.lobbyId()).isEqualTo(lobby.getId());
        assertThat(dto.memberId()).isEqualTo(member.getId());
        assertThat(dto.displayName()).isEqualTo("Alice");
        assertThat(dto.sessionToken()).isEqualTo(member.getSessionToken());
    }
}