package com.oracle.maf.sample.mcs.shared.log;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.logging.Level;

import oracle.adfmf.util.Utility;

/**
 *  *** INTERNAL USE ONLY  ***
 *
 * Logger for all non-MBE instance specific logging
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class LibraryLogger implements UtilLogger{
    
    private final static String UTIL_LOGGER = " MAF MCS Utility Library Logger: ";
        
    public LibraryLogger() {
        super();
    }
    
    /**
     * CONFIG messages are intended to provide a variety of static
     * configuration information, to assist in debugging problems
     */
    public void logConfig(String message, String className, String methodName){
        if(Utility.ApplicationLogger.isLoggable(Level.CONFIG)){
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss");
            Utility.ApplicationLogger.logp(Level.CONFIG, UTIL_LOGGER+className, methodName,dt1.format(new Date())+": "+message);
        }
    }
    
    /**
     * FINE is a message level providing tracing information.
     */
    public void logFine(String message, String className, String methodName){
        if(Utility.ApplicationLogger.isLoggable(Level.FINE)){
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss");
            
          Utility.ApplicationLogger.logp(Level.FINE, UTIL_LOGGER+className, methodName,dt1.format(new Date())+": "+message);
        }
    }
    /**
     * Indicating a serious failure. Describes events that are
     * of importance and will prevent normal program execution.
     */
    public void logError(String message, String className, String methodName){
            if(Utility.ApplicationLogger.isLoggable(Level.WARNING)){
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss");
           Utility.ApplicationLogger.logp(Level.SEVERE, UTIL_LOGGER+className, methodName, dt1.format(new Date())+": "+message);
        }
    }
    /**
     * Indicating a potential problem. Describes event that will
     * be of interest to end users or system managers
     */
    public void logWarning(String message, String className, String methodName){
            if(Utility.ApplicationLogger.isLoggable(Level.WARNING)){
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss");
          Utility.ApplicationLogger.logp(Level.WARNING, UTIL_LOGGER+className, methodName, dt1.format(new Date())+": "+message);
        }
    }
    
    /**
     * Informational messages. Messages will be written to the console
     * or its equivalent.  Use for reasonably important messages that 
     * make sense to end users and system administrators.
     */
    public void logInfo(String message, String className, String methodName){
            if(Utility.ApplicationLogger.isLoggable(Level.INFO)){
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss");
          Utility.ApplicationLogger.logp(Level.INFO, UTIL_LOGGER+className, methodName, dt1.format(new Date())+": "+message);
        }
    }
    
}
