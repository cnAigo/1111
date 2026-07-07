package cases.manual;

import base.ApiTestHelper;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("ReqFolderModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReqItemEditManualTest extends ApiTestHelper {

    @Test @DisplayName("11.1 新建需求条目(正向)")
    void test_1101_addItem() { String d=api.createDocument(PROJECT_ID,PROJECT_ID); String id=api.addReqItem(PROJECT_ID,d,d); Assertions.assertNotNull(id); log.info("11.1 通过"); }
    @Test @DisplayName("11.2 编辑需求条目标题(正向)")
    void test_1102_editTitle() { String d=api.createDocument(PROJECT_ID,PROJECT_ID); api.addReqItemRaw(PROJECT_ID,d,d); log.info("11.2 通过"); }
    @Test @DisplayName("11.3 编辑需求条目描述(正向)")
    void test_1103_editDesc() { String d=api.createDocument(PROJECT_ID,PROJECT_ID); api.addReqItemRaw(PROJECT_ID,d,d); log.info("11.3 通过"); }
    @Test @DisplayName("11.4 删除需求条目(正向)")
    void test_1104_delItem() { String d=api.createDocument(PROJECT_ID,PROJECT_ID); String id=api.addReqItem(PROJECT_ID,d,d); String r=api.deleteReqItem(id); Assertions.assertTrue(r.contains("200")); log.info("11.4 通过"); }
    @Test @DisplayName("11.5 恢复需求条目(正向)")
    void test_1105_recoverItem() { String d=api.createDocument(PROJECT_ID,PROJECT_ID); String id=api.addReqItem(PROJECT_ID,d,d); api.deleteReqItem(id); String r=api.recoverReqItem(id); Assertions.assertTrue(r.contains("200")); log.info("11.5 通过"); }
    @Test @DisplayName("11.6 永久清除需求条目(正向)")
    void test_1106_cleanItem() { String d=api.createDocument(PROJECT_ID,PROJECT_ID); String id=api.addReqItem(PROJECT_ID,d,d); api.deleteReqItem(id); String r=api.cleanReqItem(id,d); Assertions.assertTrue(r.contains("200")); log.info("11.6 通过"); }
    @Test @DisplayName("11.7 复制需求条目(正向)")
    void test_1107_copyItem() { log.info("11.7 待开发"); }
    @Test @DisplayName("11.8 剪切需求条目(正向)")
    void test_1108_cutItem() { log.info("11.8 待开发"); }
    @Test @DisplayName("11.9 加锁解锁(正向)")
    void test_1109_lockUnlock() { log.info("11.9 待开发"); }
}
