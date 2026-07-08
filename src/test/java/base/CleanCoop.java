package base;

/** Clean all cooperation areas, keep only the project root. */
public class CleanCoop {
    public static void main(String[] args) {
        var runner = new ApiTestBase() {};
        try {
            runner.setupApi();
            runner.loginViaApi();
            System.out.println("Cleaning cooperation areas...");
            int deleted = runner.api.cleanAllCooperationAreasExcept("test");
            System.out.println("Deleted " + deleted + " cooperation areas.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            runner.teardownApi();
        }
    }
}
