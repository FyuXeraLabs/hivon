/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * database connection configuration
 * loads config from .env file using dotenv
 * 
 * @author Sanod
 */
public class DatabaseConfig {
    
    private static final Dotenv dotenv = Dotenv.load();
    
    private static final String HOST = dotenv.get("DB_HOST");
    private static final String PORT = dotenv.get("DB_PORT");
    private static final String DATABASE = dotenv.get("DB_NAME");
    private static final String USERNAME = dotenv.get("DB_USERNAME");
    private static final String PASSWORD = dotenv.get("DB_PASSWORD");
    private static final String DRIVER = dotenv.get("DB_DRIVER", "com.mysql.cj.jdbc.Driver");
    
    private static final String DB_URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE + 
                                        "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    
    private static final int MAX_POOL_SIZE = Integer.parseInt(dotenv.get("DB_POOL_MAX_SIZE", "10"));
    private static final int MIN_POOL_SIZE = Integer.parseInt(dotenv.get("DB_POOL_MIN_SIZE", "5"));
    private static final int TIMEOUT = Integer.parseInt(dotenv.get("DB_TIMEOUT", "30000"));
    
    // get database url
    public static String getUrl() {
        return DB_URL;
    }
    
    // get database username
    public static String getUsername() {
        return USERNAME;
    }
    
    // get database password
    public static String getPassword() {
        return PASSWORD;
    }
    
    // get database driver class name
    public static String getDriverClass() {
        return DRIVER;
    }
    
    // get connection pool maximum size
    public static int getMaxPoolSize() {
        return MAX_POOL_SIZE;
    }
    
    // get connection pool minimum size
    public static int getMinPoolSize() {
        return MIN_POOL_SIZE;
    }
    
    // get connection timeout in milliseconds
    public static int getConnectionTimeout() {
        return TIMEOUT;
    }
}