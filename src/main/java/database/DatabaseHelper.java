/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * db utility methods
 * provides helper methods for common database operations
 * 
 * @author Sanod
 */
public class DatabaseHelper {
    
    // functional interface for result set handling
    @FunctionalInterface
    public interface ResultSetHandler<T> {
        T handle(ResultSet rs) throws SQLException;
    }
    
    // execute update statement with parameters
    public static int executeUpdate(String sql, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            
            // set parameters
            setParameters(pstmt, params);
            
            // execute update
            return pstmt.executeUpdate();
            
        } finally {
            closeQuietly(pstmt);
            closeQuietly(conn);
        }
    }
    
    // execute insert and return generated key
    public static int executeInsert(String sql, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            // set parameters
            setParameters(pstmt, params);
            
            // execute insert
            pstmt.executeUpdate();
            
            // get generated key
            rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            
            return 0;
            
        } finally {
            closeQuietly(rs);
            closeQuietly(pstmt);
            closeQuietly(conn);
        }
    }
    
    // execute query and return single result
    public static <T> T executeQuery(String sql, ResultSetHandler<T> handler, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            
            // set parameters
            setParameters(pstmt, params);
            
            // execute query
            rs = pstmt.executeQuery();
            
            // handle result set
            return handler.handle(rs);
            
        } finally {
            closeQuietly(rs);
            closeQuietly(pstmt);
            closeQuietly(conn);
        }
    }
    
    // execute query and return list of results
    public static <T> List<T> executeQueryList(String sql, ResultSetHandler<T> handler, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            
            // set parameters
            setParameters(pstmt, params);
            
            // execute query
            rs = pstmt.executeQuery();
            
            // handle result set and build list
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                T item = handler.handle(rs);
                if (item != null) {
                    results.add(item);
                }
            }
            
            return results;
            
        } finally {
            closeQuietly(rs);
            closeQuietly(pstmt);
            closeQuietly(conn);
        }
    }
    
    // execute batch update
    public static int[] executeBatch(String sql, List<Object[]> paramsList) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            
            pstmt = conn.prepareStatement(sql);
            
            // add batches
            for (Object[] params : paramsList) {
                setParameters(pstmt, params);
                pstmt.addBatch();
            }
            
            // execute batch
            int[] results = pstmt.executeBatch();
            conn.commit();
            
            return results;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    // ignore rollback exception
                }
            }
            throw e;
            
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    // ignore
                }
            }
            closeQuietly(pstmt);
            closeQuietly(conn);
        }
    }
    
    // set parameters in prepared statement
    private static void setParameters(PreparedStatement pstmt, Object... params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
        }
    }
    
    // safely close result set
    public static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }
    
    // safely close statement
    public static void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }
    
    // safely close connection
    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                // ignore
            }
        }
    }
    
    // begin transaction
    public static Connection beginTransaction() throws SQLException {
        Connection conn = DBConnection.getConnection();
        conn.setAutoCommit(false);
        return conn;
    }
    
    // commit transaction
    public static void commitTransaction(Connection conn) throws SQLException {
        if (conn != null) {
            conn.commit();
            conn.setAutoCommit(true);
        }
    }
    
    // rollback transaction
    public static void rollbackTransaction(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                // ignore
            }
        }
    }
    
    // check if table exists
    public static boolean tableExists(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
        Integer count = executeQuery(sql, rs -> {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }, tableName);
        return count != null && count > 0;
    }
    
    // get row count from table
    public static int getRowCount(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        Integer count = executeQuery(sql, rs -> {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        });
        return count != null ? count : 0;
    }
}