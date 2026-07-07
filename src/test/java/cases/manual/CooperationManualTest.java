package cases.manual;

import base.ApiTestHelper;
import actions.CoopHelper;
import org.junit.jupiter.api.*;

/**
 * Cooperation area CRUD tests.
 *
 * Each test method creates its own cooperation area(s), operates on them,
 * and tears them down individually — no shared state between methods.
 * The project context ({@code PROJECT_ID}) comes from the environment
 * ({@code TAAS_PROJECT_ID}) or {@code TestConstants}, never hardcoded.
 */
@Tag("CollaborationModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CooperationManualTest extends ApiTestHelper {

    private CoopHelper c;

    @Override
    @BeforeAll
    public void setupApi() {
        needsClassCooperationArea = false; // cooperation areas are top-level — no sandbox needed
        super.setupApi();
        c = new CoopHelper(api, this::suffix, PROJECT_ID);
    }

    // ═══ 69. 添加/修改合作区 ═══
    @Test @DisplayName("69.1 添加合作区(正向)")
    void test_6901_add() { c.ok("Coop"); }

    @Test @DisplayName("69.2 修改合作区")
    void test_6902_update() { c.updateRaw(c.ok("Upd"), c.name("Upd2"), c.code("U2")); }

    // ═══ 70. 名称必填/编码必填/密级必选 ═══
    @Test @DisplayName("70.1 空名称(负向)")
    void test_7001_emptyName() { c.fail("", c.code("E"), "空名称"); }

    @Test @DisplayName("70.2 空编码(负向)")
    void test_7002_emptyCode() { c.fail(c.name("NC"), "", "空编码"); }

    // ═══ 71. 名称校验 ═══
    @Test @DisplayName("71.1 非空(正向)")
    void test_7101_nonEmpty() { c.ok(c.name("V"), c.code("C1")); }

    @Test @DisplayName("71.2 重复名称(负向)")
    void test_7102_dupName() { String n=c.name("DupN"); c.ok(n,c.code("DC1")); c.fail(n,c.code("DC2"),"重复名称"); }

    @Test @DisplayName("71.3 XSS名称(负向)")
    void test_7103_xssName() { c.fail("<img src=x onerror=alert(1)>", c.code("X"), null); }

    // ═══ 72. 编码校验 ═══
    @Test @DisplayName("72.1 非字母开头(负向)")
    void test_7201_invalidPrefix() { c.fail(c.name("Inv"), "1abc"+suffix().substring(0,4), "非字母开头编码"); }

    @Test @DisplayName("72.2 重复编码(负向)")
    void test_7202_dupCode() { String cd=c.code("DC"); c.ok(c.name("A"),cd); c.fail(c.name("B"),cd,"重复编码"); }

    @Test @DisplayName("72.3 编码长度超限(负向)")
    void test_7203_length() { c.fail(c.name("Long"), "A"+suffix().replace("-","")+"extra_long", null); }

    // ═══ 73. 删除合作区 ═══
    @Test @DisplayName("73.1 删除无用户合作区(正向)")
    void test_7301_delNoUser() { c.del(c.ok("Del")); }

    @Test @DisplayName("73.2 删除不存在ID(负向)")
    void test_7302_invalidId() { c.delInvalid("invalid_99999"); }

    // ═══ 74. 搜索合作区 ═══
    @Test @DisplayName("74.1 名称检索(正向)")
    void test_7401_search() { c.search(""); }

    @Test @DisplayName("74.2 模糊搜索(正向)")
    void test_7402_searchFuzzy() { c.search("AT_"); }

    // ═══ 75. 分配人员 ═══
    @Test @DisplayName("75.1 查看项目人员列表(正向)")
    void test_7501_listPerson() { c.firstUser(); }

    @Test @DisplayName("75.2 分配人员到合作区(正向)")
    void test_7502_assign() { c.assign(c.ok("Assign"), c.firstUser()); }

    @Test @DisplayName("75.3 重复添加同一人员(负向)")
    void test_7503_dupAssign() { c.assignDup(c.ok("DupAs"), c.firstUser()); }

    // ═══ 76. 取消授权 ═══
    @Test @DisplayName("76.1 取消人员授权")
    void test_7601_remove() { String a=c.ok("Remove"),u=c.user(1); c.assign(a,u); c.unassignRaw(a,u); }

    @Test @DisplayName("76.2 重复取消授权(负向)")
    void test_7602_removeTwice() { String u=c.user(1); c.unassignDup(c.ok("RemTw"),u); }

    // ═══ 77. 重复添加合作区 ═══
    @Test @DisplayName("77.1 添加已有合作区(负向)")
    void test_7701_dupAdd() { c.failExact(c.name("DupAdd"), c.code("DA"), 500, "已经存在相同的合作区编码或名称！"); }

    // ═══ 78. 获取合作区人员列表 / 取消admin授权 ═══
    @Test @DisplayName("78.1 获取合作区人员列表(正向)")
    void test_7801_listAreaPerson() { c.personCount(c.ok("AreaPerson")); }

    @Test @DisplayName("78.2 取消创建者授权(负向)")
    void test_7802_removeAdmin() { c.unassignRaw(c.ok("RemAdmin"), "1"); }

    @Test @DisplayName("78.3 重复取消admin授权(负向)")
    void test_7803_removeAdminTwice() { c.unassignDup(c.ok("RemAdTw"), "1"); }

    // ═══ 79. 修改时唯一性校验 ═══
    @Test @DisplayName("79.1 修改名称为已有名称(负向)")
    void test_7901_updateDupName() { String n1=c.name("UpdN1"); c.ok(n1,c.code("N1")); c.updateFail(c.ok("UpdN2"),n1,c.code("N2"),"重复名称"); }

    @Test @DisplayName("79.2 修改编码为已有编码(负向)")
    void test_7902_updateDupCode() { String cd1=c.code("C1"); c.ok(c.name("UpdC1"),cd1); c.updateFail(c.ok("UpdC2"),c.name("UpdC2"),cd1,"重复编码"); }
}
