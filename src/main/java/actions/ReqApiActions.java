package actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import config.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReqApiActions {

    private static final Logger log = LoggerFactory.getLogger(ReqApiActions.class);
    private final APIRequestContext request;

    // API endpoints
    private static final String ERM_ADD_FOLDER = "/erm/add/addReqSpeFolder";
    private static final String ERM_ADD_DOC = "/erm/add/addReqSpe";
    private static final String ERM_UPDATE_FOLDER = "/erm/update/updateReqSpeFolderInfo";
    private static final String ERM_UPDATE_DOC = "/erm/update/updateReqSpeInfo";
    private static final String ERM_DEL_FOLDER = "/erm/del/delReqSpeFolder";
    private static final String ERM_DEL_DOC = "/erm/del/delReqSpe";
    private static final String ERM_RECOVER_FOLDER = "/erm/recover/recoverReqSpeFolder";
    private static final String ERM_RECOVER_DOC = "/erm/recover/recoverReqSpe";
    private static final String ERM_CLEAN_FOLDER = "/erm/clean/cleanReqSpeFolder";
    private static final String ERM_CLEAN_DOC = "/erm/clean/cleanReqSpe";
    private static final String ERM_SEARCH_TREE = "/erm/search/searchReqFolderStructureTree";
    private static final String ERM_SEARCH_LIST = "/erm/search/searchReqSpeListFromProject";
    private static final String ERM_ATTR_ADD = "/erm/customAttribute/addCustomAttribute";
    private static final String ERM_ATTR_SELECT = "/erm/customAttribute/selectCustomAttributeList";
    private static final String ERM_ATTR_UPDATE = "/erm/customAttribute/updateCustomAttribute";
    private static final String ERM_SEARCH_PROJECT = "/common/search/searchProjectByUser";
    private static final String ERM_IMPORT_EXCEL = "/erm/import/importReqSpecification";
    private static final String ERM_ATTR_DELETE = "/erm/customAttribute/deleteCustomAttributes";
    private static final String ERM_ATTR_PUBLISH = "/erm/customAttribute/publishCustomAttribute";
    private static final String ERM_SEARCH_USER = "/common/search/searchUserByUser";

    // Req Item CRUD
    private static final String ERM_ADD_REQ = "/erm/add/addReq";
    private static final String ERM_UPDATE_REQ_LIST = "/erm/update/updateReqList";
    private static final String ERM_DEL_REQ_OBJECT = "/erm/del/delReqObjectList";
    private static final String ERM_CLEAN_REQ = "/erm/clean/cleanReq";
    private static final String ERM_RECOVER_REQ = "/erm/recover/recoverReq";
    private static final String ERM_SEARCH_CHILD_REQ = "/erm/search/searchChildReqInfoByReqSpeId";

    // View management
    private static final String ERM_ADD_VIEW = "/erm/add/addReqSpeView";
    private static final String ERM_DEL_VIEW = "/erm/del/delReqSpeView";
    private static final String ERM_SEARCH_VIEW_LIST = "/erm/search/searchReqSpeViewList";

    // Export
    private static final String ERM_EXPORT_EXCEL = "/erm/exportExcelReqSpecification";
    private static final String ERM_EXPORT_WORD = "/erm/exportWordReqSpecification";
    private static final String ERM_EXPORT_REQIF = "/erm/reqIf/post/exportReqIf";
    private static final String ERM_GET_ATOZ_PARAM = "/erm/reqIf/get/getAllAtozParam";

    // Import
    private static final String ERM_DOWNLOAD_TEMPLATE = "/erm/downloadReqImportTemplate";
    private static final String ERM_GET_IMPORT_ATTRS = "/erm/import/getAttributes";

    // Other
    private static final String ERM_UNLOCK = "/erm/unlockModeForCloseWindow";
    private static final String ERM_GET_ACCESS = "/erm/get/getReqAccess";
    private static final String ERM_SEARCH_FOLDER_CHILDREN = "/erm/search/searchReqFolderChildrenList";
    private static final String ERM_GET_VERSION_LIST = "/erm/search/getReqSpeVersionList";
    private static final String ERM_GET_TEMPLATE_NAMES = "/erm/attr/get/getTemplateNames";
    private static final String ERM_CHECK_OPEN_MODE = "/erm/get/checkOpenMode";
    private static final String ERM_SEARCH_ATTRS = "/erm/customAttribute/searchAttributes";
    private static final String ERM_SEARCH_TRACE = "/erm/search/searchReqSpecificationTrace";
    private static final String ERM_CHANGE_ANALYSIS = "/erm/aiAnalysis/search/searchChangeAnalysisResultList";
    private static final String ERM_INSERT_TEMPLATE = "/erm/attr/post/insertTemplate";

    private static final String PARENT_TYPE_FOLDER = "reqSpeFolder";
    private static final String PARENT_TYPE_PROJECT = "project";

    public ReqApiActions(APIRequestContext request) {
        this.request = request;
    }

    public String getProjectIdByName(String projectName) {
        APIResponse response = request.get(TestConfig.API_PREFIX + ERM_SEARCH_PROJECT);
        String resp = response.text();

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        JsonArray data = root.getAsJsonArray("data");

        if (data == null) {
            throw new RuntimeException("接口返回 data 为 null，响应: " + resp);
        }

        for (JsonElement el : data) {
            JsonObject project = el.getAsJsonObject();
            String name = project.get("projectName").getAsString();
            if (projectName.equals(name)) {
                String projectId = project.get("projectId").getAsString();
                log.info("找到项目 [{}] -> projectId: {}", projectName, projectId);
                return projectId;
            }
        }

        throw new RuntimeException("未找到项目: " + projectName);
    }

    public String createFolder(String projectId, String parentId) {
        String payload = """
                {
                    "parentId": "%s",
                    "parentType": "%s",
                    "projectId": "%s"
                }
                """.formatted(parentId, PARENT_TYPE_FOLDER, projectId);

        String resp = post(ERM_ADD_FOLDER, payload);
        return extractField(resp, "objectId");
    }

    public String createDocument(String projectId, String parentId) {
        String payload = """
                {
                    "parentId": "%s",
                    "parentType": "%s",
                    "projectId": "%s"
                }
                """.formatted(parentId, PARENT_TYPE_FOLDER, projectId);

        String resp = post(ERM_ADD_DOC, payload);
        return extractField(resp, "objectId");
    }

    public String renameFolder(String projectId, String objectId, String parentId, String newTitle) {
        String payload = """
                {
                    "projectId": "%s",
                    "objectId": "%s",
                    "parentId": "%s",
                    "parentType": "%s",
                    "title": "%s"
                }
                """.formatted(projectId, objectId, parentId, PARENT_TYPE_FOLDER, newTitle);

        return post(ERM_UPDATE_FOLDER, payload);
    }

    public String renameDocument(String projectId, String objectId, String parentId, String newTitle) {
        String payload = """
                {
                    "projectId": "%s",
                    "objectId": "%s",
                    "parentId": "%s",
                    "parentType": "%s",
                    "title": "%s"
                }
                """.formatted(projectId, objectId, parentId, PARENT_TYPE_FOLDER, newTitle);

        return post(ERM_UPDATE_DOC, payload);
    }

    public String editDescription(String projectId, String objectId, String parentId, String description) {
        String payload = """
                {
                    "projectId": "%s",
                    "objectId": "%s",
                    "parentId": "%s",
                    "parentType": "%s",
                    "description": "%s"
                }
                """.formatted(projectId, objectId, parentId, PARENT_TYPE_FOLDER, description);

        return post(ERM_UPDATE_FOLDER, payload);
    }

    public String deleteFolder(String objectId, String parentId, String parentType) {
        String payload = """
                {
                    "objectId": "%s",
                    "parentId": "%s",
                    "parentType": "%s"
                }
                """.formatted(objectId, parentId, parentType);

        return post(ERM_DEL_FOLDER, payload);
    }

    public String recoverFolder(String objectId, String parentId) {
        String payload = """
                {
                    "objectId": "%s",
                    "parentId": "%s"
                }
                """.formatted(objectId, parentId);

        return post(ERM_RECOVER_FOLDER, payload);
    }
    public String deleteDocument(String objectId, String parentId) {
        String payload = """
                {
                    "objectId": "%s",
                    "parentId": "%s",
                    "parentType": "%s"
                }
                """.formatted(objectId, parentId, PARENT_TYPE_FOLDER);

        return post(ERM_DEL_DOC, payload);
    }

    public String recoverDocument(String objectId, String parentId) {
        String payload = """
                {
                    "objectId": "%s",
                    "parentId": "%s",
                    "parentType": "%s"
                }
                """.formatted(objectId, parentId, PARENT_TYPE_FOLDER);

        return post(ERM_RECOVER_DOC, payload);
    }

    public String cleanDocument(String objectId, String parentId) {
        String payload = """
                {
                    "objectId": "%s",
                    "parentId": "%s",
                    "parentType": "%s"
                }
                """.formatted(objectId, parentId, PARENT_TYPE_FOLDER);

        return post(ERM_CLEAN_DOC, payload);
    }

    public String getTree(String projectId, String parentId) {
        String payload = """
                {
                    "projectId": "%s",
                    "parentId": "%s",
                    "parentType": "%s"
                }
                """.formatted(projectId, parentId, PARENT_TYPE_PROJECT);

        return post(ERM_SEARCH_TREE, payload);
    }

    public String findNodeIdByTitle(String projectId, String targetTitle) {
        try {
            String resp = getTree(projectId, projectId);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            if (!root.has("data") || root.get("data").isJsonNull()) {
                log.warn("findNodeIdByTitle: API 返回 data 为 null");
                return null;
            }
            JsonArray dataList = root.getAsJsonArray("data");
            for (JsonElement el : dataList) {
                String found = deepFind(el.getAsJsonObject(), targetTitle);
                if (found != null) return found;
            }
        } catch (Exception e) {
            log.warn("findNodeIdByTitle 接口返回异常（可能未登录）: {}", e.getMessage());
        }
        return null;
    }

    public void cleanFolderByName(String projectId, String targetName) {
        log.info("====== 开始清理环境 ======");

        String resp = getTree(projectId, projectId);
        JsonObject root = null;

        // 👇 给解析 JSON 穿上防弹衣 👇
        try {
            root = JsonParser.parseString(resp).getAsJsonObject();
        } catch (Exception e) {
            log.error("❌ 清理失败：获取目录树的接口返回了非 JSON 数据！");
            log.error("❌ 接口返回的真实内容是: \n{}", resp);
            return; // 遇到接口报错直接退出清理，不让程序崩溃，去跑后面的 UI 测试
        }
        // 👆 防弹衣结束 👆

        // 防止 data 为空导致后面的代码空指针
        if (!root.has("data") || root.get("data").isJsonNull()) {
            log.warn("获取到的树结构中没有 data 字段，无需清理");
            return;
        }

        JsonArray dataList = root.getAsJsonArray("data");

        JsonObject targetNode = null;
        for (JsonElement el : dataList) {
            JsonObject node = el.getAsJsonObject();
            if (targetName.equals(node.get("title").getAsString())) {
                targetNode = node;
                break;
            }
        }

        if (targetNode == null) {
            log.info("未找到 [{}]，无需清理", targetName);
            return;
        }

        String folderId = targetNode.get("objectId").getAsString();
        log.info("锁定目标文件夹 ID: {}", folderId);

        if (targetNode.has("children") && !targetNode.get("children").isJsonNull()) {
            JsonArray children = targetNode.getAsJsonArray("children");
            for (JsonElement childEl : children) {
                JsonObject child = childEl.getAsJsonObject();
                String childId = child.get("objectId").getAsString();
                String childTitle = child.get("title").getAsString();
                String childType = child.get("type").getAsString();

                if (PARENT_TYPE_FOLDER.equals(childType)) {
                    deleteFolder(childId, folderId, PARENT_TYPE_FOLDER);
                    forceCleanFolder(childId);
                    log.info("  已清理子文件夹: {}", childTitle);
                } else if ("reqSpe".equals(childType)) {
                    deleteDocument(childId, folderId);
                    forceCleanDocument(childId, folderId);
                    log.info("  已清理需求规格: {}", childTitle);
                }
            }
        }

        deleteFolder(folderId, projectId, PARENT_TYPE_PROJECT);
        forceCleanFolder(folderId);

        log.info("\n====== 清理环境结束 ======");
    }

    public void forceCleanFolder(String objectId) {
        String recoverPayload = """
                {"objectId": "%s", "parentId": "%s"}
                """.formatted(objectId, objectId);
        String cleanPayload = """
                {"objectId": "%s"}
                """.formatted(objectId);

        // 改为调用统一的 post 方法，这样就能自动带上 Token 了
        post(ERM_RECOVER_FOLDER, recoverPayload);
        post(ERM_CLEAN_FOLDER, cleanPayload);
    }

    public void forceCleanDocument(String objectId, String parentId) {
        String payload = """
                {
                    "objectId": "%s",
                    "parentId": "%s",
                    "parentType": "%s"
                }
                """.formatted(objectId, parentId, PARENT_TYPE_FOLDER);

        // 同理，改为调用统一的 post 方法
        post(ERM_RECOVER_DOC, payload);
        post(ERM_CLEAN_DOC, payload);
    }

    public String getReqSpeList(String projectId) {
        String payload = """
                {
                    "projectId": "%s"
                }
                """.formatted(projectId);

        return post(ERM_SEARCH_LIST, payload);
    }

    public String addCustomAttribute(String nameEn, String name, String type, String projectId) {
        String payload = """
                {
                    "nameEn": "%s",
                    "name": "%s",
                    "type": "%s",
                    "current": "1",
                    "valueRange": "",
                    "defaultValue": "",
                    "isMultiple": false,
                    "description": "自动化测试创建",
                    "businessDomain": "需求管理",
                    "objectType": "req",
                    "id": "",
                    "createTime": "",
                    "creator": "",
                    "modifier": "",
                    "projectId": "%s",
                    "usedColor": "#1e90ff",
                    "isUseDefaultValue": true,
                    "valueRangeMapping": []
                }
                """.formatted(nameEn, name, type, projectId);

        return post(ERM_ATTR_ADD, payload);
    }

    public String[] findCustomAttribute(String nameEn, String projectId) {
        String resp = request.get(
                TestConfig.API_PREFIX + ERM_ATTR_SELECT,
                RequestOptions.create()
                        .setQueryParam("projectId", projectId)
                        .setQueryParam("businessDomain", "需求管理")
                        .setQueryParam("objectType", "req")
                        .setQueryParam("name", "")
                        .setQueryParam("type", "")
                        .setQueryParam("current", "")
        ).text();

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        JsonArray data = root.getAsJsonArray("data");

        for (JsonElement el : data) {
            JsonObject obj = el.getAsJsonObject();
            if (nameEn.equals(obj.get("nameEn").getAsString())) {
                return new String[]{
                        obj.get("id").getAsString(),
                        obj.get("createTime").getAsString(),
                        obj.get("creator").getAsString()
                };
            }
        }
        return null;
    }

    public String updateCustomAttribute(String id, String nameEn, String name, String type,
                                        String createTime, String creator, String projectId) {
        String payload = """
                {
                    "nameEn": "%s",
                    "name": "%s",
                    "type": "%s",
                    "current": "1",
                    "valueRange": "",
                    "defaultValue": "",
                    "isMultiple": false,
                    "description": "update text",
                    "businessDomain": "需求管理",
                    "objectType": "req",
                    "id": "%s",
                    "createTime": "%s",
                    "creator": "%s",
                    "modifier": "admin",
                    "projectId": "%s",
                    "usedColor": "#1e90ff",
                    "isUseDefaultValue": true,
                    "valueRangeMapping": []
                }
                """.formatted(nameEn, name, type, id, createTime, creator, projectId);

        return post(ERM_ATTR_UPDATE, payload);
    }

    public String addReqItem(String projectId, String parentId, String parentReqSpeId) {
        String payload = """
                {
                    "parentId": "%s",
                    "projectId": "%s",
                    "parentReqSpeId": "%s",
                    "beforeLinkOrderNo": ""
                }
                """.formatted(parentId, projectId, parentReqSpeId);
        String resp = post(ERM_ADD_REQ, payload);
        return extractField(resp, "objectId");
    }

    public String addReqItemRaw(String projectId, String parentId, String parentReqSpeId) {
        String payload = """
                {
                    "parentId": "%s",
                    "projectId": "%s",
                    "parentReqSpeId": "%s",
                    "beforeLinkOrderNo": ""
                }
                """.formatted(parentId, projectId, parentReqSpeId);
        return post(ERM_ADD_REQ, payload);
    }

    public String updateReqList(String reqSpeId, String reqListJson) {
        String payload = """
                {
                    "reqSpeId": "%s",
                    "reqList": %s
                }
                """.formatted(reqSpeId, reqListJson);
        return post(ERM_UPDATE_REQ_LIST, payload);
    }

    public String deleteReqItem(String objectId) {
        String payload = """
                {"objectId": "%s"}
                """.formatted(objectId);
        return post(ERM_DEL_REQ_OBJECT, payload);
    }

    public String cleanReqItem(String objectId, String reqSpecId) {
        String payload = """
                {
                    "objectId": "%s",
                    "reqSpecId": "%s"
                }
                """.formatted(objectId, reqSpecId);
        return post(ERM_CLEAN_REQ, payload);
    }

    public String recoverReqItem(String objectId) {
        String payload = """
                {"objectId": "%s"}
                """.formatted(objectId);
        return post(ERM_RECOVER_REQ, payload);
    }

    public String searchChildReqInfo(String reqSpeId) {
        String payload = """
                {"objectId": "%s"}
                """.formatted(reqSpeId);
        return post(ERM_SEARCH_CHILD_REQ, payload);
    }

    // ========== View management ==========

    public String addView(String objectId, String name, String description, String viewHeaderValues) {
        String payload = """
                {
                    "objectId": "%s",
                    "viewHeaderValues": "%s",
                    "name": "%s",
                    "description": "%s"
                }
                """.formatted(objectId, viewHeaderValues, name, description);
        return post(ERM_ADD_VIEW, payload);
    }

    public String deleteView(String objectId) {
        String payload = """
                {"objectId": "%s"}
                """.formatted(objectId);
        return post(ERM_DEL_VIEW, payload);
    }

    public String searchViewList(String objectId) {
        String payload = """
                {"objectId": "%s"}
                """.formatted(objectId);
        return post(ERM_SEARCH_VIEW_LIST, payload);
    }

    // ========== Export ==========

    public APIResponse exportExcel(String objectId, String templateType) {
        return request.get(TestConfig.API_PREFIX + ERM_EXPORT_EXCEL,
                RequestOptions.create()
                        .setQueryParam("objectId", objectId)
                        .setQueryParam("templateType", templateType));
    }

    public APIResponse exportWord(String objectId, String templateType) {
        return request.get(TestConfig.API_PREFIX + ERM_EXPORT_WORD,
                RequestOptions.create()
                        .setQueryParam("objectId", objectId)
                        .setQueryParam("templateType", templateType));
    }

    public String getAllAtozParam(String projectId) {
        return get(ERM_GET_ATOZ_PARAM + "?projectId=" + projectId
                + "&businessDomain=需求管理&objectType=req");
    }

    public String exportReqIf(String payload) {
        return post(ERM_EXPORT_REQIF, payload);
    }

    // ========== Import ==========

    public APIResponse downloadImportTemplate(String templateType) {
        return request.get(TestConfig.API_PREFIX + ERM_DOWNLOAD_TEMPLATE,
                RequestOptions.create().setQueryParam("templateType", templateType));
    }

    public String getImportAttributes() {
        return get(ERM_GET_IMPORT_ATTRS);
    }

    // ========== Other ==========

    public String unlockMode(String objectId, String unlockMode, String person) {
        String payload = """
                {
                    "objectId": "%s",
                    "unlockMode": "%s",
                    "unlockModePerson": "%s"
                }
                """.formatted(objectId, unlockMode, person);
        return post(ERM_UNLOCK, payload);
    }

    public String getReqAccess(String objectId) {
        return get(ERM_GET_ACCESS + "?objectId=" + objectId);
    }

    public String searchFolderChildren(String objectId) {
        String payload = """
                {"objectId": "%s"}
                """.formatted(objectId);
        return post(ERM_SEARCH_FOLDER_CHILDREN, payload);
    }

    public String getVersionList(String objectId) {
        return get(ERM_GET_VERSION_LIST + "?objectId=" + objectId);
    }

    public String getTemplateNames(String projectId) {
        return get(ERM_GET_TEMPLATE_NAMES + "?projectId=" + projectId);
    }

    public String importReqSpecification(String projectId, String parentId, String reqSpecName, String dataJson) {
        String payload = """
                {
                    "projectId": "%s",
                    "reqSpeParentId": "%s",
                    "reqSpeName": "%s",
                    "dataJson": %s
                }
                """.formatted(projectId, parentId, reqSpecName, dataJson);
        return post(ERM_IMPORT_EXCEL, payload);
    }

    public String deleteCustomAttribute(String id) {
        String payload = """
                {"ids": ["%s"]}
                """.formatted(id);
        return post(ERM_ATTR_DELETE, payload);
    }

    public String getCustomAttributeList(String projectId) {
        APIResponse response = request.get(
                TestConfig.API_PREFIX + ERM_ATTR_SELECT,
                RequestOptions.create()
                        .setQueryParam("projectId", projectId)
                        .setQueryParam("businessDomain", "")
                        .setQueryParam("objectType", "")
                        .setQueryParam("name", "")
                        .setQueryParam("type", "")
                        .setQueryParam("current", "")
        );
        return response.text();
    }

    public boolean isDataEmpty(String resp) {
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        JsonArray data = root.getAsJsonArray("data");
        return data == null || data.size() == 0;
    }

    public String searchCustomAttribute(String projectId, String businessDomain, String objectType,
                                        String name, String type, String current) {
        APIResponse response = request.get(
                TestConfig.API_PREFIX + ERM_ATTR_SELECT,
                RequestOptions.create()
                        .setQueryParam("projectId", projectId)
                        .setQueryParam("businessDomain", businessDomain != null ? businessDomain : "")
                        .setQueryParam("objectType", objectType != null ? objectType : "")
                        .setQueryParam("name", name != null ? name : "")
                        .setQueryParam("type", type != null ? type : "")
                        .setQueryParam("current", current != null ? current : "")
        );
        return response.text();
    }

    public String publishCustomAttribute(String id, String projectId) {
        String payload = """
                {
                    "id": "%s",
                    "projectId": "%s"
                }
                """.formatted(id, projectId);
        return post(ERM_ATTR_PUBLISH, payload);
    }

    public String batchDeleteCustomAttributes(String... ids) {
        StringBuilder jsonIds = new StringBuilder("[");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) jsonIds.append(",");
            jsonIds.append("\"").append(ids[i]).append("\"");
        }
        jsonIds.append("]");
        String payload = "{\"ids\": " + jsonIds.toString() + "}";
        return post(ERM_ATTR_DELETE, payload);
    }

    public String searchUser(String keyword) {
        String payload = """
                {"userName": "%s"}
                """.formatted(keyword);
        APIResponse response = request.post(
                TestConfig.API_PREFIX + ERM_SEARCH_USER,
                RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData(payload)
        );
        return response.text();
    }

    // ========== New HAR-discovered endpoints ==========

    public String checkOpenMode(String masterId, String operateType, String openPerson) {
        return get(ERM_CHECK_OPEN_MODE + "?masterId=" + masterId
                + "&operateType=" + operateType + "&openPerson=" + openPerson);
    }

    public String searchAttributes(String projectId, String businessDomain, String objectType) {
        APIResponse response = request.get(
                TestConfig.API_PREFIX + ERM_SEARCH_ATTRS,
                RequestOptions.create()
                        .setQueryParam("projectId", projectId)
                        .setQueryParam("businessDomain", businessDomain)
                        .setQueryParam("objectType", objectType)
        );
        return response.text();
    }

    public String searchReqSpecTrace(String objectId, String type) {
        return get(ERM_SEARCH_TRACE + "?objectId=" + objectId + "&type=" + type);
    }

    public String searchChangeAnalysis(String masterId, String version) {
        return get(ERM_CHANGE_ANALYSIS + "?masterId=" + masterId + "&version=" + version);
    }

    public String insertTemplate(String templateName, String projectId,
                                 String templateDescribe, String attrListJson) {
        String payload = """
                {
                    "templateName": "%s",
                    "projectId": "%s",
                    "templateDescribe": "%s",
                    "attrTemplateInfoRspVoList": %s
                }
                """.formatted(templateName, projectId, templateDescribe, attrListJson);
        return post(ERM_INSERT_TEMPLATE, payload);
    }

    // ========== Generic search helpers ==========

    public String searchProjectByUser() {
        return get(ERM_SEARCH_PROJECT);
    }

    private String get(String path) {
        APIResponse response = request.get(TestConfig.API_PREFIX + path);
        return response.text();
    }

    private String post(String endpoint, String payload) {
        APIResponse response = request.post(
                TestConfig.API_PREFIX + endpoint,
                RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData(payload)
        );
        return response.text();
    }

    private String extractField(String json, String fieldName) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject data = root.getAsJsonObject("data");
            return data.get(fieldName).getAsString();
        } catch (Exception e) {
            throw new RuntimeException("解析字段 [" + fieldName + "] 失败: " + json, e);
        }
    }

    private String deepFind(JsonObject node, String targetTitle) {
        if (targetTitle.equals(node.get("title").getAsString())) {
            return node.get("objectId").getAsString();
        }
        if (node.has("children") && !node.get("children").isJsonNull()) {
            for (JsonElement child : node.getAsJsonArray("children")) {
                String found = deepFind(child.getAsJsonObject(), targetTitle);
                if (found != null) return found;
            }
        }
        return null;
    }
}