import com.google.gson.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import config.TestConfig;
import java.nio.file.*;

public class QuickApiCheck {
    public static void main(String[] args) throws Exception {
        Playwright playwright = Playwright.create(new Playwright.CreateOptions()
            .setEnv(java.util.Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
            .setHeadless(true)
            .setExecutablePath(Paths.get("C:/Program Files/Google/Chrome/Application/chrome.exe")));
        BrowserContext ctx = browser.newContext(new Browser.NewContextOptions()
            .setIgnoreHTTPSErrors(true));
        Page page = ctx.newPage();
        APIRequestContext api = page.request();

        api.post(TestConfig.BASE_URL + "/login-api/auth/token/login",
            RequestOptions.create().setHeader("Content-Type", "application/json")
                .setData("{\"username\":\"admin\",\"password\":\"Aa123456\"}"));

        JsonObject body = new JsonObject();
        body.addProperty("name", "TEST_CHECK");
        body.addProperty("description", "auto");
        body.addProperty("projectId", "2058851105448046592");
        
        APIResponse resp = api.post(TestConfig.BASE_URL + "/api-api/moe/add/addLogicStructure",
            RequestOptions.create().setHeader("Content-Type", "application/json")
                .setHeader("ProjectId", "2058851105448046592")
                .setData(body.toString()));
        
        String text = resp.text();
        System.out.println("Status: " + resp.status());
        System.out.println("Response: " + text);
        
        JsonObject r = JsonParser.parseString(text).getAsJsonObject();
        System.out.println("code: " + r.get("code"));
        if (r.has("data") && !r.get("data").isJsonNull()) {
            JsonElement d = r.get("data");
            System.out.println("data type: " + (d.isJsonObject() ? "object" : d.isJsonArray() ? "array" : "other"));
            if (d.isJsonObject()) {
                JsonObject dobj = d.getAsJsonObject();
                for (String key : dobj.keySet()) {
                    System.out.println("  data." + key + " = " + dobj.get(key));
                }
            }
        }

        playwright.close();
    }
}
