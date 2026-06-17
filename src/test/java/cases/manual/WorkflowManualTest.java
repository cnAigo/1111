package cases.manual;

import base.ApiTestHelper;
import org.junit.jupiter.api.*;

/**
 * 需求审签 — 按操作手册 56-66。
 */
@Tag("ReqFolderModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WorkflowManualTest extends ApiTestHelper {

    @Test @DisplayName("49. 另存为草稿")
    void test_4901() {
        log.info("49. 另存为草稿: TODD - 草稿存成冻结中, 启动后变审批中");
    }

    @Test @DisplayName("50. 不存草稿直接启动审批")
    void test_5001() {
        log.info("50. 直接启动审批: TODD - 启动后不可删除不可修改");
    }

    @Test @DisplayName("51. 启动草稿")
    void test_5101() {
        log.info("51. 启动草稿: TODD");
    }

    @Test @DisplayName("52. 审批同意")
    void test_5201() {
        log.info("52. 审批同意: TODD - 需要第二账号");
    }

    @Test @DisplayName("53. 审批不同意")
    void test_5301() {
        log.info("53. 审批不同意: TODD - 不同意后恢复工作中编辑状态");
    }

    @Test @DisplayName("54. 审批完成后升版")
    void test_5401() {
        log.info("54. 审批完成后升版: TODD - 发布后升版创建新版本");
    }

    // ═══ 62. 基线 → FolderManualTest ═══
    // ═══ 65-66. 收藏 → FolderManualTest ═══

    @Test @DisplayName("56. 更改单-另存为草稿")
    void test_5601() {
        log.info("56. 更改单草稿: TODD");
    }

    @Test @DisplayName("57. 更改审签发布")
    void test_5701() {
        log.info("57. 更改审签发布: TODD");
    }
}
