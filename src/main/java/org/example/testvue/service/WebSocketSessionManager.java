package org.example.testvue.service;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WebSocket sessions keyed by taskId.
 * Buffers messages when no session is connected yet, delivering them on register.
 */
@Component
public class WebSocketSessionManager {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketSessionManager.class);
    private static final Gson GSON = new Gson();

    /** taskId → WebSocket session */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /** taskId → buffered messages (delivered when session connects) */
    private final Map<String, List<String>> buffers = new ConcurrentHashMap<>();

    public void register(String taskId, WebSocketSession session) {
        sessions.put(taskId, session);
        LOG.info("WebSocket registered: taskId={}", taskId);
        // Drain buffered messages
        List<String> pending = buffers.remove(taskId);
        if (pending != null) {
            LOG.info("Delivering {} buffered messages for taskId={}", pending.size(), taskId);
            for (String msg : pending) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(msg));
                    }
                } catch (IOException e) {
                    LOG.warn("Failed to deliver buffered message for taskId={}: {}", taskId, e.getMessage());
                }
            }
        }
    }

    public void remove(String taskId) {
        sessions.remove(taskId);
    }

    public boolean hasSession(String taskId) {
        return sessions.containsKey(taskId);
    }

    /** Push a raw log line to the frontend for that taskId. */
    public void pushLine(String taskId, String rawLine) {
        WebSocketSession session = sessions.get(taskId);
        Map<String, Object> msg = new java.util.LinkedHashMap<>();
        msg.put("type", "line");
        msg.put("text", rawLine);
        String json = GSON.toJson(msg);
        if (session == null || !session.isOpen()) {
            buffers.computeIfAbsent(taskId, k -> new ArrayList<>()).add(json);
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            LOG.warn("WebSocket push failed for taskId={}: {}", taskId, e.getMessage());
            sessions.remove(taskId);
        }
    }

    /** Push progress update. */
    public void pushProgress(String taskId, int progress, int progressTotal) {
        WebSocketSession session = sessions.get(taskId);
        Map<String, Object> msg = new java.util.LinkedHashMap<>();
        msg.put("type", "progress");
        msg.put("progress", progress);
        msg.put("progressTotal", progressTotal);
        String json = GSON.toJson(msg);
        if (session == null || !session.isOpen()) {
            buffers.computeIfAbsent(taskId, k -> new ArrayList<>()).add(json);
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            LOG.warn("WebSocket push failed for taskId={}: {}", taskId, e.getMessage());
            sessions.remove(taskId);
        }
    }

    /** Push final status + results. */
    public void pushResult(String taskId, String status, String durationFmt,
                           String errorMessage, int progress, int progressTotal) {
        WebSocketSession session = sessions.get(taskId);
        Map<String, Object> msg = new java.util.LinkedHashMap<>();
        msg.put("type", "result");
        msg.put("status", status);
        msg.put("durationFmt", durationFmt != null ? durationFmt : "");
        msg.put("errorMessage", errorMessage != null ? errorMessage : "");
        msg.put("progress", progress);
        msg.put("progressTotal", progressTotal);
        String json = GSON.toJson(msg);
        if (session == null || !session.isOpen()) {
            buffers.computeIfAbsent(taskId, k -> new ArrayList<>()).add(json);
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            LOG.warn("WebSocket push failed for taskId={}: {}", taskId, e.getMessage());
            sessions.remove(taskId);
        }
    }

    /** Push stopped notification. */
    public void pushStopped(String taskId) {
        WebSocketSession session = sessions.get(taskId);
        Map<String, Object> msg = new java.util.LinkedHashMap<>();
        msg.put("type", "status");
        msg.put("status", "STOPPED");
        String json = GSON.toJson(msg);
        if (session == null || !session.isOpen()) {
            buffers.computeIfAbsent(taskId, k -> new ArrayList<>()).add(json);
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            sessions.remove(taskId);
        }
    }
}
