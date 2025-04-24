//package io.project.clientkeeperbot.infrastructure.db;
//
//import lombok.extern.slf4j.Slf4j;
//
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.SQLException;
//
//@Slf4j
//public class DbConnectionFactory {
//    private static Connection connection;
//
//    public static Connection createConnection() {
//        if (connection == null) {
//            try {
//                connection = DriverManager.getConnection("jdbc:postgresql://localhost:5434/postgres", "postgres", "pass");
//                log.info("Connection to DB OK");
//            } catch (SQLException e) {
//                log.error("Connection to DB ERROR: {}", e.getMessage());
//            }
//        }
//
//        return connection;
//    }
//
//}
