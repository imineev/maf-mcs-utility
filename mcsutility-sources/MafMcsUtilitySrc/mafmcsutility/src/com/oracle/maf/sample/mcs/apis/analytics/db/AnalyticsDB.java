package com.oracle.maf.sample.mcs.apis.analytics.db;

import com.oracle.maf.sample.mcs.shared.log.LibraryLogger;
import com.oracle.maf.sample.mcs.shared.sqlite.DBConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;


/**
 * MAF MCS Utility requires an online connection to work against Oracle MCS. However, for Analytics chances are that events
 * cannot be sent to the MCS server because of a missing network connectiity. To avoid loss of information needed by mobile
 * program managers, analytic events are cached in a SQLite database to be flushed to the server with the next event createObject.
 * <p>
 * Unlike other MAF MCS Utility proxies like Storage or Custom API, the analytic events are not accessible to MAF developer to
 * implement a custom offline strategy. Therefore analytic events are handled in this utility directly
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public final class AnalyticsDB {

    private static AnalyticsDB analyticsDB = new AnalyticsDB();

    public AnalyticsDB() {
        super();
    }

    public static AnalyticsDB getInstance() {
        return analyticsDB;
    }

    /**
     * Get a connection handle to the SQLite database to log pending messages that could not be sent to the MCS
     * analytic engine for processing
     * @return SQL connection object on success, or null on failure
     */
    private Connection getConnection() {
        try {
            return DBConnectionFactory.getInstance().getConnection();
        } catch (SQLException e) {
            //we cannot obtain a handle to a SQLite connection. So what we do is, log the problem as an
            //error and don't save the event as we cannot recover from the problem.
            LibraryLogger logger = new LibraryLogger();
            logger.logError("Cannot obtain SQL connection. SQLException message is: " + e.getClass().toString() +
                            " :: " + e.getMessage() + "Error Code: " + e.getErrorCode(), "AnalyticsDB",
                            "getConnection");
            return null;
        }
    }

    /**
     * Make sure the Analytic table is created or does exist.
     * @return success / false based on whether the tables exist
     */
    private Connection ensureAnalyticTables() {

        LibraryLogger logger = new LibraryLogger();
        logger.logFine("Trying to obtain SQLite connection", "AnalyticsDB", "ensureAnalyticTables");
        Connection conn = getConnection();

        if (conn != null) {
            logger.logFine("Connection obtained from SQLite", "AnalyticsDB", "ensureAnalyticTables");
            try {
                Statement pStmt = conn.createStatement();

                logger.logFine("Ensuring analytic table: " + AnalyticsSQLHelper.ANALYTIC_TABLE_NAME+" exists", "AnalyticsDB", "ensureAnalyticTables"); 
                pStmt.execute(AnalyticsSQLHelper.CREATE_ANALYTIC_TABLE_IF_NOT_EXIST);
                logger.logFine("Analytic table created or reused if available", "AnalyticsDB", "ensureAnalyticTables");

                pStmt.close();
                //close connection
                conn.commit();

                //return connection to called for use
                return conn;

            } catch (SQLException sqlException) {
                logger.logError("Exception occured in access to SQLite database : " + sqlException.getMessage() +"Error Code: " + sqlException.getErrorCode(), "AnalyticsDB", "ensureAnalyticTables");
                try {
                    conn.close();
                } catch (Exception e) {
                    //no way we can handle the exception here. So just print it
                    logger.logError("Failed to close connection after Expecption. "+e.getMessage(),"AnalyticsDB", "ensureAnalyticTables");
                }
                //no connection to return, so return null
                return null;
            }
        }

        //we don't have a valid database access
        return null;
    }

    /**
     * This method should be called when all events are successfully delivered to the client. If a client cannot send all
     *  messages it still should purge all saved messages for an MBE and re-upload those that couldn't be sent
     *
     * @param mobileBackendId the ID of a mobile backend for which the SQLite database should be purged
     *
     * @return true if operation was performed successfully, false otherwise
     */
    public boolean purgeAnalyticMessagesForMobileBackend(String mobileBackendId) {
        boolean operationSuccess = false;
        LibraryLogger logger = new LibraryLogger();

        logger.logFine("Deleting all saved messages for MBE: " + mobileBackendId, "AnalyticsDB","purgeAnalyticMessagesForMobileBackend");

        //get connection and ensure table does exist
        Connection conn = ensureAnalyticTables();

        if (conn != null) {
            try {
                logger.logFine("Established connection to SQLite", "AnalyticsDB",
                               "purgeAnalyticMessagesForMobileBackend");
                //delete rows saved for specific mobile backend
                PreparedStatement pStmt = conn.prepareStatement(AnalyticsSQLHelper.ANALYTIC_TABLE_CONTENT_DELETE);
                pStmt.setString(1, mobileBackendId);
                pStmt.executeUpdate();
                pStmt.close();
                conn.commit();
                logger.logFine("Deleted messages for mobileBackend ID " + mobileBackendId, "AnalyticsDB",
                               "purgeAnalyticTables");
                conn.close();
                operationSuccess = true;

            } catch (SQLException sqlExepction) {
                logger.logError("SQLException when deleting existing content for backend Id " + mobileBackendId +
                                " : " + sqlExepction.getMessage() + "Error Code: " + sqlExepction.getErrorCode(),
                                "AnalyticsDB", "purgeAnalyticTables");
                try {
                    logger.logFine("Trying to close connection", "AnalyticsDB",
                                   "purgeAnalyticMessagesForMobileBackend");
                    conn.close();
                } catch (Exception e) {
                    //we can't throw an excpetion here
                    logger.logError("Unable to close connection. Exception is " + e.getClass().toString() + " :: " +
                                    e.getMessage(), "AnalyticsDB", "purgeAnalyticMessagesForMobileBackend");
                }
            }
        }
        return operationSuccess;
    }

    /**
     * Saves a single message and delimted header value ("key:value,key2:value2,..." for sending later
     *
     * @param mobileBackendId the mobile backend for which this messages is saved.
     * @param jsonMessage the analytic event in a json format
     * @param delimitedHeaders a string that contains header keys and values delimited with a colon ":" for keys and a comma "," for entries
     *
     * @return true if row was sucessfully saved in SQLite
     */
    public boolean saveForLater(String mobileBackendId, String jsonMessage, String delimitedHeaders) {
        boolean success = false;

        //get connection and ensure the analytic table exists. 
        Connection conn = ensureAnalyticTables();

        LibraryLogger logger = new LibraryLogger();

        logger.logFine("Trying to save analytic event for MBE Id: " + mobileBackendId, "AnalyticsDB", "saveForLater");
        logger.logFine("Analytic event message is: " + jsonMessage, "AnalyticsDB", "saveForLater");
        logger.logFine("Analytic headers: " + delimitedHeaders, "AnalyticsDB", "saveForLater");

        //ensure the analytic table is ready for updates
        if (conn != null) {
            try {
                logger.logFine("SQLite connection obtained", "AnalyticsDB", "saveForLater");
                //issue a prepared statement to ensure that any form of encoding used in the analytic event doesn't break the statement
                PreparedStatement pStmt = conn.prepareStatement(AnalyticsSQLHelper.ANALYTIC_TABLE_CONTENT_INSERT);
                pStmt.setString(1, mobileBackendId);
                pStmt.setString(2, jsonMessage);
                pStmt.setString(3, delimitedHeaders);
                pStmt.execute();

                logger.logFine("Statement processed for backendId "+mobileBackendId, "AnalyticsDB", "saveForLater");
                //statement close
                pStmt.close();
                //connection commit and close
                conn.commit();

                success = true;
                logger.logFine("Changes committed", "AnalyticsDB", "saveForLater");
                //release connection
                conn.close();

            } catch (SQLException sqlException) {
                logger.logError("Exception occured in update to SQLite database : " + sqlException.getMessage() +
                                "Error Code: " + sqlException.getErrorCode(), "AnalyticsDB", "saveForLater");
                try {
                    conn.close();
                } catch (Exception e) {
                    //we can't throw an excpetion here
                    logger.logError("Failed to close DB connection : " + e.getMessage(), "AnalyticsDB", "saveForLater");
                }
            }
        } else {
            logger.logError("Could not obtain handle to anayltic table : " + AnalyticsSQLHelper.ANALYTIC_TABLE_NAME, "AnalyticsDB", "saveForLater");
            logger.logError("Analytic events could not be saved", "AnalyticsDB", "saveForLater");
        }

        return success;
    }

    /**
     * method returns all pending analytic messages for a specified mobile backend Id from SQLite
     * @param mobileBackendId the mobile backedn Id for which messages are stored in SQLite
     * @return List of SavedMessages
     */
    public List<SavedMessage> getPendingMessages(String mobileBackendId) {

        LibraryLogger logger = new LibraryLogger();

        logger.logFine("Trying get pending messages for MBE with id \"" + mobileBackendId + "\" from SQLite",
                       "AnalyticsDB", "getPendingMessages");

        //get connection and ensure table does exist
        Connection conn = ensureAnalyticTables();

        ArrayList<SavedMessage> pendingMessages = new ArrayList<SavedMessage>();

        if (conn != null) {
            try {
                logger.logFine("SQL connection obtained", "AnalyticsDB", "getPendingMessages");
                PreparedStatement pStmt = conn.prepareStatement(AnalyticsSQLHelper.QUERY_PENDING_MESSAGES_FOR_BACKEND);
                pStmt.setString(1, mobileBackendId);
                ResultSet resultSet = pStmt.executeQuery();

                //if result set is not null and if result set has rows
                if (resultSet != null && resultSet.first() == true) {
                    //ensure cursor is set before first row
                    resultSet.previous();
                    logger.logFine("Resultset - cursor befor first row? " + resultSet.isBeforeFirst(), "AnalyticsDB",
                                   "getPendingMessages");
                    //now iterate over the result set to read saved messages. Note that the call to next() returns false if the cursor
                    //is positioned after the last row in the set
                    while (resultSet.next()) {
                        String payload = resultSet.getString("MESSAGES");
                        String headers = resultSet.getString("HEADERS");

                        logger.logFine("Get saved message from row number: " + resultSet.getRow(), "AnalyticsDB",
                                       "getPendingMessages");
                        logger.logFine("Saved message payload: " + payload, "AnalyticsDB", "getPendingMessages");
                        logger.logFine("Saved message headers: " + headers, "AnalyticsDB", "getPendingMessages");

                        SavedMessage message = new SavedMessage();
                        message.setPayload(payload);
                        message.setHeaderString(headers);

                        pendingMessages.add(message);
                    }

                    logger.logFine("Number of messages read: " + pendingMessages.size(), "AnalyticsDB",
                                   "getPendingMessages");

                } else {
                    logger.logFine("Query for saved events returned no records." +
                                   AnalyticsSQLHelper.ANALYTIC_TABLE_NAME, "AnalyticsDB", "getPendingMessages");
                }
                logger.logFine("Closing prepared statement and connection", "AnalyticsDB", "getPendingMessages");
                pStmt.close();
                conn.close();
            } catch (SQLException sqlException) {
                logger.logError("Exception occured in query to SQLite database : " + sqlException.getMessage() +
                                "Error Code: " + sqlException.getErrorCode(), "AnalyticsDB", "getPendingMessages");
                try {
                    conn.close();
                } catch (Exception e) {
                    //we can't throw an excpetion here
                    e.printStackTrace();
                }

            }
        }
        return pendingMessages;
    }


    /**
     * Deletes all messages from Analytic table. This statement actually drops the table so it is re-created with the next attempt to save messages for later delivery to MCS
     *
     * @return true if operation was performed successfully, false otherwise
     */
    public final boolean purgeAllAnalyticMessages() {
        LibraryLogger logger = new LibraryLogger();

        logger.logFine("Attempting to delete all messages from analytic table in SQLite", "AnalyticsDB","purgeAllAnalyticMessages");

        //get connection and ensure table does exist
        Connection conn = ensureAnalyticTables();

        boolean operationSuccess = false;

        if (conn != null) {
            try {
                //delete rows saved for specific mobile backend
                PreparedStatement pStmt = conn.prepareStatement(AnalyticsSQLHelper.DROP_ANALYTIC_TABLE);
                pStmt.executeUpdate();
                pStmt.close();
                conn.commit();
                logger.logFine("Deleted all messages from analytic table", "AnalyticsDB", "purgeAllAnalyticMessages");
                conn.close();
                operationSuccess = true;

            } catch (SQLException sqlExepction) {
                logger.logError("Exception when attempting to delete all analytic messages : " +
                                sqlExepction.getMessage() + "Error Code: " + sqlExepction.getErrorCode(), "AnalyticsDB",
                                "purgeAllAnalyticMessages");
                try {
                    conn.close();
                } catch (Exception e) {
                    //we can't throw an excpetion here
                    e.printStackTrace();
                }
            }
        }
        return operationSuccess;
    }

}
