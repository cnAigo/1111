package cases.version_trace;

import base.ApiTestHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("VersionTraceModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UnlockApiTest extends ApiTestHelper {

    @Test
    @DisplayName("检查打开模式(正向)")
    void test_checkOpenMode() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.checkOpenMode(docId, "dblClick", "admin");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "检查打开模式应成功, resp: " + resp);
            log.info("检查打开模式 通过: masterId={}", docId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("解锁模式(正向)")
    void test_unlockMode() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.unlockMode(docId, "unlock", "admin");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "解锁应成功, resp: " + resp);
            log.info("解锁模式 通过: objectId={}", docId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("解锁模式-强制解锁(正向)")
    void test_unlockModeForce() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.unlockMode(docId, "forceUnlock", "admin");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "强制解锁应成功, resp: " + resp);
            log.info("强制解锁 通过: objectId={}", docId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("检查打开模式-无效masterId(负向)")
    void test_checkOpenModeInvalidId() {
        String resp = api.checkOpenMode("invalid_id_99999", "dblClick", "admin");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("检查打开模式-无效ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("检查打开模式-空operateType(负向)")
    void test_checkOpenModeEmptyType() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.checkOpenMode(docId, "", "admin");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("检查打开模式-空操作类型: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("检查打开模式-空openPerson(负向)")
    void test_checkOpenModeEmptyPerson() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.checkOpenMode(docId, "dblClick", "");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("检查打开模式-空操作人: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("解锁模式-非法unlockMode(负向)")
    void test_unlockModeInvalid() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.unlockMode(docId, "invalidMode", "admin");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("解锁模式-非法模式: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("解锁模式-无效objectId(负向)")
    void test_unlockModeInvalidId() {
        String resp = api.unlockMode("invalid_id_99999", "unlock", "admin");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("解锁模式-无效ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("解锁模式-空解锁人(负向)")
    void test_unlockModeEmptyPerson() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.unlockMode(docId, "unlock", "");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("解锁模式-空解锁人: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }
}
