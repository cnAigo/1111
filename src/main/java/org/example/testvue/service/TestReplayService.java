package org.example.testvue.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.example.testvue.entity.TestCaseStep;
import org.example.testvue.repository.TestCaseStepRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestReplayService {

    private final TestCaseStepRepository stepRepo;

    public TestReplayService(TestCaseStepRepository stepRepo) {
        this.stepRepo = stepRepo;
    }

    /**
     * Pure-code replay engine — zero AI dependency.
     * Loads recorded steps from the database and executes them sequentially.
     *
     * @param testCaseId the test case whose steps should be replayed
     * @return true if all steps passed, false if any step failed
     */
    public boolean replayTestCase(Long testCaseId) {
        List<TestCaseStep> steps = stepRepo.findByTestCaseIdOrderByStepOrderAsc(testCaseId);
        if (steps.isEmpty()) {
            System.out.println("该用例没有录制的步骤！");
            return false;
        }

        System.out.println("==== 开始极速回放，脱离 AI，纯净执行 ====");
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(
                 new BrowserType.LaunchOptions().setHeadless(true));
             Page page = browser.newPage()) {

            for (TestCaseStep step : steps) {
                System.out.println(">> 回放第 " + step.getStepOrder() + " 步 ["
                    + step.getActionType() + "]: " + step.getOriginalInstruction());

                switch (step.getActionType()) {
                    case "goto" -> page.navigate(step.getInputValue());
                    case "click" -> page.locator(step.getSelector())
                        .click(new Locator.ClickOptions().setTimeout(5000));
                    case "fill" -> page.locator(step.getSelector())
                        .fill(step.getInputValue(), new Locator.FillOptions().setTimeout(5000));
                    case "assert" -> {
                        page.waitForURL("**" + step.getInputValue() + "**",
                            new Page.WaitForURLOptions().setTimeout(5000));
                        System.out.println("   [断言成功] 页面已跳转至目标 URL");
                    }
                    default -> throw new IllegalArgumentException(
                        "未知的动作类型: " + step.getActionType());
                }

                page.waitForLoadState(LoadState.NETWORKIDLE);
            }

            System.out.println("==== 回放成功，测试用例通过！ ====");
            return true;

        } catch (Exception e) {
            System.err.println("X 回放失败，测试用例未通过: " + e.getMessage());
            return false;
        }
    }
}
