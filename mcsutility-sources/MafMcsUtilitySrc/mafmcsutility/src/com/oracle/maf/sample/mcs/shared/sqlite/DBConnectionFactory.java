package com.oracle.maf.sample.mcs.shared.sqlite;

import com.oracle.maf.sample.mcs.shared.log.LibraryLogger;

import java.io.File;

import java.sql.Connection;
import java.sql.SQLException;

import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.framework.api.GeneratedPassword;

/**
 *  *** INTERNAL USE ONLY  ***
 *
 * Some SDK features like Analytics require offline caching to survive the case in which network connectivity is not
 * available. A SQLite database is created at runtime by this SDK if the database doesn't exist. The tables will then
 * created for each SDK API. As we don't know which information will end up getting stored in the database, we encrypt
 * the database with a generated password
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 *
 */
public class DBConnectionFactory {

    private static DBConnectionFactory connectionFactory = new DBConnectionFactory();

    private boolean databaseExists = false;
    private LibraryLogger mlogger = null;
    private String databaseFilePathAndName = null;

    public DBConnectionFactory() {
        super();
        mlogger = new LibraryLogger();
        databaseFilePathAndName = AdfmfJavaUtilities.getDirectoryPathRoot(AdfmfJavaUtilities.ApplicationDirectory) + "/" + DatabaseConstants.MAFMCS_UTILITY_DB_NAME;
    }

    public static DBConnectionFactory getInstance() {
        return connectionFactory;
    }


    /**
     * Upon initial connection request, check if database exists or if it needs to be created. If it needs to be
     * created - do it.
     *
     * @return
     * @throws SQLException
     */
    private Connection initializeDB() throws SQLException {
        //initially check if database exists ot if it needs to be created
        mlogger.logFine("Initialize database", "DBConnectionFactory", "initializeDB");
        mlogger.logFine("Database file path and name: " + databaseFilePathAndName, "DBConnectionFactory","initializeDB");
        //check if database exists. If it doesn't exit, it will be created upon first connect attempt
        File dbFile = new File(databaseFilePathAndName);

        mlogger.logFine("Does db file exist?: " + dbFile.exists(), "DBConnectionFactory", "initializeDB");

        if (!dbFile.exists()) {
            mlogger.logFine("Database does not exist - creating ...", "DBConnectionFactory", "initializeDB");
            //create a random password
            GeneratedPassword.setPassword(DatabaseConstants.MAFMCS_UTIL_DBUSER,"_may_the_p0wer_0f_the_cl0ud_be_with_y0u");
            char[] password = GeneratedPassword.getPassword(DatabaseConstants.MAFMCS_UTIL_DBUSER);
            //create database
            Connection sqliteConnection = new SQLite.JDBCDataSource("jdbc:sqlite:" + databaseFilePathAndName).getConnection();
            
            sqliteConnection.setAutoCommit(false);
            
            //encrypt database
            AdfmfJavaUtilities.encryptDatabase(sqliteConnection, new String(password));
            mlogger.logFine("Database created and encrypted.", "DBConnectionFactory", "initializeDB");
            //set flag to indicate that database check no longer needs to be performed
            databaseExists = true;
            return sqliteConnection;
        } else {
            mlogger.logFine("Database available", "DBConnectionFactory", "initializeDB");
            //connect to existing database
            char[] password = GeneratedPassword.getPassword(DatabaseConstants.MAFMCS_UTIL_DBUSER);
            mlogger.logFine("Opening database connection", "DBConnectionFactory", "initializeDB");
            Connection sqliteConnection = new SQLite.JDBCDataSource("jdbc:sqlite:" + databaseFilePathAndName).getConnection(null, new String(password));
            sqliteConnection.setAutoCommit(false);

            databaseExists = true;
            return sqliteConnection;
        }
    }

    /**
     * Checks if database exists and returns a connection to it
     * @return
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        if (databaseExists == false) {
            mlogger.logFine("First call to obtain DB connection. Try and initialize DB", "DBConnectionFactory", "getConnection");
            return initializeDB();
        } else {
            mlogger.logFine("Database available", "DBConnectionFactory", "getConnection");
            //connect to existing database
            char[] password = GeneratedPassword.getPassword(DatabaseConstants.MAFMCS_UTIL_DBUSER);
            mlogger.logFine("Opening database connection", "DBConnectionFactory", "getConnection");
            //create a new connection each time to avoid stale connections
            Connection sqliteConnection = new SQLite.JDBCDataSource("jdbc:sqlite:" + databaseFilePathAndName).getConnection(null,new String(password));
            sqliteConnection.setAutoCommit(false);
            databaseExists = true;
            return sqliteConnection;
        }
    }
}

