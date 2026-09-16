package com.telemisl.rcher.modules.telemisbowling.web;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemisl.rcher.modules.telemisbowling.lobby.LobbySnapshot;
import com.telemisl.rcher.modules.telemisbowling.session.GameSessionSnapshot;
import com.telemisl.rcher.modules.telemisbowling.session.GameStartedPayload;
import com.telemisl.rcher.modules.telemisbowling.session.MePayload;
import com.telemisl.rcher.modules.telemisbowling.session.RollUpdateEvent;
import com.telemisl.rcher.modules.telemisbowling.web.dto.ApiResponse;
import com.telemisl.rcher.modules.telemisbowling.web.dto.CreateLobbyRequest;
import com.telemisl.rcher.modules.telemisbowling.web.dto.JoinLobbyRequest;
import com.telemisl.rcher.modules.telemisbowling.web.dto.LobbyMembershipDto;
import com.telemisl.rcher.modules.telemisbowling.web.dto.SetReadyRequest;
import com.telemisl.rcher.modules.telemisbowling.web.dto.SubmitRollRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Helpers HTTP communs aux tests d'API REST (parcours nominal et chemins
 * d'erreur), passant par la vraie pile MVC (contrôleurs + services + JPA/H2).
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractApiTest {

    @Autowired
    protected MockMvc mockMvc;


     // ObjectMapper pour sérialiser/désérialiser les payloads JSON.

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected LobbyMembershipDto createLobby(String displayName) throws Exception {
        return sendPost("/api/lobbies", new CreateLobbyRequest(displayName), null, 201, LobbyMembershipDto.class);
    }

    protected LobbyMembershipDto join(UUID lobbyId) throws Exception {
        return sendPost("/api/lobbies/" + lobbyId + "/join", new JoinLobbyRequest("Bob"), null, 201, LobbyMembershipDto.class);
    }

    protected void setReady(UUID lobbyId, UUID sessionToken) throws Exception {
        sendPost("/api/lobbies/" + lobbyId + "/ready", new SetReadyRequest(true), sessionToken, 200, LobbySnapshot.class);
    }

    protected GameStartedPayload start(UUID lobbyId, UUID sessionToken) throws Exception {
        return sendPost("/api/lobbies/" + lobbyId + "/start", null, sessionToken, 200, GameStartedPayload.class);
    }

    protected RollUpdateEvent submitRoll(UUID gameId, UUID sessionToken, int pins) throws Exception {
        return sendPost("/api/games/" + gameId + "/rolls", new SubmitRollRequest(pins), sessionToken, 200, RollUpdateEvent.class);
    }

    protected GameSessionSnapshot getGame(UUID gameId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/games/" + gameId))
                .andExpect(status().isOk())
                .andReturn();
        return parse(result, GameSessionSnapshot.class).data();
    }

    protected <T> T sendPost(String url, Object body, UUID sessionToken, int expectedStatus, Class<T> dataType) throws Exception {
        ApiResponse<T> response = sendPostRaw(url, body, sessionToken, expectedStatus, dataType);
        assertThat(response.success()).isTrue();
        return response.data();
    }

    protected <T> ApiResponse<T> sendPostRaw(String url, Object body, UUID sessionToken, int expectedStatus, Class<T> dataType)
            throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(url);
        if (body != null) {
            requestBuilder = requestBuilder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        if (sessionToken != null) {
            requestBuilder = requestBuilder.header("X-Session-Token", sessionToken.toString());
        }
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return parse(result, dataType);
    }

    protected <T> ApiResponse<T> getRaw(String url, Class<T> dataType) throws Exception {
        return getRaw(url, null, 404, dataType);
    }

    protected <T> ApiResponse<T> getRaw(String url, UUID sessionToken, int expectedStatus, Class<T> dataType) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(url);
        if (sessionToken != null) {
            requestBuilder = requestBuilder.header("X-Session-Token", sessionToken.toString());
        }
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return parse(result, dataType);
    }

    protected MePayload whoAmI(UUID gameId, UUID sessionToken) throws Exception {
        ApiResponse<MePayload> response = getRaw("/api/games/" + gameId + "/me", sessionToken, 200, MePayload.class);
        assertThat(response.success()).isTrue();
        return response.data();
    }

    protected <T> ApiResponse<T> parse(MvcResult result, Class<T> dataType) throws Exception {
        JavaType type = objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, dataType);
        return objectMapper.readValue(result.getResponse().getContentAsString(), type);
    }
}
