package com.oracle.maf.sample.mcs.shared.sqlite;


/**
 *
 *  *** INTERNAL USE ONLY  ***
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 *
 */
public class DatabaseConstants {
    
    /**
     * Name of the SQLite database that is created by the MAF MCS utils library for supporting the offline usecase
     */
    public final static String MAFMCS_UTILITY_DB_NAME = "maf_mcs_utility_offline_db.db";
    /**
     * The SQLite database password to encrypt the database can be generated in MAF. For this you use a username that 
     * then has an entry in MAF to retrieve the password (to avoid hard coding of the password, which would be seen a
     * security risk)
     */
    public final static String MAFMCS_UTIL_DBUSER = "mafmcuser";

    
    private DatabaseConstants() {}
}
