package org.example.testvue.dto;

import java.util.*;

/** All API DTOs in one file to minimize file count. */

public class Dtos {

    // ── Request ──
    public static class TestRunRequest {
        public String testClass;
        public String module;
        public String url;
        public String projectId;
        public String username;
        public String password;
    }

    public static class RerunRequest {
        public String taskId;
    }

    public static class ConfigSaveRequest {
        public String configName;
        public String url;
        public String projectId;
        public String username;
        public String password;
    }

    // ── Response ──
    public static class ApiResponse {
        public int code = 200;
        public String msg = "ok";
        public String taskId;
        public String label;
        public Long id;
        public List<String> rerunClassNames;
        public int rerunClassCount;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", code);
            m.put("msg", msg);
            if (taskId != null) m.put("taskId", taskId);
            if (label != null) m.put("label", label);
            if (id != null) m.put("id", id);
            if (rerunClassNames != null) m.put("rerunClassNames", rerunClassNames);
            if (rerunClassCount > 0) m.put("rerunClassCount", rerunClassCount);
            return m;
        }
    }

    public static class StatusResponse {
        public String taskId;
        public String status;
        public String msg;
        public String label;
        public long durationMs;
        public String durationFmt;
        public int progress;
        public int progressTotal;
        public String errorMessage;
        public String output;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            if (taskId != null) m.put("taskId", taskId);
            m.put("status", status);
            m.put("msg", msg != null ? msg : "");
            m.put("label", label != null ? label : "");
            m.put("durationMs", durationMs);
            m.put("durationFmt", durationFmt != null ? durationFmt : "");
            m.put("progress", progress);
            m.put("progressTotal", progressTotal);
            m.put("errorMessage", errorMessage != null ? errorMessage : "");
            m.put("output", output != null ? output : "");
            return m;
        }
    }

    public static class HistoryItem {
        public String taskId;
        public String label;
        public String status;
        public String createTime;
        public String durationFmt;
        public int passed;
        public int failed;
        public int skipped;
    }

    public static class FailedCase {
        public String className;
        public String methodName;
        public String reason;
        public String lastFailTime;
    }

    public static class ConfigItem {
        public Long id;
        public String configName;
        public String url;
        public String projectId;
        public String username;
    }

    public static class TestCaseResult {
        public String name;
        public String time;
        public String status;   // PASS | FAIL
        public String reason;
    }

    public static class ClassResult {
        public String className;
        public int tests;
        public int failures;
        public int errors;
        public int skipped;
        public String time;
        public List<TestCaseResult> cases;
    }
}
