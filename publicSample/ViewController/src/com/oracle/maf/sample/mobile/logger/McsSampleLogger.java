package com.oracle.maf.sample.mobile.logger;


import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.logging.Level;

import oracle.adfmf.util.Utility;

/**
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class McsSampleLogger{
    
    private final static String LOGTAG = "MAF MCS Utility Sample: ";
        
    public McsSampleLogger() {
        super();
    }
    
    
    /**
     * FINE is a message level providing tracing information.
     */
    public void logFine(String message, String className, String methodName){
        if(Utility.ApplicationLogger.isLoggable(Level.FINE)){
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss");
          Utility.ApplicationLogger.logp(Level.FINE, LOGTAG+className, methodName,methodName,dt1.format(new Date())+": "+message);
        }
    }
    /**
     * Indicating a serious failure. Describes events that are
     * of importance and will prevent normal program execution.
     */
    public void logError(String message, String className, String methodName){
            if(Utility.ApplicationLogger.isLoggable(Level.WARNING)){
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss");
           Utility.ApplicationLogger.logp(Level.SEVERE, LOGTAG+className, methodName, methodName,dt1.format(new Date())+": "+message);
        }
    }
    /**
     * Indicating a potential problem. Describes event that will
     * be of interest to end users or system managers
     */
    public void logWarning(String message, String className, String methodName){
            if(Utility.ApplicationLogger.isLoggable(Level.WARNING)){
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss");
          Utility.ApplicationLogger.logp(Level.WARNING, LOGTAG+className, methodName, methodName,dt1.format(new Date())+": "+message);
        }
    }
    

    
}

