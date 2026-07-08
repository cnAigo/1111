package org.example.testvue.config;

/**
 * Centralized constants for API calls — no more scattered magic strings.
 */
public final class ApiConstants {

    private ApiConstants() {}

    public static final String APP_ID = "df059582d20e40d3995203fcc63a6d5e";

    // ── ERM API paths (requirements) ──
    public static final String ERM_TREE        = "/erm/search/searchReqFolderStructureTree";
    public static final String ERM_DEL_FOLDER  = "/erm/del/delReqSpeFolder";
    public static final String ERM_CLEAN_FOLDER = "/erm/clean/cleanReqSpeFolder";
    public static final String ERM_DEL_DOC     = "/erm/del/delReqSpe";
    public static final String ERM_CLEAN_DOC   = "/erm/clean/cleanReqSpe";
    public static final String ERM_SEARCH_SPE  = "/erm/search/searchReqSpeListFromProject";

    // ── MOE API paths (indicators) ──
    public static final String MOE_SEARCH_LS   = "/moe/search/searchLogicStructureList";
    public static final String MOE_SEARCH_LOGIC = "/moe/search/searchLogicList";
    public static final String MOE_DEL_LS       = "/moe/delete/deleteLogicStructure";
    public static final String MOE_DEL_LOGIC    = "/moe/delete/deleteLogic";
    public static final String MOE_SEARCH_PARAM = "/moe/search/searchLogicStructureParameterList";
    public static final String MOE_DEL_PARAM    = "/moe/remove/removeLogicStructureParameter";
    public static final String MOE_SEARCH_SCHEME = "/moe/search/searchPhysicalSchemeList";
    public static final String MOE_DEL_SCHEME   = "/moe/delete/deleteAPhysicalScheme";

    // ── Cooperation Area (Project) ──
    public static final String COOP_SEARCH   = "/common/search/searchProjectList";
    public static final String COOP_DELETE   = "/common/delete/delProject";

    // ── Auth ──
    public static final String LOGIN_PATH = "/login-api/auth/token/login";

    // ── Headers ──
    public static final String HEADER_APP_ID = "appId";
    public static final String HEADER_PROJECT_ID = "ProjectId";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_TOKEN = "token";
    public static final String HEADER_ACCESS_TOKEN = "Access-Token";
}
