package cases.indicator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("IndicatorModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LogicApiTest extends IndicatorTestBase {

    private String logicStructureId;
    private String logicId;

    @BeforeAll
    void ensureStructure() {
        String resp = ind.addLogicStructure("AT_LogicTest_" + suffix(), "逻辑测试容器");
        logicStructureId = ind.extractId(resp);
        Assertions.assertNotNull(logicStructureId, "测试前需创建逻辑结构");
    }

    @Test
    @DisplayName("IND_101: 逻辑结构下新建逻辑项(正向)")
    void test_addLogic() {
        String nodeId = String.valueOf(System.currentTimeMillis() % 10000000000L);
        String resp = ind.addLogic(nodeId, "", "AT_Logic_" + suffix(), logicStructureId);
        Assertions.assertTrue(ind.isOk(resp), "新建应成功, resp: " + resp);
        logicId = ind.extractId(resp);
        log.info("IND_101 通过: 新建逻辑项 id={}", logicId);
    }

    @Test
    @DisplayName("IND_102: 查询逻辑列表(正向)")
    void test_searchLogicList() {
        String resp = ind.searchLogicList(logicStructureId);
        Assertions.assertTrue(ind.isOk(resp), "查询列表应成功, resp: " + resp);
        Assertions.assertTrue(resp.contains("data"), "应包含data字段");
        log.info("IND_102 通过: 查询逻辑列表成功");
    }

    @Test
    @DisplayName("IND_103: 修改逻辑项名称(正向)")
    void test_updateLogic() {
        String nodeId = String.valueOf(System.currentTimeMillis() % 10000000000L);
        String addResp = ind.addLogic(nodeId, "", "AT_BeforeRename_" + suffix(), logicStructureId);
        String id = ind.extractId(addResp);
        Assertions.assertNotNull(id, "新建应返回ID");

        String resp = ind.updateLogic(id, "AT_AfterRename_" + suffix(), logicStructureId);
        Assertions.assertTrue(ind.isOk(resp), "修改应成功, resp: " + resp);
        log.info("IND_103 通过: 修改逻辑项 id={}", id);
    }

    @Test
    @DisplayName("IND_104: 新建逻辑项-空名称(负向)")
    void test_addLogicEmptyName() {
        String resp = ind.addLogic("", "", "", logicStructureId);
        log.info("IND_104 空名称: {}", resp);
    }

    @Test
    @DisplayName("IND_105: 查询逻辑列表-无效结构ID(负向)")
    void test_searchInvalidStructure() {
        String resp = ind.searchLogicList("invalid_id_99999");
        log.info("IND_105 无效结构ID: {}", resp);
    }

    @Test
    @DisplayName("IND_106: 删除逻辑项(正向)")
    void test_deleteLogic() {
        String nodeId = String.valueOf(System.currentTimeMillis() % 10000000000L);
        String addResp = ind.addLogic(nodeId, "", "AT_ToDelete_" + suffix(), logicStructureId);
        String id = ind.extractId(addResp);
        Assertions.assertNotNull(id, "新建应返回ID");

        String resp = ind.deleteLogic(id, logicStructureId);
        Assertions.assertTrue(ind.isOk(resp), "删除应成功, resp: " + resp);
        log.info("IND_106 通过: 删除逻辑项 id={}", id);
    }
}
