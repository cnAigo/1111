package cases.manual;

import base.ApiTestHelper;
import org.junit.jupiter.api.*;

@Tag("ArchModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ArchManualTest extends ApiTestHelper {
    { needsClassCooperationArea = false; }

    @Test @DisplayName("待开发")
    void test_placeholder() { log.info("架构管理: 待开发"); }
}
