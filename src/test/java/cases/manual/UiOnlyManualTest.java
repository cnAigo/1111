package cases.manual;

import base.ApiTestHelper;
import org.junit.jupiter.api.*;

/**
 * 仅UI操作 — 无对应API，只做记录。
 * 14.需求规格检索 / 21.权限搜索框 / 23.显示大纲 / 26.属性取值枚举
 * 32.配置移除属性 / 41-43.复制粘贴剪切 / 45.切换标题内容 / 49.分屏展示
 */
@Tag("ReqFolderModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UiOnlyManualTest extends ApiTestHelper {
    { needsClassCooperationArea = false; } // UI-only notes — no API calls

    @Test @DisplayName("14. 需求规格检索 — 仅UI")
    void test_14_searchReqSpec() { log.info("14. 需求规格检索: 仅UI"); }

    @Test @DisplayName("21. 权限搜索框 — 仅UI")
    void test_21_permissionSearch() { log.info("21. 权限搜索框: 仅UI"); }

    @Test @DisplayName("23. 显示大纲/结构定位/隐藏大纲 — 仅UI")
    void test_23_outline() { log.info("23. 显示大纲: 仅UI"); }

    @Test @DisplayName("26. 新建属性-发布状态/取值范围/枚举类型 — 仅UI")
    void test_26_attrUiOps() { log.info("26. 属性UI操作: 仅UI"); }

    @Test @DisplayName("32. 配置属性/移除属性 — 仅UI")
    void test_32_configAttr() { log.info("32. 配置移除属性: 仅UI"); }

    @Test @DisplayName("41. 复制→粘贴 — 仅UI")
    void test_41_copyPaste() { log.info("41. 复制粘贴: 仅UI, 无url"); }

    @Test @DisplayName("42. 复制 — 仅UI")
    void test_42_copy() { log.info("42. 复制: 仅UI, 无url"); }

    @Test @DisplayName("43. 剪切 — 仅UI")
    void test_43_cut() { log.info("43. 剪切: 仅UI, 无url"); }

    @Test @DisplayName("45. 切换标题/内容视图 — 仅UI")
    void test_45_switchView() { log.info("45. 切换标题内容: 仅UI"); }

    @Test @DisplayName("49. 分屏展示 — 仅UI")
    void test_49_splitScreen() { log.info("49. 分屏展示: 仅UI"); }
}
