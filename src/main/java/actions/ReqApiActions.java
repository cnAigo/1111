package actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import com.microsoft.playwright.options.FormData;
import config.TestConfig;
import java.nio.file.Paths;
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
    private static final String ERM_CHECK_OPEN_MODE = "/erm/get/checkOpenMode";
    private static final String ERM_SEARCH_ATTRS = "/erm/customAttribute/searchAttributes";
    private static final String ERM_SEARCH_TRACE = "/erm/search/searchReqSpecificationTrace";
    private static final String ERM_CHANGE_ANALYSIS = "/erm/aiAnalysis/search/searchChangeAnalysisResultList";

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
        String parentType = parentId.equals(projectId) ? PARENT_TYPE_PROJECT : PARENT_TYPE_FOLDER;
        String payload = """
                {
                    "parentId": "%s",
                    "parentType": "%s",
                    "projectId": "%s"
                }
                """.formatted(parentId, parentType, projectId);

        String resp = post(ERM_ADD_FOLDER, payload);
        return extractField(resp, "objectId");
    }

    public String createDocument(String projectId, String parentId) {
        String parentType = parentId.equals(projectId) ? PARENT_TYPE_PROJECT : PARENT_TYPE_FOLDER;
        String payload = """
                {
                    "parentId": "%s",
                    "parentType": "%s",
                    "projectId": "%s"
                }
                """.formatted(parentId, parentType, projectId);

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

        return post(ERM_UPDATE_DOC, payload);
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

    /**
     * Recursively delete and force-clean ALL folders and documents under the project root.
     * DANGER: This wipes everything. Use with caution.
     */
    public void cleanAllUnderRoot(String projectId) {
        log.info("====== 开始清理根节点下所有内容 ======");

        String resp = getTree(projectId, projectId);
        JsonObject root;
        try {
            root = JsonParser.parseString(resp).getAsJsonObject();
        } catch (Exception e) {
            log.error("获取目录树失败，返回非JSON数据: {}", resp);
            return;
        }

        if (!root.has("data") || root.get("data").isJsonNull()) {
            log.info("根节点下无数据，无需清理");
            return;
        }

        JsonArray dataList = root.getAsJsonArray("data");
        for (JsonElement el : dataList) {
            JsonObject node = el.getAsJsonObject();
            String nodeType = node.get("type").getAsString();
            String nodeId = node.get("objectId").getAsString();
            String nodeTitle = node.has("title") ? node.get("title").getAsString() : "(无标题)";

            if (PARENT_TYPE_FOLDER.equals(nodeType)) {
                // Delete children first, then the folder itself
                deleteChildrenRecursive(nodeId, node);
                deleteFolder(nodeId, projectId, PARENT_TYPE_PROJECT);
                forceCleanFolder(nodeId);
                log.info("已清理根下文件夹: {}", nodeTitle);
            } else if ("reqSpe".equals(nodeType)) {
                deleteDocument(nodeId, projectId);
                forceCleanDocument(nodeId, projectId);
                log.info("已清理根下需求规格: {}", nodeTitle);
            }
        }

        log.info("====== 根节点清理完成 ======");
    }

    /**
     * Sweep and delete all AT_-prefixed folders/docs under project root.
     * Safe: only removes automated-test items, leaves real user data untouched.
     */
    public int sweepATFolders(String projectId) {
        int removed = 0;
        try {
            String resp = getTree(projectId, projectId);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            if (!root.has("data") || root.get("data").isJsonNull()) return 0;

            JsonArray dataList = root.getAsJsonArray("data");
            for (JsonElement el : dataList) {
                JsonObject node = el.getAsJsonObject();
                String title = node.has("title") ? node.get("title").getAsString() : "";
                if (!title.startsWith("AT_")) continue;

                String nodeId = node.get("objectId").getAsString();
                String nodeType = node.get("type").getAsString();

                if (PARENT_TYPE_FOLDER.equals(nodeType)) {
                    deleteChildrenRecursive(nodeId, node);
                    deleteFolder(nodeId, projectId, PARENT_TYPE_PROJECT);
                    forceCleanFolder(nodeId);
                } else if ("reqSpe".equals(nodeType)) {
                    deleteDocument(nodeId, projectId);
                    forceCleanDocument(nodeId, projectId);
                }
                removed++;
                log.info("  Swept: {} ({})", title, nodeId);
                try { Thread.sleep(250); } catch (InterruptedException ignored) {}
            }
        } catch (Exception e) {
            log.warn("sweepATFolders failed: {}", e.getMessage());
        }
        if (removed > 0) log.info("Sweep done: {} test items removed", removed);
        return removed;
    }

    private void deleteChildrenRecursive(String parentFolderId, JsonObject folderNode) {
        if (!folderNode.has("children") || folderNode.get("children").isJsonNull()) {
            return;
        }

        JsonArray children = folderNode.getAsJsonArray("children");
        for (JsonElement childEl : children) {
            JsonObject child = childEl.getAsJsonObject();
            String childId = child.get("objectId").getAsString();
            String childType = child.get("type").getAsString();
            String childTitle = child.has("title") ? child.get("title").getAsString() : "(无标题)";

            if (PARENT_TYPE_FOLDER.equals(childType)) {
                deleteChildrenRecursive(childId, child);
                deleteFolder(childId, parentFolderId, PARENT_TYPE_FOLDER);
                forceCleanFolder(childId);
                log.info("  已清理子文件夹: {}", childTitle);
            } else if ("reqSpe".equals(childType)) {
                deleteDocument(childId, parentFolderId);
                forceCleanDocument(childId, parentFolderId);
                log.info("  已清理需求规格: {}", childTitle);
            }
        }
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
        String resp = post(ERM_ADD_VIEW, payload);
        // Try to extract objectId directly first
        try {
            return extractField(resp, "objectId");
        } catch (Exception ignored) {}
        // Fallback: search for the view by name (data is an array, id field is "id")
        String listResp = searchViewList(objectId);
        try {
            JsonObject root = JsonParser.parseString(listResp).getAsJsonObject();
            JsonElement dataEl = root.get("data");
            if (dataEl == null || dataEl.isJsonNull() || !dataEl.isJsonArray()) return "";
            JsonArray list = dataEl.getAsJsonArray();
            for (JsonElement el : list) {
                JsonObject v = el.getAsJsonObject();
                if (name.equals(v.get("name").getAsString())) {
                    return v.get("id").getAsString();
                }
            }
        } catch (Exception e) {
            log.warn("addView: failed to find view '{}': {}", name, e.getMessage());
        }
        return "";
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

    // ========== Cooperation Area (合作区管理) ==========

    private static final String ERM_COOP_AREA_ADD = "/erm/add/addCooperationArea";
    private static final String ERM_COOP_AREA_UPDATE = "/erm/update/updateCooperationArea";
    private static final String ERM_COOP_AREA_DEL = "/erm/del/delCooperationArea";
    private static final String ERM_COOP_AREA_SEARCH = "/erm/search/searchCooperationAreaList";
    private static final String ERM_COOP_AREA_ADD_USER = "/erm/cooperation/addCooperationAreaUser";
    private static final String ERM_COOP_AREA_DEL_USER = "/erm/cooperation/delCooperationAreaUser";

    public String addCooperationArea(String name, String code, String securityLevel, String description) {
        String payload = """
                {
                    "name": "%s",
                    "code": "%s",
                    "securityLevel": "%s",
                    "description": "%s"
                }
                """.formatted(name, code, securityLevel, description);
        return post(ERM_COOP_AREA_ADD, payload);
    }

    public String updateCooperationArea(String id, String name, String code,
                                        String securityLevel, String description) {
        String payload = """
                {
                    "id": "%s",
                    "name": "%s",
                    "code": "%s",
                    "securityLevel": "%s",
                    "description": "%s"
                }
                """.formatted(id, name, code, securityLevel, description);
        return post(ERM_COOP_AREA_UPDATE, payload);
    }

    public String deleteCooperationArea(String id) {
        String payload = """
                {"id": "%s"}
                """.formatted(id);
        return post(ERM_COOP_AREA_DEL, payload);
    }

    public String searchCooperationAreaList(String name, String code) {
        String payload = """
                {
                    "name": "%s",
                    "code": "%s"
                }
                """.formatted(name != null ? name : "", code != null ? code : "");
        return post(ERM_COOP_AREA_SEARCH, payload);
    }

    public String addCooperationAreaUser(String areaId, String userId) {
        String payload = """
                {
                    "cooperationAreaId": "%s",
                    "userId": "%s"
                }
                """.formatted(areaId, userId);
        return post(ERM_COOP_AREA_ADD_USER, payload);
    }

    public String deleteCooperationAreaUser(String areaId, String userId) {
        String payload = """
                {
                    "cooperationAreaId": "%s",
                    "userId": "%s"
                }
                """.formatted(areaId, userId);
        return post(ERM_COOP_AREA_DEL_USER, payload);
    }

    // ========== User Management (用户管理) ==========

    private static final String ERM_USER_ADD = "/erm/user/addUser";
    private static final String ERM_USER_UPDATE = "/erm/user/updateUser";
    private static final String ERM_USER_DEL = "/erm/user/delUser";
    private static final String ERM_USER_SEARCH_LIST = "/erm/search/searchUserList";
    private static final String ERM_USER_RESET_PWD = "/erm/user/resetPassword";
    private static final String ERM_USER_IMPORT = "/erm/import/importUser";

    public String addUser(String nickname, String userName, String password, String deptId,
                          String phone, String email, String gender, String status,
                          String position, String roleIds, String remark) {
        String payload = """
                {
                    "nickname": "%s",
                    "userName": "%s",
                    "password": "%s",
                    "deptId": "%s",
                    "phone": "%s",
                    "email": "%s",
                    "gender": "%s",
                    "status": "%s",
                    "position": "%s",
                    "roleIds": "%s",
                    "remark": "%s"
                }
                """.formatted(nickname, userName, password, deptId, phone, email,
                gender, status, position, roleIds, remark);
        return post(ERM_USER_ADD, payload);
    }

    public String updateUser(String userId, String nickname, String deptId, String phone,
                             String email, String gender, String status, String position,
                             String roleIds, String remark) {
        String payload = """
                {
                    "userId": "%s",
                    "nickname": "%s",
                    "deptId": "%s",
                    "phone": "%s",
                    "email": "%s",
                    "gender": "%s",
                    "status": "%s",
                    "position": "%s",
                    "roleIds": "%s",
                    "remark": "%s"
                }
                """.formatted(userId, nickname, deptId, phone, email, gender,
                status, position, roleIds, remark);
        return post(ERM_USER_UPDATE, payload);
    }

    public String deleteUser(String userId) {
        String payload = """
                {"userId": "%s"}
                """.formatted(userId);
        return post(ERM_USER_DEL, payload);
    }

    public String searchUserList(String userName, String phone, String status, String deptId) {
        String payload = """
                {
                    "userName": "%s",
                    "phone": "%s",
                    "status": "%s",
                    "deptId": "%s"
                }
                """.formatted(userName != null ? userName : "",
                phone != null ? phone : "",
                status != null ? status : "",
                deptId != null ? deptId : "");
        return post(ERM_USER_SEARCH_LIST, payload);
    }

    public String resetPassword(String userId, String newPassword) {
        String payload = """
                {
                    "userId": "%s",
                    "newPassword": "%s"
                }
                """.formatted(userId, newPassword);
        return post(ERM_USER_RESET_PWD, payload);
    }

    public String importUser(String dataJson) {
        String payload = """
                {"dataJson": %s}
                """.formatted(dataJson);
        return post(ERM_USER_IMPORT, payload);
    }

    // ========== Favorites (收藏) ==========

    private static final String ERM_ADD_FAVORITE = "/erm/add/addFavorite";
    private static final String ERM_SEARCH_FAVORITE_LIST = "/erm/search/searchFavoriteList";
    private static final String ERM_DEL_FAVORITE = "/erm/del/delFavorite";
    private static final String ERM_GET_OPEN_MODEL = "/erm/get/getOpenModel";

    public String addFavorite(String projectId, String objectId, String type) {
        String payload = """
                {
                    "projectId": "%s",
                    "objectId": "%s",
                    "type": "%s"
                }
                """.formatted(projectId, objectId, type);
        return post(ERM_ADD_FAVORITE, payload);
    }

    public String addFavorite(String projectId, String objectId, String type, String objectMasterId) {
        String payload = """
                {
                    "projectId": "%s",
                    "objectMasterId": "%s",
                    "objectId": "%s",
                    "type": "%s"
                }
                """.formatted(projectId, objectMasterId, objectId, type);
        return post(ERM_ADD_FAVORITE, payload);
    }

    public String searchFavoriteList(String projectId) {
        String payload = """
                {"projectId": "%s"}
                """.formatted(projectId);
        return post(ERM_SEARCH_FAVORITE_LIST, payload);
    }

    public String deleteFavorite(String favoriteId) {
        String payload = """
                {"objectId": "%s"}
                """.formatted(favoriteId);
        return post(ERM_DEL_FAVORITE, payload);
    }

    public String getOpenModel(String objectId, String hasAccess, String openPerson) {
        return get(ERM_GET_OPEN_MODEL + "?objectId=" + objectId
                + "&hasAccess=" + hasAccess + "&openPerson=" + openPerson);
    }

    // ========== Project Person Allocation (权限分配) ==========

    private static final String SYS_SEARCH_PROJECT_LIST = "/common/search/searchProjectList";
    private static final String SYS_SEARCH_PROJECT_PERSON_LIST = "/common/search/searchProjectPersonList";
    private static final String SYS_ASSIGN_PROJECT_PERSON = "/common/update/assignProjectPersonList";
    private static final String SYS_DEPT_TREE = "/system/user/deptTree";
    private static final String SYS_USER_LIST = "/system/user/listWithoutAdmins";

    public String searchProjectList(String title, String originated) {
        return get(SYS_SEARCH_PROJECT_LIST + "?title=" + (title != null ? title : "")
                + "&originated=" + (originated != null ? originated : ""));
    }

    public String searchProjectPersonList(String objectId) {
        return get(SYS_SEARCH_PROJECT_PERSON_LIST + "?objectId=" + objectId);
    }

    public String assignProjectPersonList(String objectId, String dataJson) {
        String payload = """
                {
                    "objectId": "%s",
                    "data": %s
                }
                """.formatted(objectId, dataJson);
        return post(SYS_ASSIGN_PROJECT_PERSON, payload);
    }

    public String deptTree() {
        return get(SYS_DEPT_TREE);
    }

    public String listUsersWithoutAdmins(int pageNum, int pageSize, String deptId, String userName) {
        return get(SYS_USER_LIST + "?pageNum=" + pageNum + "&pageSize=" + pageSize
                + "&deptId=" + (deptId != null ? deptId : "")
                + "&userName=" + (userName != null ? userName : ""));
    }

    // ========== System User Management (系统用户管理 /system/user/) ==========

    private static final String SYS_USER_LIST_ALL = "/system/user/list";
    private static final String SYS_USER_DICT_SEX = "/system/dict/data/type/sys_user_sex";
    private static final String SYS_USER_CRUD = "/system/user";

    public String sysUserList(int pageNum, int pageSize, String userName, String phonenumber, String status) {
        StringBuilder url = new StringBuilder(SYS_USER_LIST_ALL
                + "?pageNum=" + pageNum + "&pageSize=" + pageSize
                + "&userName=" + (userName != null ? userName : "")
                + "&phonenumber=" + (phonenumber != null ? phonenumber : "")
                + "&status=" + (status != null ? status : ""));
        return get(url.toString());
    }

    public String sysUserGetById(String userId) {
        return get(SYS_USER_CRUD + "/" + userId);
    }

    public String sysUserCreate(String userName, String nickName, String password,
                                int deptId, String email, String phonenumber, String sex,
                                String status, String remark, String secretLevel, String postIdsJson) {
        String payload = """
                {
                    "userId": 0,
                    "userName": "%s",
                    "nickName": "%s",
                    "password": "%s",
                    "deptId": %d,
                    "email": "%s",
                    "phonenumber": "%s",
                    "sex": "%s",
                    "status": "%s",
                    "remark": "%s",
                    "expirationTime": "",
                    "isTemp": 0,
                    "secretLevel": "%s",
                    "admin": false,
                    "delFlag": "0",
                    "createTime": "",
                    "updateTime": "",
                    "avatar": "",
                    "deptName": "",
                    "roles": [],
                    "postIds": %s
                }
                """.formatted(userName, nickName, password, deptId, email, phonenumber,
                sex, status, remark, secretLevel, postIdsJson);
        return post(SYS_USER_CRUD, payload);
    }

    public String sysUserUpdate(String jsonBody) {
        return put(SYS_USER_CRUD, jsonBody);
    }

    public String sysUserDelete(String userId) {
        return delete(SYS_USER_CRUD + "/" + userId);
    }

    public String sysDictData(String dictType) {
        return get("/system/dict/data/type/" + dictType);
    }

    public APIResponse sysUserExport() {
        return request.post(TestConfig.API_PREFIX + SYS_USER_CRUD + "/export",
                RequestOptions.create().setHeader("Content-Type", "application/json"));
    }

    public String sysUserResetPwd(String userId, String password) {
        String payload = """
                {
                    "userId": "%s",
                    "password": "%s"
                }
                """.formatted(userId, password);
        return put(SYS_USER_CRUD + "/resetPwd", payload);
    }

    public APIResponse sysUserImportTemplate() {
        return request.post(TestConfig.API_PREFIX + SYS_USER_CRUD + "/importTemplate",
                RequestOptions.create().setHeader("Content-Type", "application/json"));
    }

    public String sysUserImportData(String filePath, boolean updateSupport) {
        APIResponse response = request.post(
                TestConfig.API_PREFIX + SYS_USER_CRUD + "/importData?updateSupport=" + updateSupport,
                RequestOptions.create()
                        .setMultipart(FormData.create()
                                .set("file", Paths.get(filePath)))
        );
        return response.text();
    }

    public String sysUserDeptTree() {
        return get(SYS_USER_CRUD + "/deptTree");
    }

    // ========== System Post Management (岗位管理 /system/post/) ==========

    private static final String SYS_POST_CRUD = "/system/post";

    public String sysPostList(int pageNum, int pageSize, String postCode, String postName, String status) {
        return get(SYS_POST_CRUD + "/list?pageNum=" + pageNum + "&pageSize=" + pageSize
                + "&postCode=" + (postCode != null ? postCode : "")
                + "&postName=" + (postName != null ? postName : "")
                + "&status=" + (status != null ? status : ""));
    }

    public String sysPostGetById(String postId) {
        return get(SYS_POST_CRUD + "/" + postId);
    }

    public String sysPostCreate(String postName, String postCode, int postSort, String status, String remark) {
        String payload = """
                {
                    "postName": "%s",
                    "postCode": "%s",
                    "postSort": %d,
                    "status": "%s",
                    "remark": "%s"
                }
                """.formatted(postName, postCode, postSort, status, remark);
        return post(SYS_POST_CRUD, payload);
    }

    public String sysPostUpdate(String jsonBody) {
        return put(SYS_POST_CRUD, jsonBody);
    }

    public String sysPostDelete(String postId) {
        return delete(SYS_POST_CRUD + "/" + postId);
    }

    public APIResponse sysPostExport() {
        return request.post(TestConfig.API_PREFIX + SYS_POST_CRUD + "/export",
                RequestOptions.create().setHeader("Content-Type", "application/json"));
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

    private String put(String endpoint, String payload) {
        APIResponse response = request.put(
                TestConfig.API_PREFIX + endpoint,
                RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData(payload)
        );
        return response.text();
    }

    private String delete(String endpoint) {
        APIResponse response = request.delete(TestConfig.API_PREFIX + endpoint);
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