package base;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import config.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthHelper {

    private static final Logger log = LoggerFactory.getLogger(AuthHelper.class);
    private static final String LOGIN_URL = TestConfig.BASE_URL + "/login-api/auth/token/login";

    private static String cachedToken;

    public static String login(APIRequestContext request, String username, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);

        APIResponse resp = request.post(LOGIN_URL,
                RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData(body.toString()));

        String text = resp.text();
        log.info("Login response status={}", resp.status());

        JsonObject json = JsonParser.parseString(text).getAsJsonObject();
        int code = json.has("code") ? json.get("code").getAsInt() : -1;
        String msg = json.has("msg") ? json.get("msg").getAsString() : "";

        // 登录成功但用cookie认证（无token返回）
        if (code == 200 || msg.contains("成功") || msg.contains("登陆成功")) {
            log.info("Login OK (cookie-based): {}", msg);
            cachedToken = null;
            return "cookie";
        }

        // 有token的登录
        if (code == 200 && json.has("data") && !json.get("data").isJsonNull()) {
            JsonObject data = json.getAsJsonObject("data");
            if (data.has("token")) {
                cachedToken = data.get("token").getAsString();
                return cachedToken;
            }
            if (data.has("access_token")) {
                cachedToken = data.get("access_token").getAsString();
                return cachedToken;
            }
        }

        throw new RuntimeException("Login failed: " + msg);
    }

    public static String getToken() { return cachedToken; }
}
