import com.google.gson.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;

public class T2 {
    public static void main(String[] a) throws Exception {
        Playwright pw = Playwright.create();
        APIRequestContext api = pw.request().newContext(
            new APIRequest.NewContextOptions().setIgnoreHTTPSErrors(true));
        String base = "https://192.168.6.171:8088";
        String dev = base + "/dev-api";
        
        JsonObject lb = new JsonObject(); lb.addProperty("username","admin"); lb.addProperty("password","Aa123456");
        APIResponse lr = api.post(base + "/login-api/auth/token/login",
            RequestOptions.create().setHeader("Content-Type","application/json").setData(lb.toString()));
        System.out.println("1 LOGIN: " + lr.status() + " " + lr.text().substring(0,Math.min(150,lr.text().length())));
        
        APIResponse r2 = api.get(dev + "/common/search/searchProjectList?title=&originated=");
        System.out.println("2 PROJECT_LIST: " + r2.status() + " " + r2.text().substring(0,Math.min(200,r2.text().length())));
        
        String b3 = "{\"projectId\":\"2058851105448046592\",\"parentId\":\"2058851105448046592\",\"parentType\":\"project\"}";
        APIResponse r3 = api.post(dev + "/erm/search/searchReqFolderStructureTree",
            RequestOptions.create().setHeader("Content-Type","application/json").setData(b3));
        System.out.println("3 TREE: " + r3.status() + " " + r3.text().substring(0,Math.min(200,r3.text().length())));
        
        pw.close();
    }
}
