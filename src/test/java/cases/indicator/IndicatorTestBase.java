package cases.indicator;

import actions.IndicatorApiActions;
import base.ApiTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import base.TimeoutSkipExtension;

/**
 * Base class for indicator tests — provides IndicatorApiActions instance
 * and cleanup helpers.  Extends ApiTestHelper for auth + browser lifecycle.
 */
@ExtendWith(TimeoutSkipExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IndicatorTestBase extends ApiTestHelper {

    protected IndicatorApiActions ind;

    @BeforeAll
    @Override
    public void setup() {
        super.setup();
        ind = new IndicatorApiActions(context.request(), PROJECT_ID);
    }
}
