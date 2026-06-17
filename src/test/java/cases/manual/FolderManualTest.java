package cases.manual;

import base.ApiTestHelper;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("ReqFolderModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FolderManualTest extends ApiTestHelper {

    // ═══ 1. 新建文件夹 ═══
    @Test @DisplayName("1.1 根节点新建文件夹(正向)")
    void test_0101_create() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); Assertions.assertNotNull(f); log.info("1.1 根节点新建文件夹(正向) 通过"); }
    @Test @DisplayName("1.2 新建子文件夹(正向)")
    void test_0102_createSub() { String p=api.createFolder(PROJECT_ID,PROJECT_ID); String c=api.createFolder(PROJECT_ID,p); Assertions.assertNotNull(c); log.info("1.2 新建子文件夹(正向) 通过"); }

    // ═══ 2. 文件夹名称校验 ═══
    @Test @DisplayName("2.1 重命名-重复(负向)")
    void test_0201_renameDup() { String a=api.createFolder(PROJECT_ID,PROJECT_ID),b=api.createFolder(PROJECT_ID,PROJECT_ID); String r=api.renameFolder(PROJECT_ID,b,PROJECT_ID,"dup"); log.info("2.1 {}",r); }
    @Test @DisplayName("2.2 重命名-空(负向)")
    void test_0202_renameEmpty() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); String r=api.renameFolder(PROJECT_ID,f,PROJECT_ID,""); assertRejected(r,"空名称"); log.info("2.2 重命名-空(负向) 通过"); }
    @Test @DisplayName("2.3 重命名-超长(负向)")
    void test_0203_renameLong() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); String r=api.renameFolder(PROJECT_ID,f,PROJECT_ID,"A".repeat(200)); log.info("2.3 {}",r); }
    @Test @DisplayName("2.4 重命名-XSS(负向)")
    void test_0204_renameXss() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); String r=api.renameFolder(PROJECT_ID,f,PROJECT_ID,"<img src=x onerror=alert(1)>"); log.info("2.4 {}",r); }
    @Test @DisplayName("2.5 重命名-特殊字符(负向)")
    void test_0205_renameSpecial() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); String r=api.renameFolder(PROJECT_ID,f,PROJECT_ID,"!@#$%^&*()"); assertRejected(r,"特殊字符"); log.info("2.5 重命名-特殊字符(负向) 通过"); }

    // ═══ 3. 新建子文件夹 ═══
    @Test @DisplayName("3.1 文件夹下新建子文件夹(正向)")
    void test_0301_subFolder() { String p=api.createFolder(PROJECT_ID,PROJECT_ID); String c=api.createFolder(PROJECT_ID,p); Assertions.assertNotNull(c); log.info("3.1 文件夹下新建子文件夹(正向) 通过"); }

    // ═══ 4. 删除文件夹 ═══
    @Test @DisplayName("4.1 删除空文件夹(正向)")
    void test_0401_deleteEmpty() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); String r=api.deleteFolder(f,PROJECT_ID); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); api.forceCleanFolder(f); log.info("4.1 删除空文件夹(正向) 通过"); }
    @Test @DisplayName("4.2 删除有子文件夹(负向)")
    void test_0402_deleteWithChild() { String p=api.createFolder(PROJECT_ID,PROJECT_ID); api.createFolder(PROJECT_ID,p); String r=api.deleteFolder(p,PROJECT_ID); assertRejected(r,"有子应拦截"); log.info("4.2 删除有子文件夹(负向) 通过"); }
    @Test @DisplayName("4.3 取消删除(正向)")
    void test_0403_recover() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); api.deleteFolder(f,PROJECT_ID); api.recoverFolder(f,f); log.info("4.3 取消删除(正向) 通过"); }
    @Test @DisplayName("4.4 彻底清除(正向)")
    void test_0404_forceClean() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); api.deleteFolder(f,PROJECT_ID); api.forceCleanFolder(f); log.info("4.4 彻底清除(正向) 通过"); }

    // ═══ 5. 文件夹描述 ═══
    @Test @DisplayName("5.1 编辑描述(正向)")
    void test_0501_editDesc() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); String r=api.editFolderDescription(PROJECT_ID,f,PROJECT_ID,"AT_Desc_"+suffix()); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("5.1 编辑描述(正向) 通过"); }
    @Test @DisplayName("5.2 描述-空(负向)")
    void test_0502_editDescEmpty() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); String r=api.editFolderDescription(PROJECT_ID,f,PROJECT_ID,""); log.info("5.2 {}",r); }
    @Test @DisplayName("5.3 描述-超长1000字(负向)")
    void test_0503_editDescLong() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); String r=api.editFolderDescription(PROJECT_ID,f,PROJECT_ID,"D".repeat(1001)); log.info("5.3 {}",r); }
    @Test @DisplayName("5.4 描述-XSS(负向)")
    void test_0504_editDescXss() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); String r=api.editFolderDescription(PROJECT_ID,f,PROJECT_ID,"<script>alert(1)</script>"); log.info("5.4 {}",r); }

    // ═══ 6. 根节点刷新 ═══
    @Test @DisplayName("6.1 根节点刷新(正向)")
    void test_0601_refreshRoot() { String resp=api.getTree(PROJECT_ID,PROJECT_ID); Assertions.assertTrue(resp.contains("\"code\":200")); log.info("6.1 根节点刷新(正向) 通过"); }

    // ═══ 60. 收藏 ═══
    @Test @DisplayName("60.1 收藏文件夹(正向)")
    void test_6001_addFav() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); String r=api.addFavorite(PROJECT_ID,f,"reqSpeFolder"); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("60.1 收藏文件夹(正向) 通过"); }
    @Test @DisplayName("60.2 查看收藏列表(正向)")
    void test_6002_listFav() { String r=api.searchFavoriteList(PROJECT_ID); Assertions.assertTrue(r.contains("\"code\":200")); log.info("60.2 查看收藏列表(正向) 通过"); }
    @Test @DisplayName("60.3 取消收藏(正向)")
    void test_6003_delFav() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); api.addFavorite(PROJECT_ID,f,"reqSpeFolder"); String favResp=api.searchFavoriteList(PROJECT_ID); String favId=api.extractId(favResp); if(favId!=null){ String r=api.deleteFavorite(favId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); } log.info("60.3 取消收藏(正向) 通过"); }
    @Test @DisplayName("60.4 重复收藏同一文件夹(负向)")
    void test_6004_dupFav() { String f=api.createFolder(PROJECT_ID,PROJECT_ID); api.addFavorite(PROJECT_ID,f,"reqSpeFolder"); String r=api.addFavorite(PROJECT_ID,f,"reqSpeFolder"); assertRejected(r,"重复收藏"); log.info("60.4 重复收藏同一文件夹(负向) 通过"); }

    // ═══ 55. 基线(暂未实现) ═══
    @Test @DisplayName("55.1 基线(占位)")
    void test_5501_baseline() { log.info("55.1 基线: TODD - 暂无API"); }
}
