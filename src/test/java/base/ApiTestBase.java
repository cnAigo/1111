package base;

import actions.ReqApiActions;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import config.TestConfig;
import config.TestConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Base for API-only test classes. Creates a standalone {@link APIRequestContext}
 * without launching a browser — lighter and faster than UI test bases.
 */
public abstract class ApiTestBase {

    protected static final Logger log = LoggerFactory.getLogger(ApiTestBase.class);

    protected Playwright playwright;
    protected APIRequestContext apiContext;
    protected ReqApiActions api;
    protected String PROJECT_ID;

    /** 记录创建的资源ID，用于快速清理（避免查树） */
    private final List<String> createdIds = new ArrayList<>();

    @BeforeAll
    public void setupApi() {
        playwright = Playwright.create();
        apiContext = playwright.request().newContext(
                new APIRequest.NewContextOptions().setIgnoreHTTPSErrors(true));
        api = new ReqApiActions(apiContext);
        PROJECT_ID = resolveProjectId();
        log.info("API setup complete. PROJECT_ID={}, BASE_URL={}", PROJECT_ID, TestConfig.BASE_URL);
    }

    @AfterAll
    public void teardownApi() {
        try { if (apiContext != null) apiContext.dispose(); } catch (Exception ignored) {}
        try { if (playwright != null) playwright.close(); } catch (Exception ignored) {}
    }

    // ── Auth ──

    protected void loginViaApi() {
        try {
            AuthHelper.login(apiContext, TestConfig.ADMIN_USER, TestConfig.ADMIN_PWD);
            log.info("API login OK");
        } catch (Exception e) {
            log.error("API login failed: {}", e.getMessage());
        }
    }

    // ── Helpers ──

    protected String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    protected String resolveParentId() {
        return PROJECT_ID;
    }

    // ── Temp resource creation ──

    protected String[] createTempFolder() {
        String folderId = api.createFolder(PROJECT_ID, PROJECT_ID);
        String folderName = "AT_Folder_" + suffix();
        api.renameFolder(PROJECT_ID, folderId, PROJECT_ID, folderName);
        log.info("Created temp folder: {} ({})", folderName, folderId);
        return new String[]{folderId, folderName};
    }

    protected String[] createTempDoc() {
        String[] folder = createTempFolder();
        String folderId = folder[0];
        String docId = api.createDocument(PROJECT_ID, folderId);
        String docName = "AT_Doc_" + suffix();
        api.renameDocument(PROJECT_ID, docId, folderId, docName);
        // 记录：倒序清理（先删文档再删文件夹）
        createdIds.add("folder:" + folderId);
        createdIds.add("doc:" + docId + ":" + folderId);
        return new String[]{docId, docName, folderId};
    }

    protected String[] createTempDoc(String ignored) { return createTempDoc(); }
    protected String[] createTempDocFull(String ignored) { return createTempDoc(); }

    // ── Cleanup ──

    /** 按记录清理：先删文档再删文件夹，跳过查树（比 forceCleanFolder 快） */
    protected void cleanupAll() {
        // 倒序：doc在后→先删，folder在前→后删
        for (int i = createdIds.size() - 1; i >= 0; i--) {
            String entry = createdIds.get(i);
            try {
                if (entry.startsWith("doc:")) {
                    String[] parts = entry.substring(4).split(":", 2);
                    api.deleteDocumentCleanup(parts[0], parts[1]);
                    api.forceCleanDocument(parts[0], parts[1]);
                } else if (entry.startsWith("folder:")) {
                    String fid = entry.substring(7);
                    api.deleteFolderCleanup(fid, PROJECT_ID, "project");
                    api.deleteFolderCleanup(fid, PROJECT_ID, "reqSpeFolder");
                    api.forceCleanFolder(fid);
                }
            } catch (Exception ignored) {}
        }
        createdIds.clear();
    }

    /** 兜底清理：查树递归清理（仅在 cleanupAll 搞不定时用） */
    protected void forceCleanFolder(String folderId) {
        try { api.cleanFolderTree(folderId, PROJECT_ID); } catch (Exception ignored) {}
        try { api.deleteFolderCleanup(folderId, PROJECT_ID, "project"); } catch (Exception ignored) {}
        try { api.deleteFolderCleanup(folderId, PROJECT_ID, "reqSpeFolder"); } catch (Exception ignored) {}
        try { api.forceCleanFolder(folderId); } catch (Exception ignored) {}
    }

    protected void cleanupFolderByName(String folderName) {
        if (folderName == null) return;
        try {
            api.sweepATFolders(PROJECT_ID);
            log.info("Swept folders matching: {}", folderName);
        } catch (Exception e) {
            log.warn("Failed to sweep folders: {}", e.getMessage());
        }
    }

    protected void cleanupDoc(String docId, String parentId) {
        if (docId == null || parentId == null) return;
        try {
            api.deleteDocument(docId, parentId);
            api.forceCleanDocument(docId, parentId);
        } catch (Exception e) {
            log.warn("Failed to cleanup doc {}: {}", docId, e.getMessage());
        }
    }

    protected void cleanupByName(String name) {
        if (name == null) return;
        try { api.sweepATFolders(PROJECT_ID); } catch (Exception e) {
            log.warn("Failed to sweep: {}", e.getMessage());
        }
    }

    protected void cleanupCustomAttr(String nameEn) {
        try { if (nameEn != null) api.deleteCustomAttribute(nameEn); } catch (Exception ignored) {}
    }

    // ── Internal ──

    private String resolveProjectId() {
        String id = TestConstants.PROJECT_ID;
        if (id == null || id.isBlank()) {
            id = System.getenv("TAAS_PROJECT_ID");
            if (id == null || id.isBlank()) {
                try {
                    id = api.getProjectIdByName(TestConstants.PROJECT_NAME);
                } catch (Exception e) {
                    log.warn("Could not resolve project by name '{}', using fallback", TestConstants.PROJECT_NAME);
                    id = "2058851105448046592";
                }
            }
            TestConstants.PROJECT_ID = id;
        }
        return id;
    }
}
