package cases.manual;

import base.ApiTestHelper;
import org.junit.jupiter.api.*;

@Tag("ArchModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FuncManualTest extends ApiTestHelper {
    { needsClassCooperationArea = false; }

    @Test @DisplayName("待开发")
    void test_placeholder() { log.info("功能管理: 待开发"); }
}
