package actions;

import com.google.gson.*;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import config.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndicatorApiActions {

    private static final Logger log = LoggerFactory.getLogger(IndicatorApiActions.class);
    private final APIRequestContext request;
    private final String projectId;

    private static final String MOE = TestConfig.API_PREFIX_MOE + "/moe";

    public IndicatorApiActions(APIRequestContext request, String projectId) {
        this.request = request;
        this.projectId = projectId;
    }

    // ==================== 逻辑结构 (LogicStructure) ====================

    public String addLogicStructure(String name, String description) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("description", description != null ? description : "");
        body.addProperty("projectId", projectId);
        return post(MOE + "/add/addLogicStructure", body);
    }

    public String searchLogicStructureList() {
        JsonObject body = new JsonObject();
        body.addProperty("projectId", projectId);
        return post(MOE + "/search/searchLogicStructureList", body);
    }

    public String getLogicStructureInfo(String objectId) {
        JsonObject body = new JsonObject();
        body.addProperty("objectId", objectId);
        return post(MOE + "/get/getLogicStructureInfo", body);
    }

    public String deleteLogicStructure(String objectId) {
        JsonObject body = new JsonObject();
        body.addProperty("objectId", objectId);
        body.addProperty("action", "delete");
        return post(MOE + "/delete/deleteLogicStructure", body);
    }

    public String removeLogicStructure(String objectId) {
        JsonObject body = new JsonObject();
        body.addProperty("objectId", objectId);
        return post(MOE + "/remove/removeLogicStructure", body);
    }

    // ==================== 逻辑 (Logic) ====================

    public String addLogic(String objectId, String parentId, String name, String logicStructureId) {
        JsonObject body = new JsonObject();
        body.addProperty("objectId", objectId);
        body.addProperty("parentId", parentId != null ? parentId : "");
        body.addProperty("parentType", "");
        body.addProperty("name", name);
        body.addProperty("type", "system");
        body.addProperty("deviceCode", "暂无设备代号");
        body.addProperty("description", "暂无描述");
        body.addProperty("level", "");
        body.addProperty("logicStructureId", logicStructureId);
        body.addProperty("addMark", true);
        body.addProperty("projectId", projectId);
        return post(MOE + "/add/addLogic", body);
    }

    public String searchLogicList(String objectId) {
        JsonObject body = new JsonObject();
        body.addProperty("objectId", objectId);
        return post(MOE + "/search/searchLogicList", body);
    }

    public String updateLogic(String objectId, String name, String logicStructureId) {
        JsonObject body = new JsonObject();
        body.addProperty("objectId", objectId);
        body.addProperty("name", name);
        body.addProperty("description", "暂无描述");
        body.addProperty("deviceCode", "暂无设备代号");
        body.addProperty("logicStructureId", logicStructureId);
        return post(MOE + "/update/updateLogic", body);
    }

    public String deleteLogic(String objectId, String logicStructureId) {
        JsonObject body = new JsonObject();
        body.addProperty("objectId", objectId);
        body.addProperty("logicStructureId", logicStructureId);
        return post(MOE + "/delete/deleteLogic", body);
    }

    // ==================== 指标参数 (LogicStructureParameter) ====================

    public String addParameter(String objectId, String parentId, String name,
                                String description, String logicStructureId) {
        JsonObject body = new JsonObject();
        body.addProperty("objectId", objectId);
        body.addProperty("parentId", parentId);
        body.addProperty("name", name != null ? name : "未命名指标");
        body.addProperty("description", description != null ? description : "");
        body.addProperty("parameterUnit", "");
        body.addProperty("indexValue", "");
        JsonObject constraints = new JsonObject();
        constraints.addProperty("type", "文本");
        JsonArray value = new JsonArray();
        JsonObject rule = new JsonObject();
        rule.addProperty("rule", "norule");
        value.add(rule);
        constraints.add("value", value);
        body.add("constraints", constraints);
        body.addProperty("logicStructureId", logicStructureId);
        body.addProperty("type", "index");
        body.addProperty("addMark", true);
        return post(MOE + "/add/addLogicStructureParameter", body);
    }

    public String searchParameterList(String objectId) {
        JsonObject body = new JsonObject();
        body.addProperty("objectId", objectId);
        return post(MOE + "/search/searchLogicStructureParameterList", body);
    }

    public String searchParameterValueList(String objectId) {
        JsonObject body = new JsonObject();
        body.addProperty("objectId", objectId);
        return post(MOE + "/search/searchLogicStructureParameterValueList", body);
    }

    public String updateParameter(String objectId, String name, String indexValue, String logicStructureId) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name != null ? name : "未命名指标");
        body.addProperty("parameterUnit", "");
        body.addProperty("indexValue", indexValue != null ? indexValue : "");
        JsonObject constraints = new JsonObject();
        constraints.addProperty("type", "文本");
        JsonArray value = new JsonArray();
        JsonObject rule = new JsonObject();
        rule.addProperty("rule", "norule");
        value.add(rule);
        constraints.add("value", value);
        body.add("constraints", constraints);
        body.addProperty("objectId", objectId);
        body.addProperty("description", "暂无描述");
        body.addProperty("logicStructureId", logicStructureId);
        return post(MOE + "/update/updateLogicStructureParameter", body);
    }

    // ==================== 物理方案 (PhysicalScheme) ====================

    public String addPhysicalScheme(String logicStructureId, String name) {
        JsonObject body = new JsonObject();
        body.addProperty("logicStructureId", logicStructureId);
        body.addProperty("name", name != null ? name : "未命名方案");
        return post(MOE + "/add/addAPhysicalScheme", body);
    }

    public String searchPhysicalSchemeList(String objectId) {
        JsonObject body = new JsonObject();
        body.addProperty("objectId", objectId);
        return post(MOE + "/search/searchPhysicalSchemeList", body);
    }

    // ==================== Helpers ====================

    private String post(String url, JsonObject body) {
        long t0 = System.currentTimeMillis();
        APIResponse resp = request.post(url,
                RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setHeader("ProjectId", projectId)
                        .setData(body.toString()));
        String text = resp.text();
        long ms = System.currentTimeMillis() - t0;
        int code = resp.status();
        if (code >= 400 || text.contains("\"code\":500")) {
            log.warn("API POST {} → HTTP {} ({}ms) body: {}", url, code, ms, text.length() > 200 ? text.substring(0, 200) + "..." : text);
        } else {
            log.info("API POST {} → HTTP {} ({}ms)", url, code, ms);
        }
        return text;
    }

    public String extractId(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.get("code").getAsInt() != 200) return null;
            JsonObject data = root.getAsJsonObject("data");
            if (data.has("id")) return data.get("id").getAsString();
            if (data.has("objectId")) return data.get("objectId").getAsString();
        } catch (Exception e) {
            log.warn("extractId failed: {}", e.getMessage());
        }
        return null;
    }

    public boolean isOk(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject().get("code").getAsInt() == 200;
        } catch (Exception e) { return false; }
    }
}
