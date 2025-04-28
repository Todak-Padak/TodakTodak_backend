package com.padaks.todaktodak.untact.socket;


import com.fasterxml.jackson.databind.ObjectMapper;

import com.padaks.todaktodak.untact.domain.Room;
import com.padaks.todaktodak.untact.domain.WebSocketMessage;
import com.padaks.todaktodak.untact.service.RedisPublisher;
import com.padaks.todaktodak.untact.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


//@Component
@Component("customSignalHandler")
public class SignalHandler extends TextWebSocketHandler implements MessageListener {
    @Autowired
    private RoomService roomService;
    @Autowired
    private RedisPublisher redisPublisher;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();

    // session id to room mapping
    private Map<String, Room> sessionIdToRoomMap = new HashMap<>();

    // signalling에 사용되는 메시지 타입
    private static final String MSG_TYPE_TEXT = "text";
    private static final String MSG_TYPE_OFFER = "offer";
    private static final String MSG_TYPE_ANSWER = "answer";
    private static final String MSG_TYPE_ICE = "ice";
    private static final String MSG_TYPE_JOIN = "join";
    private static final String MSG_TYPE_LEAVE = "leave";

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) {
        logger.info("[ws] Session has been closed with status {}", status);
        sessionIdToRoomMap.remove(session.getId());
    }

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) {
        // webSocket has been opened, send a message to the client
        // when data field contains 'true' value, the client starts negotiating
        // to establish peer-to-peer connection, otherwise they wait for a counterpart
        sendMessage(session, new WebSocketMessage("Server", MSG_TYPE_JOIN, Boolean.toString(!sessionIdToRoomMap.isEmpty()), null, null));
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage textMessage) {
        // a message has been received
        try {
            WebSocketMessage message = objectMapper.readValue(textMessage.getPayload(), WebSocketMessage.class);
            logger.info("[ws] Message of {} type from {} received", message.getType(), message.getFrom());
            String userName = message.getFrom(); // 보낸 사용자
            String data = message.getData(); // 방 정보

            Room room;
            switch (message.getType()) {
                // text message from client has been received
                case MSG_TYPE_TEXT:
                    logger.info("[ws] Text message: {}", message.getData());
                    break;

                case MSG_TYPE_OFFER:
                case MSG_TYPE_ANSWER:
                case MSG_TYPE_ICE:
                    Object candidate = message.getCandidate();
                    Object sdp = message.getSdp();
                    logger.info("[ws] Signal: {}",
                            candidate != null
                                    ? candidate.toString().substring(0, 64)
                                    : sdp.toString().substring(0, 64));
                    // Redis로 메시지 publish
                    redisPublisher.publish(message);
                    break;

                // 사용자가 화상채팅에 들어왔을 때
                case MSG_TYPE_JOIN:
                    // message.data contains connected room id
                    logger.info("[ws] {} has joined Room: #{}", userName, message.getData());
                    room = roomService.findRoomByStringId(data)
                            .orElseThrow(() -> new IOException("Invalid room number received!"));
                    // add client to the Room clients list
                    roomService.addClient(room, userName, session);
                    sessionIdToRoomMap.put(session.getId(), room);
                    break;

                // 사용자가 화상채팅을 나갔을 때
                case MSG_TYPE_LEAVE:
                    logger.info("[ws] {} is going to leave Room: #{}", userName, message.getData());
                    room = sessionIdToRoomMap.get(session.getId());
                    // leave 메시지 보낸 회원 찾기
                    Optional<String> client = roomService.getClients(room).entrySet().stream()
                            .filter(entry -> Objects.equals(entry.getValue().getId(), session.getId()))
                            .map(Map.Entry::getKey)
                            .findAny();
                    client.ifPresent(c -> {
                        // 해당 멤버 삭제
                        roomService.removeClientByName(room, c);
                        // Redis를 통해 메시지 브로드캐스트
                        redisPublisher.publish(new WebSocketMessage(
                                userName, MSG_TYPE_LEAVE, message.getData(), null, null
                        ));

                        // 방에 남은 사용자가 없으면 방 삭제
                        if (roomService.getClients(room).isEmpty()) {
                            logger.info("[ws] Room #{} is empty and will be removed", message.getData());
                            roomService.removeRoom(session.getId()); // 방 삭제 메서드 호출
                        }
                    });
                    break;

                // 예기치 못한 메시지를 받을 때, 로그
                default:
                    logger.info("[ws] Type of the received message {} is undefined!", message.getType());
            }

        } catch (IOException e) {
            logger.info("An error occured: {}", e.getMessage());
        }
    }

    private synchronized void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            logger.debug("An error occured: {}", e.getMessage());
        }
    }

    // Redis 메시지를 수신하는 메서드
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {// 바이트 배열을 String으로 변환
            String jsonMessage = new String(message.getBody(), StandardCharsets.UTF_8);

            // JSON 메시지를 WebSocketMessage 객체로 역직렬화
            WebSocketMessage webSocketMessage = objectMapper.readValue(jsonMessage, WebSocketMessage.class);
            Room room = roomService.findRoomById(webSocketMessage.getData()).orElseThrow(()->new RuntimeException("방을 찾을 수 없음"));
            if (room != null) {
                Map<String, WebSocketSession> clients = roomService.getClients(room);
                for (Map.Entry<String, WebSocketSession> client : clients.entrySet()) {
                    if (!client.getKey().equals(webSocketMessage.getFrom())) {  // Don't send to the leaving client
                        sendMessage(client.getValue(), webSocketMessage);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("An error occurred while processing the message: {}", e.getMessage());
        }
    }
}
