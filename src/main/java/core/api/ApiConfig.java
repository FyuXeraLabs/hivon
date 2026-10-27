/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.api;

import core.config.SystemParameters;
import core.logging.Logger;

/**
 * API configuration loaded from .env
 *
 * @author Sanod
 */
public class ApiConfig {



    // api base url (no trailing slash)
    public static final String BASE_URL;

    // session timeout in minutes
    public static final int SESSION_TIMEOUT_MINUTES;

    // http timeouts in seconds
    public static final int CONNECT_TIMEOUT = 5;
    public static final int READ_TIMEOUT = 15;

    // api endpoints
    public static final String AUTH_LOGIN = "/auth/login";
    public static final String AUTH_REFRESH = "/auth/refresh";
    public static final String AUTH_ME = "/auth/me";
    public static final String USERS = "/users";
    public static final String USERS_OFFLINE_DB_KEY = "/users/offline-db-key";
    public static final String PERMISSIONS = "/permissions";

    // masterdata endpoints
    public static final String WAREHOUSES = "/warehouses";
    public static final String VENDORS = "/vendors";
    public static final String CUSTOMERS = "/customers";
    public static final String MATERIALS = "/materials";
    public static final String BINS = "/bins";
    public static final String BINS_GENERATE = "/bins/generate";
    public static final String ZONES = "/zones";
    public static final String BATCHES = "/batches";
    public static final String SYSTEM_PARAMETERS = "/system-parameters";
    public static final String INVENTORY = "/inventory";
    public static final String PURCHASE_ORDERS = "/purchase-orders";
    public static final String SALES_ORDERS = "/sales-orders";
    public static final String CYCLE_COUNTS = "/cycle-counts";
    public static final String TO_BIN_TO_BIN = "/transfer-orders/bin-to-bin";
    public static final String TO_BIN_TO_BIN_MATERIALS = "/transfer-orders/bin-to-bin/materials";
    public static final String TO_BIN_TO_BIN_SOURCE_BINS = "/transfer-orders/bin-to-bin/source-bins";
    public static final String TO_BIN_TO_BIN_DEST_BINS = "/transfer-orders/bin-to-bin/dest-bins";
    public static final String MOVEMENTS_SPLITTING = "/movements/splitting";
    public static final String MOVEMENTS_SPLITTING_MATERIALS = "/movements/splitting/materials";
    public static final String MOVEMENTS_SPLITTING_SOURCE_BINS = "/movements/splitting/source-bins";
    public static final String MOVEMENTS_SPLITTING_DEST_BINS = "/movements/splitting/dest-bins";

    static {
        String url = SystemParameters.API_BASE_URL;
        // remove trailing slash if present
        BASE_URL = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

        SESSION_TIMEOUT_MINUTES = SystemParameters.SESSION_TIMEOUT_MINUTES;

        Logger.log("ApiConfig", "API Base URL: " + BASE_URL);
        Logger.log("ApiConfig", "Session Timeout: " + SESSION_TIMEOUT_MINUTES + " minutes");
    }
}
