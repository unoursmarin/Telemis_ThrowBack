package com.telemisl.rcher.modules.telemisbowling.web.mapper;

import com.telemisl.rcher.modules.telemisbowling.lobby.LobbyMember;
import com.telemisl.rcher.modules.telemisbowling.web.dto.LobbyMembershipDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// Mapping to MembershipDto is kept here because it is the only response that carries a session token (private data for the concerned member), and therefore the only one that cannot be carried by LobbySnapshot (public view shared between REST and WebSocket, see package lobby).
@Mapper(componentModel = "spring")
public interface LobbyMapper {

    @Mapping(target = "lobbyId", source = "lobbyId")
    @Mapping(target = "memberId", source = "id")
    LobbyMembershipDto toMembershipDto(LobbyMember member);
}
