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

    public ReqApiActions(APIRequestContext request) { this.request = request; }

    // ==================== Project ====================
    public String searchProjectByUser() { return post(P + "/project/search/searchProjectByUser", obj()); }
    public String getProjectIdByName(String projectName) {
        String resp = searchProjectByUser();
        JsonArray arr = dataArr(resp);
        if (arr != null) for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            if (projectName.equals(str(o, "name"))) return str(o, "id");
        }
        throw new RuntimeException("Project not found: " + projectName);
    }

    // ==================== Folder CRUD ====================
    public String createFolder(String projectId, String parentId) {
        JsonObject b = obj("projectId", projectId, "parentId", parentId, "name", "AT_Folder", "description", "auto", "type", "FOLDER");
        return post(P + "/reqFolder/add/addFolder", b);
    }
    public String renameFolder(String projectId, String folderId, String parentId, String newName) {
        JsonObject b = obj("projectId", projectId, "objectId", folderId, "parentId", parentId, "name", newName, "description", "auto");
        return post(P + "/reqFolder/update/updateFolder", b);
    }
    public String deleteFolder(String folderId, String projectId, String type) {
        JsonObject b = obj("objectId", folderId, "projectId", projectId, "action", "delete");
        if (type != null) b.addProperty("type", type);
        return post(P + "/reqFolder/delete/deleteFolder", b);
    }
    // Overload for internal use
    public String deleteFolder(String folderId, String projectId) {
        return deleteFolder(folderId, projectId, null);
    }
    public String recoverFolder(String folderId, String projectId) {
        return post(P + "/reqFolder/recover/recoverFolder", obj("objectId", folderId, "projectId", projectId));
    }
    public String forceCleanFolder(String folderId) {
        return post(P + "/reqFolder/clean/cleanFolder", obj("objectId", folderId, "action", "forceClean"));
    }
    public void cleanFolderByName(String projectId, String folderName) {
        String resp = searchFolderChildren(projectId, projectId);
        JsonArray arr = dataArr(resp);
        if (arr != null) for (JsonElement e : arr) {
            JsonObject f = e.getAsJsonObject();
            if (folderName.equals(str(f, "name")) && "FOLDER".equals(str(f, "type")))
                try { deleteFolder(str(f, "id"), projectId); forceCleanFolder(str(f, "id")); } catch (Exception ignored) {}
        }
    }
    public void sweepATFolders(String projectId) {
        String resp = searchFolderChildren(projectId, projectId);
        JsonArray arr = dataArr(resp);
        if (arr != null) for (JsonElement e : arr) {
            JsonObject f = e.getAsJsonObject();
            String name = str(f, "name");
            if (name.startsWith("AT_") && "FOLDER".equals(str(f, "type")))
                try { deleteFolder(str(f, "id"), projectId); forceCleanFolder(str(f, "id")); } catch (Exception ignored) {}
        }
    }
    public void cleanAllUnderRoot(String projectId) {
        String resp = searchFolderChildren(projectId, projectId);
        JsonArray arr = dataArr(resp);
        if (arr != null) for (JsonElement e : arr) {
            JsonObject f = e.getAsJsonObject();
            if ("FOLDER".equals(str(f, "type")))
                try { deleteFolder(str(f, "id"), projectId); forceCleanFolder(str(f, "id")); } catch (Exception ignored) {}
        }
    }

    // ==================== Document CRUD ====================
    public String createDocument(String projectId, String parentId) {
        JsonObject b = obj("projectId", projectId, "parentId", parentId, "name", "AT_Doc", "description", "auto", "type", "DOCUMENT");
        return post(P + "/reqSpec/add/addDocument", b);
    }
    public String renameDocument(String projectId, String docId, String parentId, String newName) {
        JsonObject b = obj("projectId", projectId, "objectId", docId, "parentId", parentId, "name", newName, "description", "auto");
        return post(P + "/reqSpec/update/updateDocument", b);
    }
    public String deleteDocument(String docId, String parentId) {
        return post(P + "/reqSpec/delete/deleteDocument", obj("objectId", docId, "parentId", parentId, "action", "delete"));
    }
    public String recoverDocument(String docId, String parentId) {
        return post(P + "/reqSpec/recover/recoverDocument", obj("objectId", docId, "parentId", parentId));
    }
    public String forceCleanDocument(String docId, String parentId) {
        return post(P + "/reqSpec/clean/cleanDocument", obj("objectId", docId, "parentId", parentId, "action", "forceClean"));
    }
    public String cleanDocument(String docId, String parentId) { return forceCleanDocument(docId, parentId); }

    // ==================== Req Item CRUD ====================
    public String addReqItem(String projectId, String parentId, String docId) {
        return addReqItemRaw(projectId, parentId, docId);
    }
    public String addReqItemRaw(String projectId, String parentId, String docId) {
        JsonObject b = obj("projectId", projectId, "parentId", parentId, "docId", docId,
            "name", "AT_RI_" + UUID.randomUUID().toString().substring(0,6), "description", "auto", "level", "1");
        return post(P + "/reqItem/add/reqItemBatch", b);
    }
    public String deleteReqItem(String itemId) {
        return post(P + "/reqItem/delete/reqItems", obj("objectId", itemId, "action", "delete"));
    }
    public String recoverReqItem(String itemId) {
        return post(P + "/reqItem/recover/reqItems", obj("objectId", itemId));
    }
    public String cleanReqItem(String itemId, String docId) {
        return post(P + "/reqItem/clean/reqItems", obj("objectId", itemId, "parentId", docId, "action", "forceClean"));
    }
    public String searchChildReqInfo(String objectId) {
        return post(P + "/reqItem/search/searchChildReqInfo", obj("objectId", objectId));
    }
    public String updateReqList(String docId, String json) {
        JsonObject b = new JsonObject();
        b.addProperty("docId", docId);
        b.add("reqList", JsonParser.parseString(json));
        return request.post(P + "/reqItem/update/updateReqList",
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(b.toString())).text();
    }
    public String editDescription(String projectId, String docId, String folderId, String description) {
        return post(P + "/reqItem/update/editDescription",
            obj("projectId", projectId, "objectId", docId, "parentId", folderId, "description", description));
    }

    // ==================== Search / Tree ====================
    public String searchFolderChildren(String parentId) {
        return post(P + "/reqFolder/search/searchFolderChildren", obj("parentId", parentId));
    }
    // Overload for internal use
    public String searchFolderChildren(String projectId, String parentId) {
        return post(P + "/reqFolder/search/searchFolderChildren", obj("projectId", projectId, "parentId", parentId));
    }
    public String getReqSpeList(String projectId) {
        return post(P + "/reqSpec/search/searchReqSpeList", obj("projectId", projectId));
    }
    public String getTree(String parentId, String projectId) {
        return post(P + "/reqItem/search/searchReqItemTree", obj("parentId", parentId, "projectId", projectId));
    }
    public String searchAttributes(String projectId, String bizDomain, String type) {
        JsonObject b = obj("projectId", projectId, "bizDomain", nvl(bizDomain, "req"));
        if (type != null && !type.isEmpty()) b.addProperty("type", type);
        return post(P + "/attribute/search/searchAttributes", b);
    }
    // Overload for internal use
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

    // ==================== Favorite ====================
    public String addFavorite(String projectId, String objectId, String type) {
        JsonArray ids = new JsonArray(); ids.add(objectId);
        JsonObject b = obj("projectId", projectId, "type", type, "addMark", true);
        b.add("objectIds", ids);
        return post(P + "/favorite/add/batchAddFavorites", b);
    }
    public String addFavorite(String projectId, String objectId, String type, String parentId) {
        JsonArray ids = new JsonArray(); ids.add(objectId);
        JsonObject b = obj("projectId", projectId, "type", type, "addMark", true, "parentId", nvl(parentId));
        b.add("objectIds", ids);
        return post(P + "/favorite/add/batchAddFavorites", b);
    }
    public String searchFavoriteList(String projectId) {
        return post(P + "/favorite/search/searchFavorites", obj("projectId", projectId, "type", "req_spec"));
    }
    public String deleteFavorite(String favoriteId) {
        return post(P + "/favorite/delete/deleteFavorites", obj("objectId", favoriteId));
    }

    // ==================== View ====================
    public String addView(String objectId, String name, String description, String columns) {
        JsonObject b = obj("objectId", objectId, "name", nvl(name, "AT_V_" + suf()),
            "description", nvl(description, "auto"));
        if (columns != null && !columns.isEmpty()) {
            JsonArray cols = new JsonArray();
            for (String c : columns.split(",")) cols.add(c.trim());
            b.add("columns", cols);
        }
        return post(P + "/view/add/addView", b);
    }
    public String searchViewList(String objectId) {
        return post(P + "/view/search/searchViewList", obj("objectId", objectId));
    }
    public String deleteView(String viewId) {
        return post(P + "/view/delete/deleteView", obj("objectId", viewId));
    }

    // ==================== Custom Attribute ====================
    public String addCustomAttribute(String nameEn, String name, String type, String projectId) {
        return post(P + "/attribute/add/addCustomAttribute",
            obj("nameEn", nameEn, "name", name, "type", type, "projectId", projectId, "description", "auto", "isRequired", false));
    }
    public String[] findCustomAttribute(String nameEn, String projectId) {
        String resp = getCustomAttributeList(projectId);
        JsonArray arr = dataArr(resp);
        if (arr != null) for (JsonElement e : arr) {
            JsonObject a = e.getAsJsonObject();
            if (nameEn.equals(str(a, "nameEn"))) return new String[]{str(a, "id"), str(a, "name"), str(a, "type")};
        }
        return null;
    }
    public String getCustomAttributeList(String projectId) {
        return post(P + "/attribute/search/searchCustomAttributeList", obj("projectId", projectId));
    }
    public String searchCustomAttribute(String projectId, String nameEn, String name, String type, String page, String pageSize) {
        JsonObject b = obj("projectId", projectId, "nameEn", nvl(nameEn), "name", nvl(name), "type", nvl(type));
        if (page != null) b.addProperty("page", page);
        if (pageSize != null) b.addProperty("pageSize", pageSize);
        return post(P + "/attribute/search/searchCustomAttributeList", b);
    }
    public String updateCustomAttribute(String objectId, String nameEn, String name, String type,
                                          String originalName, String originalType, String projectId) {
        JsonObject b = obj("objectId", objectId, "nameEn", nameEn, "name", name, "type", type, "projectId", projectId, "description", "auto");
        if (originalName != null) b.addProperty("originalName", originalName);
        if (originalType != null) b.addProperty("originalType", originalType);
        return post(P + "/attribute/update/updateCustomAttribute", b);
    }
    public String publishCustomAttribute(String objectId, String projectId) {
        return post(P + "/attribute/publish/publishCustomAttribute", obj("objectId", objectId, "projectId", projectId));
    }
    public String deleteCustomAttribute(String objectId) {
        return post(P + "/attribute/delete/deleteCustomAttribute", obj("objectId", objectId));
    }
    public String batchDeleteCustomAttributes(String ids, String projectId) {
        // ids is a comma-separated string
        JsonArray arr = new JsonArray();
        for (String id : ids.split(",")) arr.add(id.trim());
        JsonObject b = new JsonObject(); b.add("ids", arr);
        return post(P + "/attribute/delete/batchDeleteAttributes", b);
    }

    // ==================== Export / Import ====================
    public APIResponse exportExcel(String objectId, String templateId) {
        JsonObject b = obj("objectId", objectId, "templateId", nvl(templateId));
        return request.post(P + "/export/exportExcel",
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(b.toString()));
    }
    public APIResponse exportWord(String objectId, String templateId) {
        JsonObject b = obj("objectId", objectId, "templateId", nvl(templateId));
        return request.post(P + "/export/exportWord",
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(b.toString()));
    }
    public String exportReqIf(String payload) {
        return request.post(P + "/export/exportReqIf",
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(payload)).text();
    }
    public String getAllAtozParam(String projectId) {
        return post(P + "/export/getAtoZParams", obj("projectId", projectId));
    }
    public APIResponse downloadImportTemplate(String type) {
        JsonObject b = obj("type", type);
        return request.post(P + "/export/downloadTemplate",
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(b.toString()));
    }
    public String getImportAttributes() {
        return post(P + "/import/getImportAttrList", obj());
    }
    public String importReqSpecification(String projectId, String parentId, String name, String jsonData) {
        return request.post(P + "/import/importData",
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(jsonData)).text();
    }

    // ==================== Cooperation Area ====================
    public String addCooperationArea(String name, String code, String securityLevel, String description) {
        return post(P + "/cooperationArea/add/addCooperationArea",
            obj("name", name, "code", code, "securityLevel", nvl(securityLevel, "内部"), "description", nvl(description, "auto")));
    }
    public String updateCooperationArea(String areaId, String name, String code, String securityLevel, String description) {
        return post(P + "/cooperationArea/update/updateCooperationArea",
            obj("objectId", areaId, "name", name, "code", code, "securityLevel", nvl(securityLevel, "内部"), "description", nvl(description)));
    }
    public String deleteCooperationArea(String areaId) {
        return post(P + "/cooperationArea/delete/deleteCooperationArea", obj("objectId", areaId, "action", "delete"));
    }
    public String searchCooperationAreaList(String keyword, String projectId) {
        return post(P + "/cooperationArea/search/searchCooperationAreaList", obj("keyword", nvl(keyword), "projectId", projectId));
    }
    public String addCooperationAreaUser(String areaId, String userId) {
        return post(P + "/cooperationArea/add/addCooperationAreaUser", obj("areaId", areaId, "userId", userId));
    }
    public String deleteCooperationAreaUser(String areaId, String userId) {
        return post(P + "/cooperationArea/delete/deleteCooperationAreaUser", obj("areaId", areaId, "userId", userId));
    }

    // ==================== Project Personnel ====================
    public String searchProjectList(String keyword, String page) {
        JsonObject b = obj("keyword", nvl(keyword));
        if (page != null && !page.isEmpty()) b.addProperty("page", page);
        return post(P + "/project/search/searchProjectList", b);
    }
    public String searchProjectPersonList(String objectId) {
        return post(P + "/projectPersonnel/search/searchProjectPersonnelList", obj("objectId", objectId));
    }
    public String deptTree() {
        return post(P + "/projectPersonnel/search/searchDeptTree", obj());
    }
    public String listUsersWithoutAdmins(int page, int pageSize, String keyword, String deptId) {
        JsonObject b = obj("page", page, "pageSize", pageSize, "keyword", nvl(keyword));
        if (deptId != null && !deptId.isEmpty()) b.addProperty("deptId", deptId);
        return post(P + "/projectPersonnel/search/searchNonAdminUserList", b);
    }
    public String assignProjectPersonList(String objectId, String data) {
        return request.post(P + "/projectPersonnel/add/assignProjectPersonnel",
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(data)).text();
    }

    // ==================== System User (sysUser*) ====================
    public String sysUserList(int page, int pageSize, String userName, String phone, String status) {
        JsonObject b = obj("page", page, "pageSize", pageSize);
        if (userName != null && !userName.isEmpty()) b.addProperty("userName", userName);
        if (phone != null && !phone.isEmpty()) b.addProperty("phonenumber", phone);
        if (status != null && !status.isEmpty()) b.addProperty("status", status);
        return post(P + "/systemUser/search/searchSystemUserList", b);
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
            JsonArray pa = new JsonArray(); for (String s : postIds.split(",")) pa.add(s.trim());
            b.add("postIds", pa);
        }
        if (roleIds != null) {
            JsonArray ra = new JsonArray(); for (String s : roleIds.split(",")) ra.add(s.trim());
            b.add("roleIds", ra);
        }
        return post(P + "/systemUser/add/addSystemUser", b);
    }
    public String sysUserGetById(String userId) {
        return post(P + "/systemUser/get/getSystemUserInfo", obj("objectId", userId));
    }
    public String sysUserUpdate(String json) {
        return request.post(P + "/systemUser/update/updateSystemUser",
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(json)).text();
    }
    public String sysUserDelete(String userId) {
        return post(P + "/systemUser/delete/deleteSystemUser", obj("objectId", userId, "action", "delete"));
    }
    public String sysUserResetPwd(String userId, String password) {
        return post(P + "/systemUser/update/resetPassword", obj("userId", userId, "password", password));
    }
    public APIResponse sysUserExport() {
        return request.post(P + "/systemUser/export/exportSystemUsers",
            RequestOptions.create().setHeader("Content-Type", "application/json").setData("{}"));
    }
    public APIResponse sysUserImportTemplate() {
        return request.post(P + "/systemUser/export/downloadUserImportTemplate",
            RequestOptions.create().setHeader("Content-Type", "application/json").setData("{}"));
    }
    public String sysUserDeptTree() {
        return post(P + "/systemUser/search/searchDeptTree", obj());
    }
    public String sysDictData(String dictType) {
        return post(P + "/dict/search/searchDictDataList", obj("dictType", dictType));
    }
    public String searchUser(String userName) {
        return post(P + "/systemUser/search/searchSystemUserList", obj("userName", nvl(userName), "page", 1, "pageSize", 10));
    }
    public String resetPassword(String userId, String password) {
        return post(P + "/systemUser/update/resetPassword", obj("userId", userId, "password", password));
    }
    public String importUser(String json) {
        return request.post(P + "/systemUser/import/importSystemUsers",
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(json)).text();
    }

    // ==================== System Post (sysPost*) ====================
    public String sysPostList(int page, int pageSize, String postCode, String postName, String status) {
        JsonObject b = obj("page", page, "pageSize", pageSize);
        if (postCode != null && !postCode.isEmpty()) b.addProperty("postCode", postCode);
        if (postName != null && !postName.isEmpty()) b.addProperty("postName", postName);
        if (status != null && !status.isEmpty()) b.addProperty("status", status);
        return post(P + "/systemPost/search/searchSystemPostList", b);
    }
    public String sysPostCreate(String postName, String postCode, int postSort, String status, String remark) {
        JsonObject b = obj("postName", postName, "postCode", postCode, "postSort", postSort,
            "status", nvl(status, "0"), "remark", nvl(remark));
        return post(P + "/systemPost/add/addSystemPost", b);
    }
    public String sysPostUpdate(String json) {
        return request.post(P + "/systemPost/update/updateSystemPost",
            RequestOptions.create().setHeader("Content-Type", "application/json").setData(json)).text();
    }
    public String sysPostGetById(String postId) {
        return post(P + "/systemPost/get/getSystemPostInfo", obj("objectId", postId));
    }
    public String sysPostDelete(String postId) {
        return post(P + "/systemPost/delete/deleteSystemPost", obj("objectId", postId, "action", "delete"));
    }
    public APIResponse sysPostExport() {
        return request.post(P + "/systemPost/export/exportSystemPosts",
            RequestOptions.create().setHeader("Content-Type", "application/json").setData("{}"));
    }

    // ==================== Version Trace ====================
    public String getVersionList(String objectId) {
        return post(P + "/version/search/searchVersionList", obj("objectId", objectId));
    }
    public String getReqAccess(String objectId) {
        return post(P + "/version/get/getReqAccessPermission", obj("objectId", objectId));
    }
    public String searchReqSpecTrace(String objectId, String type) {
        return post(P + "/version/search/searchReqTrace", obj("objectId", objectId, "type", nvl(type)));
    }
    public String searchChangeAnalysis(String objectId, String versionId) {
        return post(P + "/version/get/getChangeAnalysis", obj("objectId", objectId, "versionId", nvl(versionId)));
    }
    public String checkOpenMode(String docId, String operateType, String openPerson) {
        return post(P + "/version/check/checkOpenMode",
            obj("masterId", docId, "operateType", nvl(operateType, "check"), "openPerson", nvl(openPerson, "admin")));
    }
    public String getOpenModel(String masterId, String operateType, String openPerson) {
        return post(P + "/version/check/checkOpenMode",
            obj("masterId", masterId, "operateType", nvl(operateType, "check"), "openPerson", nvl(openPerson)));
    }
    public String unlockMode(String masterId, String unlockMode, String unlockPerson) {
        return post(P + "/version/unlock/unlockMode",
            obj("masterId", masterId, "unlockMode", nvl(unlockMode, "unlock"), "unlockPerson", nvl(unlockPerson, "admin")));
    }

    // ==================== Helpers ====================
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

    // ==================== Internal ====================
    private String post(String url, JsonObject body) {
        return request.post(url, RequestOptions.create().setHeader("Content-Type", "application/json").setData(body.toString())).text();
    }
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
    private static String nvl(String s) { return s == null ? "" : s; }
    private static String nvl(String s, String def) { return s == null || s.isEmpty() ? def : s; }
    private static String suf() { return UUID.randomUUID().toString().substring(0, 6); }
    private JsonArray arrFromCsv(String csv) {
        JsonArray a = new JsonArray();
        if (csv != null && !csv.isEmpty()) for (String s : csv.split(",")) a.add(s.trim());
        return a;
    }
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
