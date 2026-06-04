package base;

import config.TestConstants;

/** Standalone utility: clean ALL folders and documents under the project root node via API. */
public class CleanRoot {

    public static void main(String[] args) {
        String projectId = args.length > 0 ? args[0] : TestConstants.PROJECT_ID;

        System.out.println("=== API Root Cleaner ===");
        System.out.println("Project ID: " + projectId);
        System.out.println("WARNING: This will delete ALL folders and documents under the root node!");

        var runner = new AbstractTestBase() {};
        try {
            runner.setup();
            runner.loginViaApi();
            runner.api.sweepATFolders(projectId);
            runner.api.cleanAllUnderRoot(projectId);
            System.out.println("=== Done ===");
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            runner.teardown();
        }
    }
}
