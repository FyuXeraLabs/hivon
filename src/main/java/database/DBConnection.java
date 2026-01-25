/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import core.config.DatabaseConfig;
import core.logging.ErrorLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * manages db connections using connection pooling
 * uses hikaricp for connection management
 * 
 * @author Sanod
 */
public class DBConnection {
    
    private static HikariDataSource dataSource;
    private static final Object lock = new Object();
    
    // initialize connection pool with hikaricp
    private static void initializeDataSource() {
        if (dataSource == null) {
            synchronized (lock) {
                if (dataSource == null) {
                    try {
                        // configure hikari connection pool
                        HikariConfig config = new HikariConfig();
                        config.setJdbcUrl(DatabaseConfig.getUrl());
                        config.setUsername(DatabaseConfig.getUsername());
                        config.setPassword(DatabaseConfig.getPassword());
                        config.setDriverClassName(DatabaseConfig.getDriverClass());
                        
                        // set pool configuration
                        config.setMaximumPoolSize(DatabaseConfig.getMaxPoolSize());
                        config.setMinimumIdle(DatabaseConfig.getMinPoolSize());
                        config.setConnectionTimeout(DatabaseConfig.getConnectionTimeout());
                        config.setIdleTimeout(600000); // 10 minutes
                        config.setMaxLifetime(1800000); // 30 minutes
                        
                        // set connection properties for MySQL optimization
                        config.addDataSourceProperty("cachePrepStmts", "true");
                        config.addDataSourceProperty("prepStmtCacheSize", "250");
                        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                        config.addDataSourceProperty("useServerPrepStmts", "true");
                        config.addDataSourceProperty("useLocalSessionState", "true");
                        config.addDataSourceProperty("rewriteBatchedStatements", "true");
                        config.addDataSourceProperty("cacheResultSetMetadata", "true");
                        config.addDataSourceProperty("cacheServerConfiguration", "true");
                        config.addDataSourceProperty("elideSetAutoCommits", "true");
                        config.addDataSourceProperty("maintainTimeStats", "false");
                        
                        // create datasource
                        dataSource = new HikariDataSource(config);
                        
                        System.out.println("HikariCP connection pool initialized successfully");
                        
                    } catch (Exception e) {
                        ErrorLogger.logError(e.getMessage(), e);
                        throw new RuntimeException("failed to initialize connection pool: " + e.getMessage(), e);
                    }
                }
            }
        }
    }
    
    // get connection from pool
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            initializeDataSource();
        }
        return dataSource.getConnection();
    }
    
    // close connection and return to pool
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                ErrorLogger.logError(e.getMessage(), e);
                System.err.println("error closing connection: " + e.getMessage());
            }
        }
    }
    
    // shutdown connection pool
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Connection pool shut down successfully");
        }
    }
    
    // check if connection pool is initialized
    public static boolean isInitialized() {
        return dataSource != null && !dataSource.isClosed();
    }
    
    // get active connections count
    public static int getActiveConnections() {
        if (dataSource != null) {
            return dataSource.getHikariPoolMXBean().getActiveConnections();
        }
        return 0;
    }
    
    // get idle connections count
    public static int getIdleConnections() {
        if (dataSource != null) {
            return dataSource.getHikariPoolMXBean().getIdleConnections();
        }
        return 0;
    }
    
    // get total connections count
    public static int getTotalConnections() {
        if (dataSource != null) {
            return dataSource.getHikariPoolMXBean().getTotalConnections();
        }
        return 0;
    }
}