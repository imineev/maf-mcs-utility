package com.oracle.maf.sample.application.push;


/**
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */

public class PushConstants {
    public PushConstants() {
        super();
    }
    
    public final static String PUSH_MESSAGE             =   "#{applicationScope.push_message}";
    public final static String MCS_REGISTRATION_STRING  =   "#{applicationScope.mcsregistrationString}";
    public final static String PUSH_ERROR               =   "#{applicationScope.push_errorMessage}";
    public final static String PUSH_DeviceTOKEN         =   "#{applicationScope.deviceToken}";   
    public final static String PUSH_ENABLED             =   "#{preferenceScope.application.push.enablePush}";
    public final static String APPLE_BUNDLE_ID          =   "#{preferenceScope.application.push.appleBundleId}";
    public final static String GOOGLE_PACKAGE_NAME      =   "#{preferenceScope.application.push.googleApplicationPackage}";
    public final static String GOOGLE_SENDER_ID         =   "#{preferenceScope.application.push.gcmSenderId}";
    
    
    
    
}
