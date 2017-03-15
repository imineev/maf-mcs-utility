package com.oracle.maf.sample.mcs.apis.analytics.db;


/**
 * * SQL statements used with the Analytic table that holds pending analytic events
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class AnalyticsSQLHelper {   
    
    //table name
    public final static String ANALYTIC_TABLE_NAME          = "PENDING_ANALYTIC_MESSAGES";

    //create statement to create the table - just a simple table 
    public final static String CREATE_ANALYTIC_TABLE_IF_NOT_EXIST        = "CREATE TABLE IF NOT EXISTS "+ANALYTIC_TABLE_NAME+"(BACKEND_ID VARCHAR, MESSAGES VARCHAR, HEADERS VARCHAR);";
    
    //we need to check if there is a table and cannot always drop/create the table as it may have content
    public final static String DETECT_ANALYTIC_TABLE        = "SELECT name FROM sqlite_master WHERE type='table' AND name='"+ANALYTIC_TABLE_NAME+"';";
    
    //delete all content. This should be called as soon as a MCS update goes through well
    public final static String ANALYTIC_TABLE_CONTENT_DELETE   = "DELETE FROM "+ANALYTIC_TABLE_NAME+" WHERE BACKEND_ID = ?;";
    
    //save message
    public final static String ANALYTIC_TABLE_CONTENT_INSERT   = "INSERT INTO " + AnalyticsSQLHelper.ANALYTIC_TABLE_NAME+" (BACKEND_ID,MESSAGES,HEADERS) VALUES (?,?,?);";
    
    
    //query pending messages
    public final static String QUERY_PENDING_MESSAGES_FOR_BACKEND  = "SELECT MESSAGES, HEADERS FROM "+ANALYTIC_TABLE_NAME+" WHERE BACKEND_ID = ?;";
    
    public final static String DROP_ANALYTIC_TABLE = "DROP TABLE "+ANALYTIC_TABLE_NAME+";";
    
            
    public AnalyticsSQLHelper() {
        super();
    }
}
