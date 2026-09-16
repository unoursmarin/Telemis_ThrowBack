package com.telemisl.rcher.modules.telemisbowling.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

//rest request to /api/lobbies, /api/lobbies/{id}/ready, /api/lobbies/{id}/start, /api/games/{id}/rolls
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameSessionWebSocketTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @DisplayName("un lancer soumis en REST est diffusé aux abonnés STOMP de /topic/games/{id}")
    void rollSubmission_broadcastsStompEvent() throws Exception {
        JsonNode alice = post("/api/lobbies", "{\"displayName\":\"Alice\"}", null);
        UUID lobbyId = UUID.fromString(alice.get("data").get("lobbyId").asText());
        UUID aliceToken = UUID.fromString(alice.get("data").get("sessionToken").asText());

        post("/api/lobbies/" + lobbyId + "/ready", "{\"ready\":true}", aliceToken);
        JsonNode started = post("/api/lobbies/" + lobbyId + "/start", null, aliceToken);
        UUID gameId = UUID.fromString(started.get("data").get("gameSessionId").asText());

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        //The server publishes in application/json (SimpMessagingTemplate + Jackson); the standard converters (including StringMessageConverter) check the declared content-type against their supported MIME types and therefore reject application/json. This test only wants the raw text, without re-parsing it: a minimal homemade converter, without MIME type checking.
        stompClient.setMessageConverter(new MessageConverter() {
            @Override
            public Object fromMessage(@NonNull Message<?> message, @NonNull Class<?> targetClass) {
                Object payload = message.getPayload();
                return (payload instanceof byte[] bytes) ? new String(bytes, StandardCharsets.UTF_8) : String.valueOf(payload);
            }

            @Override
            public Message<?> toMessage(@NonNull Object payload, MessageHeaders headers) {
                return MessageBuilder.withPayload(payload).copyHeaders(headers).build();
            }
        });

        // The endpoint is registered with SockJS (see WebSocketConfig); the
        // sub-path /websocket exposes the raw WebSocket transport, usable
        // directly by a standard STOMP/WS client without SockJS negotiation.
        StompSession stompSession = stompClient
                .connectAsync("ws://localhost:" + port + "/ws/websocket", new StompSessionHandlerAdapter() { })
                .get(5, TimeUnit.SECONDS);

        LinkedBlockingQueue<String> received = new LinkedBlockingQueue<>();
        stompSession.subscribe("/topic/games/" + gameId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(@NonNull StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                received.add((String) payload);
            }
        });

        // Give the subscription time to be registered on the broker before sending a message.
        Thread.sleep(1000);
        JsonNode rollResponse = post("/api/games/" + gameId + "/rolls", "{\"pins\":8}", aliceToken);
        assertThat(rollResponse.get("success").asBoolean())
                .withFailMessage("réponse du lancer : %s", rollResponse)
                .isTrue();

        String message = received.poll(10, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message).contains("\"type\":\"rollRegistered\"");
        assertThat(message).contains("\"displayName\":\"Alice\"");

        stompSession.disconnect();
        stompClient.stop();
    }

    private JsonNode post(String path, String jsonBody, UUID sessionToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        builder = (jsonBody != null)
                ? builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                : builder.POST(HttpRequest.BodyPublishers.noBody());
        if (sessionToken != null) {
            builder = builder.header("X-Session-Token", sessionToken.toString());
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }
}
