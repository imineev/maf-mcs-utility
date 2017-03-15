package com.oracle.maf.sample.mcs.shared.log;


/**
 *  *** INTERNAL USE ONLY  ***
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public interface UtilLogger {
    
    /**
     * CONFIG messages are intended to provide a variety of static
     * configuration information, to assist in debugging problems
     */
    public void logConfig(String message, String className, String methodName);
    
    /**
     * FINE is a message level providing tracing information.
     */
    public void logFine(String message, String className, String methodName);
    
    /**
     * Indicating a serious failure. Describes events that are
     * of importance and will prevent normal program execution.
     */
    public void logError(String message, String className, String methodName);
    
    /**
     * Indicating a potential problem. Describes event that will
     * be of interest to end users or system managers
     */
    public void logWarning(String message, String className, String methodName);
    
    /**
     * Informational messages. Messages will be written to the console
     * or its equivalent.  Use for reasonably important messages that 
     * make sense to end users and system administrators.
     */
    public void logInfo(String message, String className, String methodName);
}
