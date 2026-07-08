package org.example.testvue.websocket;

import org.example.testvue.service.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

/**
 * WebSocket handler for real-time test run updates.
 * Clients connect to /ws/test-run/{taskId} to receive live log + progress.
 */
@Component
public class TestRunWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TestRunWebSocketHandler.class);

    private final WebSocketSessionManager sessionManager;

    public TestRunWebSocketHandler(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String taskId = extractTaskId(session);
        if (taskId != null) {
            sessionManager.register(taskId, session);
        } else {
            try { session.close(CloseStatus.BAD_DATA); } catch (Exception ignored) {}
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Client → server messages not needed for this use case
        // Could be used for heartbeat/ping in future
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String taskId = extractTaskId(session);
        if (taskId != null) sessionManager.remove(taskId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String taskId = extractTaskId(session);
        if (taskId != null) sessionManager.remove(taskId);
    }

    private String extractTaskId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String path = uri.getPath();
        // Path: /ws/test-run/{taskId}
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : null;
    }
}
