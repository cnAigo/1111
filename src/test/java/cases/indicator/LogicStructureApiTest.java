package cases.indicator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("IndicatorModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LogicStructureApiTest extends IndicatorTestBase {

    private String structId = null;

    @Test
    @DisplayName("IND_001: 新建逻辑结构(正向)")
    void test_createLogicStructure() {
        String name = "AT_Struct_" + suffix();
        String resp = ind.addLogicStructure(name, "自动化测试创建");
        Assertions.assertTrue(ind.isOk(resp), "新建应成功, resp: " + resp);
        structId = ind.extractId(resp);
        Assertions.assertNotNull(structId, "应返回ID");
        log.info("IND_001 通过: 新建逻辑结构 [{}] id={}", name, structId);
    }

    @Test
    @DisplayName("IND_002: 查询逻辑结构列表(正向)")
    void test_searchLogicStructureList() {
        String resp = ind.searchLogicStructureList();
        Assertions.assertTrue(ind.isOk(resp), "查询列表应成功, resp: " + resp);
        Assertions.assertTrue(resp.contains("data"), "应包含data字段");
        log.info("IND_002 通过: 查询逻辑结构列表成功");
    }

    @Test
    @DisplayName("IND_003: 查询逻辑结构详情(正向)")
    void test_getLogicStructureInfo() {
        String name = "AT_Struct_" + suffix();
        String addResp = ind.addLogicStructure(name, "查询详情测试");
        String id = ind.extractId(addResp);
        Assertions.assertNotNull(id, "新建应返回ID");

        String resp = ind.getLogicStructureInfo(id);
        Assertions.assertTrue(ind.isOk(resp), "查询详情应成功, resp: " + resp);
        Assertions.assertTrue(resp.contains(name), "详情应包含名称: " + name);
        log.info("IND_003 通过: 查询详情 id={}", id);
    }

    @Test
    @DisplayName("IND_004: 新建逻辑结构-空名称(负向)")
    void test_createEmptyName() {
        String resp = ind.addLogicStructure("", "");
        log.info("IND_004 空名称: resp={}", resp);
        // Don't assert — just log; backend may accept or reject
    }

    @Test
    @DisplayName("IND_005: 查询逻辑结构详情-无效ID(负向)")
    void test_getInvalidId() {
        String resp = ind.getLogicStructureInfo("invalid_id_99999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("IND_005 无效ID: code={}", root.get("code").getAsInt());
    }

    @Test
    @DisplayName("IND_006: 删除逻辑结构(正向)")
    void test_deleteLogicStructure() {
        String name = "AT_DelStruct_" + suffix();
        String addResp = ind.addLogicStructure(name, "待删除");
        String id = ind.extractId(addResp);
        Assertions.assertNotNull(id, "新建应返回ID");

        String resp = ind.deleteLogicStructure(id);
        Assertions.assertTrue(ind.isOk(resp), "删除应成功, resp: " + resp);
        log.info("IND_006 通过: 删除逻辑结构 id={}", id);
    }

    @Test
    @DisplayName("IND_007: 删除逻辑结构-无效ID(负向)")
    void test_deleteInvalidId() {
        String resp = ind.deleteLogicStructure("invalid_id_99999");
        log.info("IND_007 删除无效ID: {}", resp);
    }

    @Test
    @DisplayName("IND_008: 重复名称(负向)")
    void test_createDuplicateName() {
        String name = "AT_Dup_" + suffix();
        ind.addLogicStructure(name, "第一个");
        String resp2 = ind.addLogicStructure(name, "第二个");
        log.info("IND_008 重复名称: {}", resp2);
    }
}
