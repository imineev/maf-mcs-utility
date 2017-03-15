package com.oracle.maf.sample.mcs.shared.log;

import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.logging.Level;

import oracle.adfmf.util.Utility;

/**
 *  *** INTERNAL USE ONLY  ***
 *
 * Mobile Backend (MBE) instance logger.
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class MBELogger implements UtilLogger {

    private static final String LOG_TAG = " MAF MCS Utility MBE Logger: ";

    //MBE backend Id (in case of basic authentication) or MBE client id in case of OAUTH 
    //authentication. This ID ensures that the log messages can be associated to a MBE instance
    String mobileBackendIdentifier = null;
    MBEConfiguration mbeConfiguration = null;

    public MBELogger(MBEConfiguration mbeConfig) {
        super();
        if (mbeConfig != null) {
            this.mbeConfiguration = mbeConfig;
            
            
            this.mobileBackendIdentifier = mbeConfig.getMobileBackendIdentifier();

        } else {
            throw new IllegalArgumentException("MBEConfiguration argument in call to MBELogger constructor cannot be null!");
        }
    }

    /**
     * Returns information about whether logs are written or not
     * @return true or false
     */
    public boolean isLoggingEnabled() {
        return mbeConfiguration.isLoggingEnabled();
    }

    /**
     * CONFIG messages are intended to provide a variety of static
     * configuration information, to assist in debugging problems
     */
    public void logConfig(String message, String className, String methodName) {
        if (Utility.ApplicationLogger.isLoggable(Level.CONFIG) && isLoggingEnabled()) {
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss");
            Utility.ApplicationLogger.logp(Level.CONFIG, dt1.format(new Date()) + LOG_TAG + className, methodName,"backendId: " + mobileBackendIdentifier + ": " + message);
        }
    }

    /**
     * FINE is a message level providing tracing information.
     */
    public void logFine(String message, String className, String methodName) {
        if ((Utility.ApplicationLogger.isLoggable(Level.FINE)) && isLoggingEnabled()) {
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss");
            Utility.ApplicationLogger.logp(Level.FINE, dt1.format(new Date()) + LOG_TAG + className, methodName,"backendId: " + mobileBackendIdentifier + ": " + message);
        }
    }

    /**
     * Indicating a serious failure. Describes events that are
     * of importance and will prevent normal program execution.
     */
    public void logError(String message, String className, String methodName) {
        if (Utility.ApplicationLogger.isLoggable(Level.SEVERE) && isLoggingEnabled()) {
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss");
            Utility.ApplicationLogger.logp(Level.SEVERE, dt1.format(new Date()) + LOG_TAG + className, methodName,"backendId: " + mobileBackendIdentifier + ": " + message);
        }
    }

    /**
     * Indicating a potential problem. Describes event that will
     * be of interest to end users or system managers
     */
    public void logWarning(String message, String className, String methodName) {
        if (Utility.ApplicationLogger.isLoggable(Level.WARNING) && isLoggingEnabled()) {
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss");
            Utility.ApplicationLogger.logp(Level.WARNING, dt1.format(new Date()) + LOG_TAG + className, methodName,"backendId: " + mobileBackendIdentifier + ": " + message);
        }
    }

    /**
     * Informational messages. Messages will be written to the console
     * or its equivalent.  Use for reasonably important messages that
     * make sense to end users and system administrators.
     */
    public void logInfo(String message, String className, String methodName) {
        if (Utility.ApplicationLogger.isLoggable(Level.INFO) && isLoggingEnabled()) {
            SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss");
            Utility.ApplicationLogger.logp(Level.INFO, dt1.format(new Date()) + LOG_TAG + className, methodName,"backendId: " + mobileBackendIdentifier + ": " + message);
        }
    }


}
