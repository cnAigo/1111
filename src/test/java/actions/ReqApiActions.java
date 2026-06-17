package actions;

import com.google.gson.*;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import config.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.util.UUID;

public class ReqApiActions {

    private static final Logger log = LoggerFactory.getLogger(ReqApiActions.class);
    private final APIRequestContext request;
    private static final String P = TestConfig.API_PREFIX;

    private static final String PARENT_TYPE_FOLDER = "reqSpeFolder";
    private static final String PARENT_TYPE_PROJECT = "project";

    // ── ERM: Folder ──
    private static final String ERM_ADD_FOLDER    = "/erm/add/addReqSpeFolder";
    private static final String ERM_UPDATE_FOLDER  = "/erm/update/updateReqSpeFolderInfo";
    private static final String ERM_DEL_FOLDER     = "/erm/del/delReqSpeFolder";
    private static final String ERM_RECOVER_FOLDER = "/erm/recover/recoverReqSpeFolder";
    private static final String ERM_CLEAN_FOLDER   = "/erm/clean/cleanReqSpeFolder";
    private static final String ERM_SEARCH_FOLDER_CHILDREN = "/erm/search/searchReqFolderChildrenList";

    // ── ERM: Document (ReqSpe) ──
    private static final String ERM_ADD_DOC     = "/erm/add/addReqSpe";
    private static final String ERM_UPDATE_DOC   = "/erm/update/updateReqSpeInfo";
    private static final String ERM_DEL_DOC      = "/erm/del/delReqSpe";
    private static final String ERM_RECOVER_DOC  = "/erm/recover/recoverReqSpe";
    private static final String ERM_CLEAN_DOC    = "/erm/clean/cleanReqSpe";
    private static final String ERM_SEARCH_SPE_LIST = "/erm/search/searchReqSpeBaseLineList";

    // ── ERM: Req Item ──
    private static final String ERM_ADD_REQ         = "/erm/add/addReq";
    private static final String ERM_UPDATE_REQ_LIST  = "/erm/update/updateReqList";
    private static final String ERM_SEARCH_CHILD_REQ = "/erm/search/searchChildReqInfoByReqSpeId";
    private static final String ERM_DEL_REQ_ITEMS    = "/erm/del/delReqObjectList";
    private static final String ERM_RECOVER_REQ_ITEMS = "/erm/recover/recoverReq";
    private static final String ERM_CLEAN_REQ_ITEMS  = "/erm/clean/cleanReq";
    private static final String ERM_COPY_REQ         = "/erm/update/copyReq";
    private static final String ERM_CHANGE_REQ_POS   = "/erm/update/changeReqPosition";
    private static final String ERM_UPDATE_REQ_STATE = "/erm/update/updateReqSpeState";

    // ── ERM: Favorite ──
    private static final String ERM_ADD_FAV      = "/erm/add/addFavorite";
    private static final String ERM_SEARCH_FAV   = "/erm/search/searchFavoriteList";
    private static final String ERM_DEL_FAV      = "/erm/del/delFavorite";

    // ── ERM: View ──
    private static final String ERM_ADD_VIEW     = "/erm/add/addReqSpeView";
    private static final String ERM_SEARCH_VIEW  = "/erm/search/searchReqSpeViewList";
    private static final String ERM_DEL_VIEW     = "/erm/del/delReqSpeView";

    // ── ERM: Custom Attribute ──
    private static final String ERM_ATTR_ADD     = "/erm/customAttribute/addCustomAttribute";
    private static final String ERM_ATTR_CHECK   = "/erm/customAttribute/checkAttribute";
    private static final String ERM_ATTR_SELECT  = "/erm/customAttribute/selectCustomAttributeList";
    private static final String ERM_ATTR_UPDATE  = "/erm/customAttribute/updateCustomAttribute";
    private static final String ERM_ATTR_DELETE  = "/erm/customAttribute/deleteCustomAttributes";
    private static final String ERM_ATTR_PUBLISH = "/erm/customAttribute/publishCustomAttributes";
    private static final String ERM_ATTR_SEARCH  = "/erm/customAttribute/searchAttributes";

    // ── ERM: Version / Trace ──
    private static final String ERM_VERSION_LIST   = "/erm/search/getReqSpeVersionList";
    private static final String ERM_GET_REQ_ACCESS = "/erm/get/getReqAccess";
    private static final String ERM_CHECK_OPEN     = "/erm/get/checkOpenMode";
    private static final String ERM_GET_OPEN       = "/erm/get/getOpenModel";
    private static final String ERM_UNLOCK         = "/erm/lockAndUnLockReq";
    private static final String ERM_SEARCH_TRACE   = "/erm/search/searchReqTrace";
    private static final String ERM_CHANGE_ANALYSIS = "/erm/get/getChangeAnalysis";

    // ── ERM: Search / Tree ──
    private static final String ERM_SEARCH_TREE   = "/erm/search/searchReqFolderStructureTree";
    private static final String ERM_SEARCH_CHILDREN = "/erm/search/searchChildrenListFromProject";

    // ── ERM: Export / Import ──
    private static final String ERM_EXPORT_EXCEL   = "/erm/exportExcelReqSpecification";
    private static final String ERM_EXPORT_WORD    = "/erm/exportWordReqSpecification";
    private static final String ERM_EXPORT_REQIF   = "/erm/reqIf/post/exportReqIf";
    private static final String ERM_GET_ATOZ       = "/erm/reqIf/get/getAllAtozParam";
    private static final String ERM_GET_TEMPLATES  = "/erm/attr/get/getTemplateNames";
    private static final String ERM_DOWNLOAD_TPL   = "/erm/downloadReqImportTemplate";
    private static final String ERM_IMPORT_ATTR    = "/erm/import/getAttributes";
    private static final String ERM_IMPORT_EXCEL   = "/erm/import/importReqSpecification";

    // ── ReqSpe Info / Update ──
    private static final String ERM_SEARCH_REQ_SPE_INFO   = "/erm/search/searchReqSpeInfo";
    private static final String ERM_UPDATE_REQ_SPE_INFO   = "/erm/update/updateReqSpeInfo";
    private static final String ERM_UPLOAD_REQ_DOC        = "/erm/upload/reqDocUpload";
    private static final String ERM_DELETE_REQ_DOC        = "/erm/reqDocDelete";
    private static final String ERM_UPDATE_WRITE_PERM     = "/erm/update/updateReqSpeWritePermission";

    // ── MOE: Indicator (指标管理) ──
    private static final String MOE = "/moe";
    private static final String MOE_ADD_STRUCTURE   = MOE + "/add/addLogicStructure";
    private static final String MOE_SEARCH_STRUCTURE = MOE + "/search/searchLogicStructureList";
    private static final String MOE_GET_STRUCTURE   = MOE + "/get/getLogicStructureInfo";
    private static final String MOE_ADD_LOGIC       = MOE + "/add/addLogic";
    private static final String MOE_SEARCH_LOGIC    = MOE + "/search/searchLogicList";
    private static final String MOE_GET_LOGIC       = MOE + "/get/getLogicInfo";
    private static final String MOE_DELETE_LOGIC    = MOE + "/delete/deleteLogic";
    private static final String MOE_UPDATE_CURRENT  = MOE + "/update/updateCurrent";

    // ── ReqIf Import ──
    private static final String ERM_IMPORT_REQIF_DOORS = "/erm/reqIf/get/getAllDoorsParam";
    private static final String ERM_IMPORT_REQIF       = "/erm/reqIf/add/importReqIfFile";

    /** Step 1: upload ReqIf file to get door attribute mapping. */
    public String getDoorsParam(Path filePath) {
        APIResponse resp = request.post(P + ERM_IMPORT_REQIF_DOORS,
            RequestOptions.create()
                .setMultipart(
                    com.microsoft.playwright.options.FormData.create()
                        .set("file", filePath)));
        return resp.text();
    }

    /** Step 2: import ReqIf file with mapping. */
    public String importReqIfFile(String projectId, String parentId, String parentType,
                                   Path filePath, String mappingAttrJson) {
        APIResponse resp = request.post(P + ERM_IMPORT_REQIF,
            RequestOptions.create()
                .setMultipart(
                    com.microsoft.playwright.options.FormData.create()
                        .set("file", filePath)
                        .set("parentId", parentId)
                        .set("parentType", parentType)
                        .set("projectId", projectId)
                        .set("mappingAttrJson", mappingAttrJson)));
        return resp.text();
    }

    // ── ReqIf Template ──
    private static final String ERM_INSERT_TEMPLATE = "/erm/attr/post/insertTemplate";

    public String insertTemplate(String templateName, String projectId, String describe, String atozParamResp) {
        JsonArray atozArr;
        try {
            JsonObject root = JsonParser.parseString(atozParamResp).getAsJsonObject();
            atozArr = root.getAsJsonArray("data");
        } catch (Exception e) {
            atozArr = new JsonArray();
        }
        JsonObject body = new JsonObject();
        body.addProperty("templateName", templateName);
        body.addProperty("projectId", projectId);
        body.addProperty("templateDescribe", describe != null ? describe : "");
        body.add("attrTemplateInfoRspVoList", atozArr);
        return postRaw(ERM_INSERT_TEMPLATE, body);
    }

    public String getTemplateNames(String projectId) {
        return get(ERM_GET_TEMPLATES, "projectId", projectId);
    }

    // ── Common: Project ──
    private static final String COMMON_SEARCH_PROJECT     = "/common/search/searchProjectList";
    // ── Common: Project Personnel ──
    private static final String COMMON_SEARCH_PERSON   = "/common/search/searchProjectPersonList";
    private static final String COMMON_ASSIGN_PERSON   = "/common/update/assignProjectPersonList";

    // ── System: User ──
    private static final String SYS_USER_LIST       = "/system/user/list";
    private static final String SYS_USER            = "/system/user";
    private static final String SYS_USER_DEPT_TREE  = "/system/user/deptTree";
    private static final String SYS_USER_RESET_PWD  = "/system/user/resetPwd";
    private static final String SYS_USER_EXPORT     = "/system/user/export";
    private static final String SYS_USER_IMPORT     = "/system/user/importData";
    private static final String SYS_USER_IMPORT_TPL = "/system/user/importTemplate";
    private static final String SYS_USER_LIST_NO_ADMIN = "/system/user/listWithoutAdmins";

    // ── System: Post ──
    private static final String SYS_POST_LIST   = "/system/post/list";
    private static final String SYS_POST        = "/system/post";
    private static final String SYS_POST_EXPORT = "/system/post/export";

    // ── System: Dict ──
    private static final String SYS_DICT_TYPE_LIST = "/system/dict/type/list";
    private static final String SYS_DICT_TYPE      = "/system/dict/type";
    private static final String SYS_DICT_TYPE_EXPORT = "/system/dict/type/export";
    private static final String SYS_DICT_DATA_TYPE  = "/system/dict/data/type";

    // ── Cooperation Area (Project) ──
    private static final String COOP_ADD    = "/common/add/addProject";
    private static final String COOP_UPDATE = "/common/update/updateProjectInfo";
    private static final String COOP_DEL    = "/common/delete/delProject";
    private static final String COOP_SEARCH = "/common/search/searchProjectList";
    private static final String COOP_ADD_USER = "/common/update/assignProjectPersonList";
    private static final String COOP_DEL_USER = "/common/update/removeProjectPersonList";

    private Runnable reLogin;

    public ReqApiActions(APIRequestContext request) { this.request = request; }

    public void setReLogin(Runnable r) { this.reLogin = r; }

    // ═══════════════════════ Project ═══════════════════════

    public String searchProjectByUser() {
        return get(COMMON_SEARCH_PROJECT, "title", "", "originated", "");
    }

    public String getProjectIdByName(String projectName) {
        return getProjectIdByName(projectName, null);
    }

    public String getProjectIdByName(String projectName, String loginName) {
        String resp;
        if (loginName != null && !loginName.isBlank()) {
            resp = get(COMMON_SEARCH_PROJECT, "title", projectName, "loginName", loginName, "originated", "");
        } else {
            resp = get(COMMON_SEARCH_PROJECT, "title", projectName, "originated", "");
        }
        JsonArray data = dataArr(resp);
        if (data != null) for (JsonElement e : data) {
            JsonObject o = e.getAsJsonObject();
            if (projectName.equals(str(o, "title")) || projectName.equals(str(o, "projectName")) || projectName.equals(str(o, "name"))) {
                String v = str(o, "projectId", "id");
                return v.isEmpty() ? str(o, "objectId") : v;
            }
        }
        throw new RuntimeException("Project not found: " + projectName);
    }

    // ═══════════════════════ Folder CRUD ═══════════════════════

    private String parentType(String projectId, String parentId) {
        return projectId.equals(parentId) ? "project" : "reqSpeFolder";
    }

    public String createFolder(String projectId, String parentId) {
        JsonObject b = obj("projectId", projectId, "parentId", parentId,
            "parentType", parentType(projectId, parentId),
            "title", "AT_Folder", "description", "auto");
        return extractId(post(ERM_ADD_FOLDER, b));
    }

    public String renameFolder(String projectId, String folderId, String parentId, String newName) {
        JsonObject b = obj("projectId", projectId, "objectId", folderId,
            "parentId", parentId, "parentType", parentType(projectId, parentId),
            "title", newName);
        return post(ERM_UPDATE_FOLDER, b);
    }

    public String deleteFolder(String folderId, String projectId, String type) {
        JsonObject b = obj("objectId", folderId, "parentId", projectId,
            "parentType", type != null ? type : "project");
        return post(ERM_DEL_FOLDER, b);
    }

    public String deleteFolderCleanup(String folderId, String projectId, String type) {
        JsonObject b = obj("objectId", folderId, "parentId", projectId,
            "parentType", type != null ? type : "project");
        return postCleanup(ERM_DEL_FOLDER, b);
    }

    public String deleteFolder(String folderId, String projectId) {
        return deleteFolder(folderId, projectId, null);
    }

    public String deleteDocumentCleanup(String docId, String parentId) {
        return postCleanup(ERM_DEL_DOC, obj("objectId", docId, "parentId", parentId, "parentType", "reqSpeFolder"));
    }

    public String recoverFolder(String folderId, String parentId) {
        String pid = nvl(parentId, folderId);
        return post(ERM_RECOVER_FOLDER, obj("objectId", folderId, "parentId", pid,
            "parentType", "reqSpeFolder"));
    }

    /** 递归清理文件夹内所有子节点（从下往上），再删自身。 */
    public void cleanFolderTree(String folderId, String projectId) {
        try {
            String treeResp = getTree(folderId, projectId);
            JsonObject root = JsonParser.parseString(treeResp).getAsJsonObject();
            if (root.has("data") && !root.get("data").isJsonNull()) {
                cleanRecursive(root.getAsJsonArray("data"), folderId);
            }
        } catch (Exception ignored) {}
    }

    private void cleanRecursive(JsonArray items, String parentId) {
        for (JsonElement el : items) {
            JsonObject node = el.getAsJsonObject();
            String type = str(node, "type");
            String id   = str(node, "objectId", "id");
            // 先递归清孙子（子文件夹的 parentType 一定是 reqSpeFolder）
            if (node.has("children") && !node.get("children").isJsonNull()) {
                cleanRecursive(node.getAsJsonArray("children"), id);
            }
            // 再删自身
            if (id.isBlank()) continue;
            try {
                if ("reqSpeFolder".equals(type)) {
                    deleteFolderCleanup(id, parentId, "reqSpeFolder");
                    deleteFolderCleanup(id, parentId, "project");
                    forceCleanFolder(id);
                } else if ("reqSpe".equals(type)) {
                    deleteDocumentCleanup(id, parentId);
                    forceCleanDocument(id, parentId);
                }
            } catch (Exception ignored) {}
        }
    }

    public String forceCleanFolder(String folderId) {
        postCleanup(ERM_RECOVER_FOLDER, obj("objectId", folderId, "parentId", folderId));
        return postCleanup(ERM_CLEAN_FOLDER, obj("objectId", folderId));
    }

    public void forceCleanDocument(String docId, String parentId) {
        postCleanup(ERM_RECOVER_DOC, obj("objectId", docId, "parentId", parentId, "parentType", PARENT_TYPE_FOLDER));
        postCleanup(ERM_CLEAN_DOC, obj("objectId", docId, "parentId", parentId, "parentType", PARENT_TYPE_FOLDER));
    }

    public void cleanFolderByName(String projectId, String targetName) {
        log.info("====== 开始清理环境 ======");
        String resp = getTree(projectId, projectId);
        JsonObject root;
        try {
            root = JsonParser.parseString(resp).getAsJsonObject();
        } catch (Exception e) {
            log.error("清理失败：获取目录树的接口返回了非 JSON 数据！");
            log.error("接口返回的真实内容是: \n{}", resp);
            return;
        }
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
            cleanChildrenBottomUp(targetNode.getAsJsonArray("children"));
        }
        log.info("\n====== 清理环境结束 ======");
    }

    /** Recursively clean children bottom-up (grandchildren first) to avoid "下有子级" errors. */
    private void cleanChildrenBottomUp(JsonArray children) {
        for (JsonElement childEl : children) {
            JsonObject child = childEl.getAsJsonObject();
            String childType = child.get("type").getAsString();
            // Recurse into grandchildren first
            if (child.has("children") && !child.get("children").isJsonNull()) {
                cleanChildrenBottomUp(child.getAsJsonArray("children"));
            }
            // Then delete this node
            String childId = child.get("objectId").getAsString();
            String childTitle = child.get("title").getAsString();
            if (PARENT_TYPE_FOLDER.equals(childType)) {
                deleteFolder(childId, childId, PARENT_TYPE_FOLDER);
                forceCleanFolder(childId);
                log.info("  已清理子文件夹: {}", childTitle);
            } else if ("reqSpe".equals(childType)) {
                deleteDocument(childId, childId);
                forceCleanDocument(childId, childId);
                log.info("  已清理需求规格: {}", childTitle);
            }
        }
    }

    public void sweepByPrefix(String projectId, String prefix) {
        String resp = getTree(projectId, projectId);
        try {
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            if (!root.has("data") || root.get("data").isJsonNull()) return;
            JsonArray dataList = root.getAsJsonArray("data");
            sweepNodes(dataList, projectId, prefix);
        } catch (Exception e) { log.warn("sweepByPrefix failed: {}", e.getMessage()); }
    }

    private void sweepNodes(JsonArray items, String parentId, String prefix) {
        for (JsonElement el : items) {
            JsonObject node = el.getAsJsonObject();
            String type = str(node, "type");
            String objectId = str(node, "objectId", "id");
            String title = str(node, "title", "name");
            if (node.has("children") && !node.get("children").isJsonNull())
                sweepNodes(node.getAsJsonArray("children"), objectId, prefix);
            if (!objectId.isBlank() && title.startsWith(prefix)) {
                try {
                    if ("reqSpe".equals(type)) { deleteDocumentCleanup(objectId, parentId); forceCleanDocument(objectId, parentId); }
                    else if (PARENT_TYPE_FOLDER.equals(type)) {
                        deleteFolderCleanup(objectId, parentId, PARENT_TYPE_FOLDER);
                        deleteFolderCleanup(objectId, parentId, "project");
                        forceCleanFolder(objectId);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public void sweepATFolders(String projectId) {
        sweepByPrefix(projectId, "AT_");
    }

    public void cleanAllUnderRoot(String projectId) {
        String resp = getTree(projectId, projectId);
        try {
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            if (!root.has("data") || root.get("data").isJsonNull()) return;
            JsonArray dataList = root.getAsJsonArray("data");
            sweepAllNodes(dataList, projectId);
        } catch (Exception e) { log.warn("cleanAllUnderRoot failed: {}", e.getMessage()); }
    }

    private void sweepAllNodes(JsonArray items, String parentId) {
        for (JsonElement el : items) {
            JsonObject node = el.getAsJsonObject();
            String type = str(node, "type");
            String objectId = str(node, "objectId", "id");
            if (node.has("children") && !node.get("children").isJsonNull())
                sweepAllNodes(node.getAsJsonArray("children"), objectId);
            if (!objectId.isBlank() && !"project".equals(type)) {
                try {
                    if ("reqSpe".equals(type)) { deleteDocumentCleanup(objectId, parentId); forceCleanDocument(objectId, parentId); }
                    else if ("req".equals(type)) { deleteReqItem(objectId); }
                    else if (PARENT_TYPE_FOLDER.equals(type)) {
                        deleteFolderCleanup(objectId, parentId, PARENT_TYPE_FOLDER);
                        deleteFolderCleanup(objectId, parentId, "project");
                        forceCleanFolder(objectId);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // ═══════════════════════ Document CRUD ═══════════════════════

    public String createDocument(String projectId, String parentId) {
        JsonObject b = obj("projectId", projectId, "parentId", parentId,
            "parentType", "reqSpeFolder",
            "title", "AT_Doc", "description", "auto");
        return extractId(post(ERM_ADD_DOC, b));
    }

    public String renameDocument(String projectId, String docId, String parentId, String newName) {
        JsonObject b = obj("projectId", projectId, "objectId", docId,
            "parentId", parentId, "parentType", "reqSpeFolder",
            "title", newName);
        return post(ERM_UPDATE_DOC, b);
    }

    public String deleteDocument(String docId, String parentId) {
        return post(ERM_DEL_DOC, obj("objectId", docId, "parentId", parentId, "parentType", "reqSpeFolder"));
    }

    public String recoverDocument(String docId, String parentId) {
        return post(ERM_RECOVER_DOC, obj("objectId", docId, "parentId", parentId, "parentType", "reqSpeFolder"));
    }

    public String cleanDocument(String docId, String parentId) {
        forceCleanDocument(docId, parentId);
        return null;
    }

    // ═══════════════════════ Req Item CRUD ═══════════════════════

    public String addReqItem(String projectId, String parentId, String docId) {
        return extractId(addReqItemRaw(projectId, parentId, docId));
    }

    public String addReqItemRaw(String projectId, String parentId, String docId) {
        JsonObject b = obj("projectId", projectId, "parentId", parentId,
            "parentReqSpeId", docId, "beforeLinkOrderNo", "");
        return post(ERM_ADD_REQ, b);
    }

    public String deleteReqItem(String itemId) {
        return post(ERM_DEL_REQ_ITEMS, obj("objectId", itemId));
    }

    public String recoverReqItem(String itemId) {
        return post(ERM_RECOVER_REQ_ITEMS, obj("objectId", itemId));
    }

    public String cleanReqItem(String itemId, String docId) {
        return post(ERM_CLEAN_REQ_ITEMS, obj("objectId", itemId, "reqSpecId", docId));
    }

    /** Copy a req item to another location. beforeLinkOrderNo controls insert position. */
    public String copyReq(String parentReqSpeId, String parentId, String objectId, String beforeLinkOrderNo) {
        JsonObject b = obj("parentReqSpeId", parentReqSpeId, "parentId", parentId,
            "objectId", objectId, "beforeLinkOrderNo", nvl(beforeLinkOrderNo, ""));
        return post(ERM_COPY_REQ, b);
    }

    /** Move/cut a req item to a different position. beforeLinkOrderNo controls insert position. */
    public String changeReqPosition(String parentId, String objectId, String beforeLinkOrderNo) {
        JsonObject b = obj("parentId", parentId, "objectId", objectId,
            "beforeLinkOrderNo", nvl(beforeLinkOrderNo, ""));
        return post(ERM_CHANGE_REQ_POS, b);
    }

    /** Switch work state of a req spec: Inwork (工作中) / Frozen (冻结). */
    public String updateReqSpeState(String objectId, String current) {
        return post(ERM_UPDATE_REQ_STATE, obj("objectId", objectId, "current", current));
    }

    public String searchChildReqInfo(String objectId) {
        return post(ERM_SEARCH_CHILD_REQ, obj("objectId", objectId));
    }

    public String updateReqList(String reqSpeId, String json) {
        JsonObject b = new JsonObject();
        b.addProperty("reqSpeId", reqSpeId);
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(json);
        } catch (com.google.gson.JsonSyntaxException e) {
            // 负向测试可能传非法JSON，直接作为字符串发过去
            b.addProperty("reqList", json);
            return post(ERM_UPDATE_REQ_LIST, b);
        }
        b.add("reqList", parsed);
        return post(ERM_UPDATE_REQ_LIST, b);
    }

    public String editFolderDescription(String projectId, String folderId, String parentId, String description) {
        return post(ERM_UPDATE_FOLDER,
            obj("projectId", projectId, "objectId", folderId, "parentId", parentId,
                "parentType", parentType(projectId, parentId),
                "description", description));
    }

    public String editDescription(String projectId, String docId, String folderId, String description) {
        return post(ERM_UPDATE_DOC,
            obj("projectId", projectId, "objectId", docId, "parentId", folderId,
                "parentType", "reqSpeFolder", "description", description));
    }

    // ═══════════════════════ Search / Tree ═══════════════════════

    public String searchFolderChildren(String parentId) {
        return post(ERM_SEARCH_FOLDER_CHILDREN, obj("objectId", parentId));
    }

    public String searchFolderChildren(String projectId, String parentId) {
        return post(ERM_SEARCH_FOLDER_CHILDREN, obj("projectId", projectId, "objectId", parentId));
    }

    /** Search requirement spec info by objectId. Returns full details (title, description, codingRule, etc.). */
    public String searchReqSpeInfo(String objectId) {
        return post(ERM_SEARCH_REQ_SPE_INFO, obj("objectId", objectId));
    }

    /** Delete an uploaded document/attachment by its objectId. */
    public String deleteReqDoc(String objectId) {
        return post(ERM_DELETE_REQ_DOC, obj("objectId", objectId));
    }

    /** Upload a document file. Returns JSON with downloadURL and objectId. */
    public String reqDocUpload(Path filePath) {
        APIResponse resp = request.post(P + ERM_UPLOAD_REQ_DOC,
            RequestOptions.create()
                .setMultipart(
                    com.microsoft.playwright.options.FormData.create()
                        .set("file", filePath)));
        return resp.text();
    }

    /** Update requirement spec info: title, codingRule (prefix), description, file attachments, and custom attributes. */
    public String updateReqSpeInfo(String projectId, String objectId, String title, String codingRule, String description, String docDataJson) {
        return updateReqSpeInfo(projectId, objectId, title, codingRule, description, docDataJson, null);
    }

    /** Update requirement spec info with custom attribute values.
     *  customAttributeJson format: [{"attrId":"...","value":"..."}, ...] */
    public String updateReqSpeInfo(String projectId, String objectId, String title, String codingRule,
                                   String description, String docDataJson, String customAttributeJson) {
        JsonObject b = obj("projectId", projectId, "objectId", objectId,
            "title", nvl(title), "codingRule", nvl(codingRule), "description", nvl(description));
        if (docDataJson != null && !docDataJson.isEmpty()) {
            try {
                b.add("docData", JsonParser.parseString(docDataJson));
            } catch (Exception e) {
                b.addProperty("docData", docDataJson);
            }
        }
        if (customAttributeJson != null && !customAttributeJson.isEmpty()) {
            try {
                b.add("customAttribute", JsonParser.parseString(customAttributeJson));
            } catch (Exception e) {
                b.addProperty("customAttribute", customAttributeJson);
            }
        }
        return post(ERM_UPDATE_REQ_SPE_INFO, b);
    }

    /** Update write permission for a requirement spec. personData format: [{"objectId":"1","userName":"admin"},...] */
    public String updateReqSpeWritePermission(String objectId, String personDataJson) {
        JsonObject b = new JsonObject();
        b.addProperty("objectId", objectId);
        try {
            b.add("personData", JsonParser.parseString(personDataJson));
        } catch (Exception e) {
            b.addProperty("personData", personDataJson);
        }
        return post(ERM_UPDATE_WRITE_PERM, b);
    }

    public String getReqSpeList(String projectId) {
        return post(ERM_SEARCH_SPE_LIST, obj("projectId", projectId));
    }

    public String getTree(String parentId, String projectId) {
        return post(ERM_SEARCH_TREE, obj("projectId", projectId, "parentId", parentId,
            "parentType", projectId.equals(parentId) ? "project" : "reqSpeFolder"));
    }

    public String searchAttributes(String projectId, String bizDomain, String type) {
        return get(ERM_ATTR_SEARCH, "projectId", projectId,
            "businessDomain", nvl(bizDomain, "req"),
            "objectType", nvl(type));
    }

    public String searchAttributes(String projectId, String bizDomain) {
        return searchAttributes(projectId, bizDomain, null);
    }

    public String findNodeIdByTitle(String parentId, String title) {
        String resp = getTree(parentId, parentId);
        try {
            JsonArray arr = dataArr(resp);
            if (arr != null) for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                if (title.equals(str(o, "title"))) return str(o, "id");
            }
        } catch (Exception ex) { log.warn("findNodeIdByTitle failed: {}", ex.getMessage()); }
        return null;
    }

    // ═══════════════════════ Favorite ═══════════════════════

    public String addFavorite(String projectId, String objectId, String type) {
        return post(ERM_ADD_FAV, obj("projectId", projectId, "objectId", objectId, "type", type));
    }

    public String addFavorite(String projectId, String objectId, String type, String parentId) {
        return post(ERM_ADD_FAV, obj("projectId", projectId, "objectId", objectId, "type", type));
    }

    public String searchFavoriteList(String projectId) {
        return post(ERM_SEARCH_FAV, obj("projectId", projectId));
    }

    public String deleteFavorite(String favoriteId) {
        return post(ERM_DEL_FAV, obj("objectId", favoriteId));
    }

    // ═══════════════════════ View ═══════════════════════

    public String addView(String objectId, String name, String description, String columns) {
        JsonObject b = obj("objectId", objectId, "name", nvl(name, "AT_V_" + suf()),
            "description", nvl(description, "auto"));
        if (columns != null && !columns.isEmpty()) {
            b.addProperty("viewHeaderValues", columns);
        }
        String resp = post(ERM_ADD_VIEW, b);
        if (!isOk(resp)) return null;
        // API returns no ID — search the view list to find it by name
        String listResp = searchViewList(objectId);
        if (!isOk(listResp)) return null;
        try {
            JsonArray data = dataArr(listResp);
            if (data != null) for (JsonElement e : data) {
                JsonObject v = e.getAsJsonObject();
                if (name.equals(str(v, "name"))) {
                    String vid = str(v, "id", "objectId");
                    return vid.isEmpty() ? str(v, "viewId") : vid;
                }
            }
        } catch (Exception ex) { log.warn("addView findId failed: {}", ex.getMessage()); }
        return null;
    }

    public String searchViewList(String objectId) {
        return post(ERM_SEARCH_VIEW, obj("objectId", objectId));
    }

    public String deleteView(String viewId) {
        return post(ERM_DEL_VIEW, obj("objectId", viewId));
    }

    // ═══════════════════════ Custom Attribute ═══════════════════════

    public String addCustomAttribute(String nameEn, String name, String type, String projectId) {
        return post(ERM_ATTR_ADD, obj(
            "nameEn", nameEn, "name", name, "type", type,
            "projectId", projectId, "description", "auto",
            "current", "1", "valueRange", "", "defaultValue", "",
            "isMultiple", false, "businessDomain", "需求管理",
            "objectType", "req", "id", "", "createTime", "",
            "creator", "", "modifier", "",
            "usedColor", "#1e90ff", "isUseDefaultValue", false,
            "valueRangeMapping", new JsonArray()));
    }

    public String[] findCustomAttribute(String nameEn, String projectId) {
        String resp = getCustomAttributeList(projectId);
        JsonArray arr = dataArr(resp);
        if (arr != null) for (JsonElement e : arr) {
            JsonObject a = e.getAsJsonObject();
            if (nameEn.equals(str(a, "nameEn")))
                return new String[]{str(a, "id"), str(a, "name"), str(a, "type")};
        }
        return null;
    }

    public String getCustomAttributeList(String projectId) {
        return get(ERM_ATTR_SELECT, "projectId", projectId, "businessDomain", "",
            "objectType", "", "name", "", "type", "", "current", "");
    }

    /** 获取已发布的自定义属性列表（用于编辑需求时选择属性） */
    public String selectCustomAttributeList(String projectId, String businessDomain,
                                            String objectType, String current) {
        return get(ERM_ATTR_SELECT, "projectId", projectId, "businessDomain", nvl(businessDomain),
            "objectType", nvl(objectType), "name", "", "type", "", "current", nvl(current));
    }

    public String searchCustomAttribute(String projectId, String businessDomain, String objectType,
                                        String name, String type, String current) {
        return get(ERM_ATTR_SELECT, "projectId", projectId,
            "businessDomain", nvl(businessDomain), "objectType", nvl(objectType),
            "name", nvl(name), "type", nvl(type), "current", nvl(current));
    }

    public String updateCustomAttribute(String id, String nameEn, String name, String type,
                                        String originalName, String originalType, String projectId) {
        JsonObject b = obj("id", id, "nameEn", nameEn, "name", name, "type", type,
            "projectId", projectId, "description", "auto");
        if (originalName != null) b.addProperty("originalName", originalName);
        if (originalType != null) b.addProperty("originalType", originalType);
        return post(ERM_ATTR_UPDATE, b);
    }

    public String publishCustomAttribute(String id, String projectId) {
        JsonArray arr = new JsonArray(); arr.add(id);
        JsonObject b = new JsonObject();
        b.addProperty("projectId", projectId);
        b.add("attributeIds", arr);
        return postRaw(ERM_ATTR_PUBLISH, b);
    }

    public String batchPublishCustomAttributes(String projectId, String... ids) {
        JsonArray arr = new JsonArray();
        for (String id : ids) arr.add(id);
        JsonObject b = new JsonObject();
        b.addProperty("projectId", projectId);
        b.add("attributeIds", arr);
        return postRaw(ERM_ATTR_PUBLISH, b);
    }

    public String checkAttribute(String projectId, String nameEn, String name) {
        return get(ERM_ATTR_CHECK,
            "projectId", projectId, "businessDomain", "需求管理",
            "objectType", "req", "name", nvl(name), "nameEn", nvl(nameEn), "id", "");
    }

    public String deleteCustomAttribute(String id) {
        JsonArray arr = new JsonArray(); arr.add(id);
        return postRaw(ERM_ATTR_DELETE, arr.toString());
    }

    public String batchDeleteCustomAttributes(String... ids) {
        JsonArray arr = new JsonArray();
        for (String id : ids) arr.add(id);
        return postRaw(ERM_ATTR_DELETE, arr.toString());
    }

    // ═══════════════════════ Version / Trace ═══════════════════════

    public String getVersionList(String objectId) {
        return get(ERM_VERSION_LIST, "objectId", objectId);
    }

    public String getReqAccess(String objectId) {
        return get(ERM_GET_REQ_ACCESS, "objectId", objectId);
    }

    public String searchReqSpecTrace(String objectId, String type) {
        return post(ERM_SEARCH_TRACE, obj("objectId", objectId, "type", nvl(type)));
    }

    public String searchChangeAnalysis(String objectId, String versionId) {
        return post(ERM_CHANGE_ANALYSIS, obj("objectId", objectId, "versionId", nvl(versionId)));
    }

    public String checkOpenMode(String docId, String operateType, String openPerson) {
        return get(ERM_CHECK_OPEN,
            "masterId", docId, "operateType", nvl(operateType, "check"), "openPerson", nvl(openPerson, "admin"));
    }

    public String getOpenModel(String objectId) {
        return get(ERM_GET_OPEN, "objectId", objectId, "hasAccess", "true", "openPerson", "admin");
    }

    public String getOpenModel(String masterId, String operateType, String openPerson) {
        return get(ERM_GET_OPEN,
            "objectId", masterId, "hasAccess", "true", "openPerson", nvl(openPerson));
    }

    public String unlockMode(String masterId, String unlockMode, String unlockPerson) {
        return post(ERM_UNLOCK, obj("objectId", masterId,
            "lockMode", nvl(unlockMode, "unlock"), "lockModePerson", nvl(unlockPerson, "admin")));
    }

    // ═══════════════════════ Export / Import ═══════════════════════

    public APIResponse exportExcel(String objectId, String templateId) {
        return request.get(P + ERM_EXPORT_EXCEL + "?objectId=" + objectId
            + "&templateType=" + nvl(templateId, "one"));
    }

    public APIResponse exportWord(String objectId, String templateId) {
        return request.get(P + ERM_EXPORT_WORD + "?objectId=" + objectId
            + "&templateType=" + nvl(templateId, "one"));
    }

    public String exportReqIf(String payload) {
        return request.post(P + ERM_EXPORT_REQIF,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(payload)).text();
    }

    public String getAllAtozParam(String projectId) {
        return get(ERM_GET_ATOZ, "projectId", projectId,
            "businessDomain", "需求管理", "objectType", "req");
    }

    public APIResponse downloadImportTemplate(String type) {
        return request.get(P + ERM_DOWNLOAD_TPL + "?templateType=" + type);
    }

    public String getImportAttributes() {
        long t0 = System.currentTimeMillis();
        APIResponse resp = request.get(P + ERM_IMPORT_ATTR);
        String text = resp.text();
        long ms = System.currentTimeMillis() - t0;
        log.info("API GET {} → HTTP {} ({}ms)", ERM_IMPORT_ATTR, resp.status(), ms);
        return text;
    }

    public String importReqSpecification(String projectId, String parentId, String reqSpecName, String dataJson) {
        return post(ERM_IMPORT_EXCEL,
            obj("projectId", projectId, "reqSpeParentId", parentId, "reqSpeName", reqSpecName, "dataJson", dataJson));
    }

    /** Excel import with real payload format (from HAR trace). */
    public String importExcelData(String projectId, String parentId, String specName, String dataJson) {
        JsonArray dataArr = JsonParser.parseString(dataJson).getAsJsonArray();
        JsonObject body = new JsonObject();
        body.addProperty("projectId", projectId);
        body.addProperty("parentId", parentId);
        body.addProperty("type", "reqSpeFolder");
        body.addProperty("reqSpecName", specName);
        body.add("data", dataArr);
        return postRaw(ERM_IMPORT_EXCEL, body);
    }

    private String postRaw(String path, JsonObject body) {
        return request.post(P + path,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(body.toString())).text();
    }

    // Word import endpoint (multipart upload)
    private static final String ERM_IMPORT_DOCX  = "/erm/import/importReqSpecDocx";

    /** Upload a Word docx into a folder. Returns full response text. */
    public String importWordDocx(String projectId, String parentId, String specName, Path filePath) {
        long t0 = System.currentTimeMillis();
        APIResponse resp = request.post(P + ERM_IMPORT_DOCX,
            RequestOptions.create()
                .setMultipart(
                    com.microsoft.playwright.options.FormData.create()
                        .set("file", filePath)
                        .set("parentId", parentId)
                        .set("projectId", projectId)
                        .set("reqSpecName", specName)
                        .set("type", "reqSpeFolder")));
        String text = resp.text();
        long ms = System.currentTimeMillis() - t0;
        log.info("API POST {} (file={}) → HTTP {} ({}ms) body={}", ERM_IMPORT_DOCX,
            filePath.getFileName(), resp.status(), ms, text);
        return text;
    }

    /** ReqIf export — actual payload format from browser HAR trace. */
    public String exportReqIfByBranch(String projectId, String specBranchId, String specName) {
        String payload = """
            {"reqSpeBranchIds":["%s"],"businessDomain":"需求管理","objectType":"req",\
            "projectId":"%s","atozReqSpecName":"%s","mappingAttrs":[]}\
            """.formatted(specBranchId, projectId, specName);
        return request.post(P + ERM_EXPORT_REQIF,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(payload)).text();
    }

    public String importUser(String json) {
        return request.post(P + SYS_USER_IMPORT + "?updateSupport=true",
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(json)).text();
    }

    public APIResponse sysUserImportTemplate() {
        return request.post(P + SYS_USER_IMPORT_TPL,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData("{}"));
    }

    // ═══════════════════════ Cooperation Area ═══════════════════════

    public String addCooperationArea(String name, String code, String securityLevel, String description) {
        return post(COOP_ADD, obj("objectId", "", "title", name, "name", code,
            "MBSE_SecretLevel", "1", "lbstype", "", "orderNo", "",
            "originated", "", "twcCategory", ""));
    }

    public String updateCooperationArea(String areaId, String name, String code, String securityLevel, String description) {
        return post(COOP_UPDATE, obj("objectId", areaId, "title", name, "name", code,
            "securityLevel", nvl(securityLevel, "内部"), "description", nvl(description)));
    }

    public String deleteCooperationArea(String areaId) {
        JsonArray arr = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("objectId", areaId);
        arr.add(item);
        return postRaw(COOP_DEL, arr.toString());
    }

    /** 删除所有合作区，保留名称/编号匹配 keepNames 的。返回删除数量。 */
    public int cleanAllCooperationAreasExcept(String... keepNames) {
        String resp = searchCooperationAreaList("", "");
        JsonArray data = dataArr(resp);
        if (data == null || data.isEmpty()) return 0;
        int deleted = 0;
        for (JsonElement el : data) {
            JsonObject item = el.getAsJsonObject();
            String name = str(item, "name");
            String title = str(item, "title");
            String objectId = str(item, "objectId");
            if (objectId.isEmpty()) continue;
            boolean keep = false;
            for (String k : keepNames) {
                if (k.equals(name) || k.equals(title)) { keep = true; break; }
            }
            if (keep) {
                log.info("保留合作区: name={}, title={}", name, title);
                continue;
            }
            try {
                String delResp = deleteCooperationArea(objectId);
                if (delResp.contains("\"code\":200")) {
                    deleted++;
                } else {
                    log.warn("删除合作区失败: name={}, title={}, resp={}", name, title, truncate(delResp, 100));
                }
            } catch (Exception e) {
                log.warn("删除合作区异常: name={}, title={}, err={}", name, title, e.getMessage());
            }
        }
        log.info("合作区清理完成: 删除{}个, 保留{}个", deleted, data.size() - deleted);
        return deleted;
    }

    public String searchCooperationAreaList(String keyword, String projectId) {
        return get(COOP_SEARCH, "title", nvl(keyword), "originated", "");
    }

    /** Find cooperation area ID by name. Returns null if not found. */
    public String findCooperationAreaId(String name) {
        String resp = searchCooperationAreaList(name, "");
        JsonArray data = dataArr(resp);
        if (data != null) for (JsonElement e : data) {
            JsonObject o = e.getAsJsonObject();
            if (name.equals(str(o, "title")) || name.equals(str(o, "name")))
                return str(o, "objectId", "id");
        }
        return null;
    }

    public String addCooperationAreaUser(String areaId, String userId) {
        JsonArray users = new JsonArray();
        JsonObject user = new JsonObject();
        user.addProperty("objectId", userId);
        users.add(user);
        return post(COOP_ADD_USER, obj("objectId", areaId, "data", users));
    }

    public String deleteCooperationAreaUser(String areaId, String userId) {
        JsonArray users = new JsonArray();
        JsonObject user = new JsonObject();
        user.addProperty("objectId", userId);
        users.add(user);
        return post(COOP_DEL_USER, obj("objectId", areaId, "data", users));
    }

    // ═══════════════════════ Project Personnel ═══════════════════════

    public String searchProjectList(String keyword, String page) {
        return get(COMMON_SEARCH_PROJECT, "keyword", nvl(keyword), "page", nvl(page));
    }

    public String searchProjectPersonList(String objectId) {
        return get(COMMON_SEARCH_PERSON, "objectId", objectId);
    }

    public String deptTree() {
        return get(SYS_USER_DEPT_TREE);
    }

    public String listUsersWithoutAdmins(int page, int pageSize, String keyword, String deptId) {
        return get(SYS_USER_LIST_NO_ADMIN,
            "pageNum", String.valueOf(page), "pageSize", String.valueOf(pageSize),
            "deptId", nvl(deptId), "userName", nvl(keyword));
    }

    public String assignProjectPersonList(String objectId, String data) {
        return request.post(P + COMMON_ASSIGN_PERSON,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(data)).text();
    }

    // ═══════════════════════ System User ═══════════════════════

    public String sysUserList(int page, int pageSize, String userName, String phone, String status) {
        return get(SYS_USER_LIST,
            "pageNum", String.valueOf(page), "pageSize", String.valueOf(pageSize),
            "userName", nvl(userName), "phonenumber", nvl(phone), "status", nvl(status));
    }

    public String sysUserCreate(String userName, String nickName, String password, int deptId,
                                 String phonenumber, String email, String sex, String status, String remark,
                                 String postIds, String roleIds) {
        JsonObject b = obj("userName", userName, "nickName", nickName, "password", password, "deptId", deptId);
        if (phonenumber != null) b.addProperty("phonenumber", phonenumber);
        if (email != null) b.addProperty("email", email);
        b.addProperty("sex", nvl(sex, "0"));
        b.addProperty("status", nvl(status, "0"));
        if (remark != null) b.addProperty("remark", remark);
        if (postIds != null) {
            JsonArray pa = new JsonArray(); for (String s : postIds.split(",")) pa.add(Long.parseLong(s.trim()));
            b.add("postIds", pa);
        }
        if (roleIds != null) {
            JsonArray ra = new JsonArray(); for (String s : roleIds.split(",")) ra.add(Long.parseLong(s.trim()));
            b.add("roleIds", ra);
        }
        return post(SYS_USER, b);
    }

    public String sysUserGetById(String userId) {
        return get(P + SYS_USER + "/" + userId);
    }

    public String sysUserUpdate(String json) {
        return request.put(P + SYS_USER,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(json)).text();
    }

    public String sysUserDelete(String userId) {
        return request.delete(P + SYS_USER + "/" + userId).text();
    }

    public String sysUserResetPwd(String userId, String password) {
        return put(SYS_USER_RESET_PWD, obj("userId", userId, "password", password));
    }

    public String resetPassword(String userId, String password) {
        return sysUserResetPwd(userId, password);
    }

    public APIResponse sysUserExport() {
        return request.post(P + SYS_USER_EXPORT,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData("{}"));
    }

    public String sysUserDeptTree() {
        return get(SYS_USER_DEPT_TREE);
    }

    public String sysDictData(String dictType) {
        return get(SYS_DICT_DATA_TYPE + "/" + dictType);
    }

    public String searchUser(String userName) {
        return get(SYS_USER_LIST, "pageNum", "1", "pageSize", "10",
            "userName", nvl(userName), "phonenumber", "", "status", "");
    }

    // ═══════════════════════ System Post ═══════════════════════

    public String sysPostList(int page, int pageSize, String postCode, String postName, String status) {
        return get(SYS_POST_LIST,
            "pageNum", String.valueOf(page), "pageSize", String.valueOf(pageSize),
            "postCode", nvl(postCode), "postName", nvl(postName), "status", nvl(status));
    }

    public String sysPostCreate(String postName, String postCode, int postSort, String status, String remark) {
        return post(SYS_POST, obj("postName", postName, "postCode", postCode,
            "postSort", postSort, "status", nvl(status, "0"), "remark", nvl(remark)));
    }

    public String sysPostUpdate(String json) {
        return request.put(P + SYS_POST,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(json)).text();
    }

    public String sysPostGetById(String postId) {
        return get(P + SYS_POST + "/" + postId);
    }

    public String sysPostDelete(String postId) {
        return request.delete(P + SYS_POST + "/" + postId).text();
    }

    public APIResponse sysPostExport() {
        return request.post(P + SYS_POST_EXPORT,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData("{}"));
    }

    // ═══════════════════════ Helpers ═══════════════════════

    public String extractId(String json) {
        try {
            JsonObject r = JsonParser.parseString(json).getAsJsonObject();
            if (r.get("code").getAsInt() != 200) return null;
            JsonObject d = r.getAsJsonObject("data");
            if (d.has("id")) return d.get("id").getAsString();
            if (d.has("objectId")) return d.get("objectId").getAsString();
        } catch (Exception e) { log.warn("extractId failed: {}", e.getMessage()); }
        return null;
    }

    public boolean isOk(String json) {
        try { return JsonParser.parseString(json).getAsJsonObject().get("code").getAsInt() == 200; }
        catch (Exception e) { return false; }
    }

    public boolean isDataEmpty(String json) {
        try {
            JsonObject r = JsonParser.parseString(json).getAsJsonObject();
            if (r.get("code").getAsInt() != 200) return true;
            JsonElement d = r.get("data");
            if (d == null || d.isJsonNull()) return true;
            if (d.isJsonArray() && d.getAsJsonArray().size() == 0) return true;
            if (d.isJsonObject()) {
                JsonObject o = d.getAsJsonObject();
                if (o.has("list") && o.get("list").isJsonArray() && o.getAsJsonArray("list").size() == 0) return true;
            }
            return false;
        } catch (Exception e) { return true; }
    }

    // ═══════════════════════ MOE: 指标管理 ═══════════════════════

    private static final String MOE_BASE = TestConfig.BASE_URL.replace("/dev-api","") + "/api-api";

    private String moePost(String path, JsonObject body) {
        long t0=System.currentTimeMillis();
        APIResponse resp=request.post(MOE_BASE + path, RequestOptions.create().setHeader("Content-Type","application/json").setData(body.toString()));
        String text=resp.text();
        log.info("API POST /api-api{} → HTTP {} ({}ms)",path,resp.status(),System.currentTimeMillis()-t0);
        return text;
    }
    public String addLogicStructure(String name, String desc, String projectId) {
        return moePost("/moe/add/addLogicStructure", obj("name",name,"description",nvl(desc),"projectId",projectId)); }
    public String searchLogicStructureList(String projectId) {
        return moePost("/moe/search/searchLogicStructureList", obj("projectId",projectId)); }
    public String getLogicStructureInfo(String structureId) {
        return moePost("/moe/get/getLogicStructureInfo", obj("objectId",structureId)); }
    public String addLogic(String parentId, String parentType, String name, String type, String logicStructureId, String projectId) {
        String oid=UUID.randomUUID().toString().replace("-","").substring(0,10);
        return moePost("/moe/add/addLogic", obj("objectId",oid,"parentId",nvl(parentId),"parentType",nvl(parentType),"name",name,"type",type,"deviceCode","默认设备编码","description","自动测试","level","","logicStructureId",logicStructureId,"addMark",true,"projectId",projectId)); }
    public String searchLogicList(String structureId) {
        return moePost("/moe/search/searchLogicList", obj("objectId",structureId)); }
    public String getLogicInfo(String logicId) {
        return moePost("/moe/get/getLogicInfo", obj("objectId",logicId)); }
    public String deleteLogic(String objectId, String logicStructureId) {
        return moePost("/moe/delete/deleteLogic", obj("objectId",objectId,"logicStructureId",logicStructureId)); }
    public String deleteLogicStructure(String objectId) {
        return moePost("/moe/delete/deleteLogicStructure", obj("objectId",objectId)); }
    public String updateLogicStructure(String objectId, String name, String description) {
        return moePost("/moe/update/updateLogicStructure", obj("objectId",objectId,"name",nvl(name),"description",nvl(description))); }
    public String updateLogicCurrent(String objectId, String after) {
        return moePost("/moe/update/updateCurrent", obj("objectId",objectId,"after",after)); }
    public String updateLogic(String objectId, String name, String description, String deviceCode, String logicStructureId) {
        return moePost("/moe/update/updateLogic", obj("objectId",objectId,"name",nvl(name),"description",nvl(description),"deviceCode",nvl(deviceCode,"默认设备编码"),"logicStructureId",logicStructureId)); }
    public String addLogicStructureParameter(String parentId, String name, String description, String logicStructureId) {
        String oid=UUID.randomUUID().toString().replace("-","").substring(0,10);
        JsonObject constraints=new JsonObject();
        constraints.addProperty("type","文本"); constraints.add("value",new JsonArray());
        return moePost("/moe/add/addLogicStructureParameter", obj("objectId",oid,"parentId",parentId,"name",name,"description",nvl(description,"自动测试"),"parameterUnit","","indexValue","","constraints",constraints,"logicStructureId",logicStructureId,"type","index","addMark",true)); }
    public String updateLogicStructureParameter(String objectId, String name, String description, String unit, String indexValue, String constraintType, String logicStructureId) {
        JsonObject constraints=new JsonObject();
        constraints.addProperty("type",nvl(constraintType,"文本")); constraints.add("value",new JsonArray());
        return moePost("/moe/update/updateLogicStructureParameter", obj("objectId",objectId,"name",nvl(name),"description",nvl(description),"parameterUnit",nvl(unit),"indexValue",nvl(indexValue),"constraints",constraints,"logicStructureId",logicStructureId)); }
    // 复制逻辑节点(全量JSON发addLogic)
    public String copyLogic(String logicJson, String parentId, String newName, String logicStructureId) {
        JsonObject node=JsonParser.parseString(logicJson).getAsJsonObject().getAsJsonObject("data");
        node.addProperty("name",newName); node.addProperty("title",newName);
        node.addProperty("parentId",nvl(parentId)); node.addProperty("parentType",node.has("type")?node.get("type").getAsString():"system");
        node.addProperty("logicStructureId",logicStructureId); node.addProperty("addMark",true);
        node.remove("objectId"); String oid=UUID.randomUUID().toString().replace("-","").substring(0,10); node.addProperty("objectId",oid);
        return moePost("/moe/add/addLogic", node); }
    // 导出架构Excel
    public APIResponse downloadExcelLogic(String structureId) {
        return request.post(MOE_BASE+"/moe/download/downloadExcelLogic", RequestOptions.create().setHeader("Content-Type","application/json").setData("{\"objectId\":\""+structureId+"\"}")); }
    // 导出指标Excel
    public APIResponse downloadExcelAIndex(String logicId) {
        return request.post(MOE_BASE+"/moe/download/downloadExcelAIndex", RequestOptions.create().setHeader("Content-Type","application/json").setData("{\"objectId\":\""+logicId+"\"}")); }
    // 下载指标模板
    public APIResponse downloadMetricTemplateExcel(String type) {
        return request.post(MOE_BASE+"/moe/download/downloadMetricTemplateExcel", RequestOptions.create().setHeader("Content-Type","application/json").setData("{\"type\":\""+type+"\"}")); }

    // ═══════════════════════ Internal HTTP ═══════════════════════

    private String post(String path, JsonObject body) {
        String text = doPost(path, body);
        if (is401(text) && reLogin != null) {
            log.info("Token expired, re-logging in...");
            reLogin.run();
            text = doPost(path, body);
        }
        return text;
    }

    private String doPost(String path, JsonObject body) {
        long t0 = System.currentTimeMillis();
        APIResponse resp = request.post(P + path,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(body.toString()));
        String text = resp.text();
        long ms = System.currentTimeMillis() - t0;
        int code = resp.status();
        if (code >= 400 || text.contains("\"code\":500")) {
            log.warn("API POST {} → HTTP {} ({}ms) body: {}", path, code, ms, truncate(text, 200));
        } else {
            log.info("API POST {} → HTTP {} ({}ms)", path, code, ms);
        }
        return text;
    }

    /** Like doPost but treats code:500 as info — for cleanup calls where business rejections are expected. */
    private String doPostCleanup(String path, JsonObject body) {
        long t0 = System.currentTimeMillis();
        APIResponse resp = request.post(P + path,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(body.toString()));
        String text = resp.text();
        long ms = System.currentTimeMillis() - t0;
        int code = resp.status();
        if (code >= 400) {
            log.warn("API POST {} → HTTP {} ({}ms) body: {}", path, code, ms, truncate(text, 200));
        } else if (text.contains("\"code\":200")) {
            log.info("API POST {} → HTTP {} ({}ms)", path, code, ms);
        }
        // code:500 during cleanup is expected, silently skipped
        return text;
    }

    private String postCleanup(String path, JsonObject body) {
        return doPostCleanup(path, body);
    }

    private String postRaw(String path, String json) {
        String text = doPostRaw(path, json);
        if (is401(text) && reLogin != null) {
            reLogin.run();
            text = doPostRaw(path, json);
        }
        return text;
    }

    private String doPostRaw(String path, String json) {
        long t0 = System.currentTimeMillis();
        APIResponse resp = request.post(P + path,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(json));
        String text = resp.text();
        long ms = System.currentTimeMillis() - t0;
        log.info("API POST {} → HTTP {} ({}ms)", path, resp.status(), ms);
        return text;
    }

    private String put(String path, JsonObject body) {
        String text = doPut(path, body);
        if (is401(text) && reLogin != null) {
            reLogin.run();
            text = doPut(path, body);
        }
        return text;
    }

    private String doPut(String path, JsonObject body) {
        long t0 = System.currentTimeMillis();
        APIResponse resp = request.put(P + path,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(body.toString()));
        String text = resp.text();
        long ms = System.currentTimeMillis() - t0;
        log.info("API PUT {} → HTTP {} ({}ms)", path, resp.status(), ms);
        return text;
    }

    private String get(String path, String... kv) {
        String text = doGet(path, kv);
        if (is401(text) && reLogin != null) {
            reLogin.run();
            text = doGet(path, kv);
        }
        return text;
    }

    private String doGet(String path, String... kv) {
        long t0 = System.currentTimeMillis();
        RequestOptions opts = RequestOptions.create();
        for (int i = 0; i < kv.length; i += 2) {
            if (kv[i + 1] != null) opts.setQueryParam(kv[i], kv[i + 1]);
        }
        APIResponse resp = request.get(P + path, opts);
        String text = resp.text();
        long ms = System.currentTimeMillis() - t0;
        log.info("API GET {} → HTTP {} ({}ms)", path, resp.status(), ms);
        return text;
    }

    private String get(String fullUrl) {
        String text = doGetUrl(fullUrl);
        if (is401(text) && reLogin != null) {
            reLogin.run();
            text = doGetUrl(fullUrl);
        }
        return text;
    }

    private String doGetUrl(String fullUrl) {
        long t0 = System.currentTimeMillis();
        APIResponse resp = request.get(fullUrl);
        String text = resp.text();
        long ms = System.currentTimeMillis() - t0;
        log.info("API GET {} → HTTP {} ({}ms)", fullUrl, resp.status(), ms);
        return text;
    }

    private boolean is401(String text) {
        return text != null && (text.contains("\"code\":401") || text.contains("\"code\": 401"));
    }

    // ═══════════════════════ Internal utils ═══════════════════════

    private static JsonObject obj(Object... kv) {
        JsonObject o = new JsonObject();
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof String) o.addProperty((String) kv[i], (String) v);
            else if (v instanceof Number) o.addProperty((String) kv[i], (Number) v);
            else if (v instanceof Boolean) o.addProperty((String) kv[i], (Boolean) v);
            else if (v instanceof JsonArray) o.add((String) kv[i], (JsonArray) v);
        }
        return o;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static String str(JsonObject o, String key1, String key2) {
        String v = str(o, key1);
        return v.isEmpty() ? str(o, key2) : v;
    }

    private static String nvl(String s) { return s == null ? "" : s; }
    private static String nvl(String s, String def) { return s == null || s.isEmpty() ? def : s; }
    private static String suf() { return UUID.randomUUID().toString().substring(0, 6); }
    private static String truncate(String s, int max) { return s != null && s.length() > max ? s.substring(0, max) + "..." : s; }

    private JsonArray dataArr(String json) {
        try {
            JsonObject r = JsonParser.parseString(json).getAsJsonObject();
            if (r.get("code").getAsInt() != 200) return null;
            JsonElement d = r.get("data");
            if (d instanceof JsonArray) return d.getAsJsonArray();
            if (d instanceof JsonObject) {
                JsonObject o = d.getAsJsonObject();
                for (String k : new String[]{"list", "records", "rows"})
                    if (o.has(k) && o.get(k) instanceof JsonArray) return o.getAsJsonArray(k);
            }
        } catch (Exception e) { log.warn("dataArr failed: {}", e.getMessage()); }
        return null;
    }
}
