package actions;

import com.google.gson.*;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import config.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final String ERM_EXPORT_EXCEL   = "/erm/export/exportExcel";
    private static final String ERM_EXPORT_WORD    = "/erm/export/exportWord";
    private static final String ERM_EXPORT_REQIF   = "/erm/export/exportReqIf";
    private static final String ERM_GET_ATOZ       = "/erm/export/getAtoZParams";
    private static final String ERM_DOWNLOAD_TPL   = "/erm/export/downloadTemplate";
    private static final String ERM_IMPORT_ATTR    = "/erm/import/getImportAttrList";
    private static final String ERM_IMPORT_EXCEL   = "/erm/import/importReqSpecification";

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
    private static final String COOP_ADD    = "/erm/add/addProject";
    private static final String COOP_UPDATE = "/erm/update/updateProjectInfo";
    private static final String COOP_DEL    = "/erm/del/delProject";
    private static final String COOP_SEARCH = "/common/search/searchProjectList";
    private static final String COOP_ADD_USER = "/common/update/assignProjectPersonList";
    private static final String COOP_DEL_USER = "/erm/update/removeProjectPersonList";

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

    public String deleteFolder(String folderId, String projectId) {
        return deleteFolder(folderId, projectId, null);
    }

    public String recoverFolder(String folderId, String parentId) {
        String pid = nvl(parentId, folderId);
        return post(ERM_RECOVER_FOLDER, obj("objectId", folderId, "parentId", pid,
            "parentType", "reqSpeFolder"));
    }

    public String forceCleanFolder(String folderId) {
        post(ERM_RECOVER_FOLDER, obj("objectId", folderId, "parentId", folderId));
        return post(ERM_CLEAN_FOLDER, obj("objectId", folderId));
    }

    public void forceCleanDocument(String docId, String parentId) {
        post(ERM_RECOVER_DOC, obj("objectId", docId, "parentId", parentId, "parentType", PARENT_TYPE_FOLDER));
        post(ERM_CLEAN_DOC, obj("objectId", docId, "parentId", parentId, "parentType", PARENT_TYPE_FOLDER));
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
        log.info("\n====== 清理环境结束 ======");
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
                    if ("reqSpe".equals(type)) { deleteDocument(objectId, parentId); forceCleanDocument(objectId, parentId); }
                    else if (PARENT_TYPE_FOLDER.equals(type)) { deleteFolder(objectId, parentId, PARENT_TYPE_FOLDER); forceCleanFolder(objectId); }
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
                    if ("reqSpe".equals(type)) { deleteDocument(objectId, parentId); forceCleanDocument(objectId, parentId); }
                    else if ("req".equals(type)) { deleteReqItem(objectId); }
                    else if (PARENT_TYPE_FOLDER.equals(type)) { deleteFolder(objectId, parentId, PARENT_TYPE_FOLDER); forceCleanFolder(objectId); }
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
        return addReqItemRaw(projectId, parentId, docId);
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

    public String searchChildReqInfo(String objectId) {
        return post(ERM_SEARCH_CHILD_REQ, obj("objectId", objectId));
    }

    public String updateReqList(String docId, String json) {
        JsonObject b = new JsonObject();
        b.addProperty("docId", docId);
        b.add("reqList", JsonParser.parseString(json));
        return request.post(P + ERM_UPDATE_REQ_LIST,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(b.toString())).text();
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
        String resp = getTree(parentId, "");
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
        return extractId(post(ERM_ADD_VIEW, b));
    }

    public String searchViewList(String objectId) {
        return post(ERM_SEARCH_VIEW, obj("objectId", objectId));
    }

    public String deleteView(String viewId) {
        return post(ERM_DEL_VIEW, obj("objectId", viewId));
    }

    // ═══════════════════════ Custom Attribute ═══════════════════════

    public String addCustomAttribute(String nameEn, String name, String type, String projectId) {
        return post(ERM_ATTR_ADD, obj("nameEn", nameEn, "name", name, "type", type,
            "projectId", projectId, "description", "auto", "isRequired", false));
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
        return post(ERM_ATTR_PUBLISH, obj("id", id, "projectId", projectId));
    }

    public String deleteCustomAttribute(String id) {
        JsonArray arr = new JsonArray(); arr.add(id);
        return postRaw(ERM_ATTR_DELETE, "{\"ids\": [\"" + id + "\"]}");
    }

    public String batchDeleteCustomAttributes(String... ids) {
        StringBuilder sb = new StringBuilder("{\"ids\": [");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(ids[i]).append("\"");
        }
        sb.append("]}");
        return postRaw(ERM_ATTR_DELETE, sb.toString());
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
        JsonObject b = obj("objectId", objectId, "templateId", nvl(templateId));
        return request.post(P + ERM_EXPORT_EXCEL,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(b.toString()));
    }

    public APIResponse exportWord(String objectId, String templateId) {
        JsonObject b = obj("objectId", objectId, "templateId", nvl(templateId));
        return request.post(P + ERM_EXPORT_WORD,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(b.toString()));
    }

    public String exportReqIf(String payload) {
        return request.post(P + ERM_EXPORT_REQIF,
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(payload)).text();
    }

    public String getAllAtozParam(String projectId) {
        return post(ERM_GET_ATOZ, obj("projectId", projectId));
    }

    public APIResponse downloadImportTemplate(String type) {
        return request.post(P + ERM_DOWNLOAD_TPL,
            RequestOptions.create().setHeader("Content-Type", "application/json")
                .setData(obj("type", type).toString()));
    }

    public String getImportAttributes() {
        return post(ERM_IMPORT_ATTR, obj());
    }

    public String importReqSpecification(String projectId, String parentId, String reqSpecName, String dataJson) {
        return post(ERM_IMPORT_EXCEL,
            obj("projectId", projectId, "reqSpeParentId", parentId, "reqSpeName", reqSpecName, "dataJson", dataJson));
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
        return post(COOP_ADD, obj("title", name, "name", code,
            "securityLevel", nvl(securityLevel, "内部"), "description", nvl(description, "auto")));
    }

    public String updateCooperationArea(String areaId, String name, String code, String securityLevel, String description) {
        return post(COOP_UPDATE, obj("objectId", areaId, "title", name, "name", code,
            "securityLevel", nvl(securityLevel, "内部"), "description", nvl(description)));
    }

    public String deleteCooperationArea(String areaId) {
        return post(COOP_DEL, obj("objectId", areaId));
    }

    public String searchCooperationAreaList(String keyword, String projectId) {
        return get(COOP_SEARCH, "title", nvl(keyword), "originated", "");
    }

    public String addCooperationAreaUser(String areaId, String userId) {
        JsonArray users = new JsonArray();
        users.add(userId);
        return post(COOP_ADD_USER, obj("objectId", areaId, "data", users));
    }

    public String deleteCooperationAreaUser(String areaId, String userId) {
        JsonArray users = new JsonArray();
        users.add(userId);
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
