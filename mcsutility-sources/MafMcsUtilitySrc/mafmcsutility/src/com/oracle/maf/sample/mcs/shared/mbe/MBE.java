package com.oracle.maf.sample.mcs.shared.mbe;

import com.oracle.maf.sample.mcs.apis.analytics.Analytics;
import com.oracle.maf.sample.mcs.apis.custom.CustomAPI;
import com.oracle.maf.sample.mcs.apis.diagnostics.Diagnostics;
import com.oracle.maf.sample.mcs.apis.notifications.Notifications;
import com.oracle.maf.sample.mcs.apis.policy.AppPolicies;
import com.oracle.maf.sample.mcs.apis.storage.Storage;
import com.oracle.maf.sample.mcs.apis.userinfo.UserInfo;
import com.oracle.maf.sample.mcs.shared.authorization.auth.Authorization;
import com.oracle.maf.sample.mcs.shared.authorization.basicauth.BasicAuthentication;
import com.oracle.maf.sample.mcs.shared.authorization.oauth.OauthAuthorization;
import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.headers.HeaderConstants;
import com.oracle.maf.sample.mcs.shared.log.MBELogger;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSResponse;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRestClient;
import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;
import com.oracle.maf.sample.mcs.shared.mbe.config.internal.InternalMBEConfig;
import com.oracle.maf.sample.mcs.shared.mbe.constants.MBEConstants;
import com.oracle.maf.sample.mcs.shared.mbe.error.OracleMobileErrorHelper;
import com.oracle.maf.sample.mcs.shared.mbe.proxy.MBEServiceProxy;

import java.util.HashMap;

import oracle.adfmf.framework.api.AdfmfContainerUtilities;
import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.framework.api.JSONBeanSerializationHelper;
import oracle.adfmf.framework.exception.IllegalArgumentException;
import oracle.adfmf.json.JSONException;
import oracle.adfmf.json.JSONObject;


/**
 * Mobile backend (MBE) is a core concept in Mobile Cloud Service (MCS) and assembles APIs to use for a specific set of mobile
 * applications. MBE is a container within MCS that provides mobile applications like applications built with Mobile Application
 * Framework (MAF) access to services through protected platform SDK APIs that are accessible through REST.
 *
 * This MBE class represents a client object new instances are created of for each MBE that an application needs to access. Usually
 * this is one. Available proxy classes are listed in a public ENUM ProxyClass
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public final class MBE {

    private String mobileBackendName = "";
    private Diagnostics mDiagnostics = null;
    private MBEConfiguration mbeConfiguration = null;
    private Authorization mAuthorizationProvider = null;
    private String mFeatureName = "Unknown Application/Feature Name";
    //try to get the UUID through Cordoval. If this fails, generate a unique device ID
    private String clientUID =   AdfmfJavaUtilities.evaluateELExpression("#{deviceScope.device.uuid}")!=null? (String) AdfmfJavaUtilities.evaluateELExpression("#{deviceScope.device.uuid}"): java.util.UUID.randomUUID().toString(); 
    private MBELogger mLogger = null;
    private String mafAppName = "";

    //service proxy instance cache
    private HashMap<String, MBEServiceProxy> mServiceProxies = new HashMap<String, MBEServiceProxy>();


    /**
     * ENUM class that exposes the class and package name reference for the proxy class to open (instantiate). Note that
     * all proxy classes are instantiated as a singleton
     */
    public enum ProxyClass {

        ANALYTICS       ("com.oracle.maf.sample.mcs.apis.analytics.Analytics"),
        NOTIFICATIONS   ("com.oracle.maf.sample.mcs.apis.notifications.Notifications"),
        STORAGE         ("com.oracle.maf.sample.mcs.apis.storage.Storage"),
        USERINFO        ("com.oracle.maf.sample.mcs.apis.userinfo.UserInfo"),
        CUSTOM          ("com.oracle.maf.sample.mcs.apis.custom.CustomAPI"),
        POLICY          ("com.oracle.maf.sample.mcs.apis.policy.AppPolicy");

        private String canonicalClassname;

        private ProxyClass(String value) {
            this.canonicalClassname = value;
        }


        public String getCanonicalClassname() {
            return canonicalClassname;
        }
    };


    /**
     * Creates an instance of MobileBackend (MBE) that represents a mobile backend instance in MCS
     *
     * @param mobileBackendName  (required) the name of the mobile backend instance. The name of the instance does not need to match the name of the mobile backend in MCS. The name information is used for logging and in analytics so that using the same name as the mobile backend has may be seen as good practice
     * @param restConnectionName (required) the name of the MAF RESR Connection definition
     * @param mbeConfig          (required) a configuration object that contains information about the MBE instance in MCS (e.g. mibile backend id)
     * 
     */
    public MBE(String mobileBackendName, MBEConfiguration mbeConfig){
        super();
        
        if (mobileBackendName == null || mobileBackendName.isEmpty() || mbeConfig == null) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
            illegalArgumentException.setMessage("mobileBackendName and mbeConfig arguments cannot be null or empty in call to MBE consructor. The mobileBackendname is \"" + mobileBackendName +"\" and the "+
                                                " mbeConfig is  = " + (mbeConfig == null ? "null" : "not null"));     
            throw illegalArgumentException;
        }

        //create isolated instance of mbe configuration
        this.mbeConfiguration = new InternalMBEConfig(mbeConfig);
        mLogger =  this.mbeConfiguration.getLogger();
        this.mobileBackendName = mobileBackendName;
        mDiagnostics = new Diagnostics(this);

        //The MCS SDK for Android uses Android component names as an identifier for Analytics.
        //Android components map best to MAF features. Because MAF features may be provided from 
        //a Feature Archive (FAR), the application name is added as a pre-fix to ensure a unique name.
        String featureName = AdfmfJavaUtilities.getFeatureName();
        mafAppName = AdfmfContainerUtilities.getApplicationInformation().getName();
        this.mFeatureName = mafAppName + "::" + featureName;

        /*
         * OAUTH or BASIC authentication. The authentication can be through the MAF Feature (security provider) or
         * manual, by a call to "authenticate" in the authorization object.
         */
        if (mbeConfiguration.getAuthtype().equalsIgnoreCase(MBEConstants.OAUTH_AUTH)) {
            mAuthorizationProvider = new OauthAuthorization(this);

        } else {
            mAuthorizationProvider = new BasicAuthentication(this);
        }
    }


    public void setMbeConfiguration(MBEConfiguration mbeConfiguration) {
        this.mbeConfiguration = mbeConfiguration;
    }

    public MBEConfiguration getMbeConfiguration() {
        return mbeConfiguration;
    }

    /**
     * convenience  typed method to access the AppPolicy proxy class
     * @return AppPolicy
     */
    public AppPolicies getServiceProxyAppPolicy(){
        return (AppPolicies) this.getServiceProxy(ProxyClass.POLICY);
    }
    /**
     * convenience  typed method to access the Analytics proxy class
     * @return Analytics
     */
    public Analytics getServiceProxyAnalytics(){
        return (Analytics) this.getServiceProxy(ProxyClass.ANALYTICS);
    }
    /**
     * convenience  typed method to access the Storage proxy class
     * @return Storage
     */
    public Storage getServiceProxyStorage(){
        return (Storage) this.getServiceProxy(ProxyClass.STORAGE);
    }
    /**
     * convenience  typed method to access the UserInfo proxy class
     * @return UserInfo
     */
    public UserInfo getServiceProxyUserInfo(){
        return (UserInfo) this.getServiceProxy(ProxyClass.USERINFO);
    }
    /**
     * convenience  typed method to access the CustomAPI proxy class
     * @return CustomAPI
     */
    public CustomAPI getServiceProxyCustomApi(){
        return (CustomAPI) this.getServiceProxy(ProxyClass.CUSTOM);
    }
    
    /**
     * convenience typed method to access the Notifications proxy class
     * @return Notifications
     */
    public Notifications getServiceProxyNotifications(){
        return (Notifications) this.getServiceProxy(ProxyClass.NOTIFICATIONS);
    }

    /**
     * Method returns the MBE instance wrapped in a Java object that exposes additional APIs for MCS services- For example,
     * Analytics.class acts as proxy for the MBE analytic API.
     * @param <T>
     * @param proxyServiceType A string representation of the proxy class to obtain a handle of. The available proxy class names are listed in ProxyClass ENUM
     * @return an instance of the proxy class for the requested service type
     * @see com.oracle.maf.sample.mcs.shared.mbe.proxy.MBEServiceProxy
     */
    @SuppressWarnings("unchecked")
    public <T extends MBEServiceProxy> T getServiceProxy(ProxyClass proxyServiceType) {
        
        Object mbeProxyClass = null;

        //check if there is an existing instance of the requested proxy class. If mServiceProxies is not empty and if the
        //key for the service proxy is found in the mServiceProxies MAP then use it. Else, create a new instance and save
        //it in mServiceProxies
        if (!this.mServiceProxies.isEmpty() && this.mServiceProxies.containsKey(proxyServiceType.getCanonicalClassname())) {
            String proxyClassLookup = proxyServiceType.getCanonicalClassname();
            mbeProxyClass = (T) this.mServiceProxies.get(proxyClassLookup);
        }
        //Service proxy doesn't exist - so lets create one
        else {                          
         switch (proxyServiceType) {
           case ANALYTICS:
              mbeProxyClass = new Analytics();
              break;
           case NOTIFICATIONS:
              mbeProxyClass = new Notifications();
              break;
           case STORAGE:
             mbeProxyClass = new Storage();
             break;
           case USERINFO:
             mbeProxyClass = new UserInfo();
             break;
           case CUSTOM:
               mbeProxyClass = new CustomAPI();
              break;          
            case POLICY:
                mbeProxyClass = new AppPolicies();
               break;
          }
                
          //add mobile bakend reference to the proxy class
          ((T)mbeProxyClass).setMbe(this);
        
          //cache instance for next request                
          this.mServiceProxies.put(proxyServiceType.getCanonicalClassname(), (T)mbeProxyClass);
          
        }
        return (T)mbeProxyClass;
    }


    /**
     * Basic Authorization in MCS v1.0, as well as OAUTH2 in a later version
     * @param mAuthorization
     */
    public void setAuthorizationProvider(Authorization mAuthorization) {
        this.mAuthorizationProvider = mAuthorization;
    }

    /**
     * mAuthorizationProvider object is called from MAF application to authenticate the MBE access
     * @return Authorization object, BASIC or OAUTH (MCS v1.1) or null if authentication is handled through MAF. 
     */
    public Authorization getAuthorizationProvider() {
        return mAuthorizationProvider;
    }

    /**
     * The MCS SDK for Android uses Android component names as an identifier for Analytics. Android components map best
     * to MAF features. Because MAF features may be provided from Feature Archive (FAR), the application name is added 
     * as a pre-fix to ensure a unique name. The above logic is executed when the MBE is instantiated and all information
     * that is required are accessed throughMAF APIs. MAF developers don't need to call this setter method. 
     * 
     * @param featureName
     */
    public void setApplicationFeatureName(String featureName) {
        this.mFeatureName = featureName;
        mFeatureName = mafAppName + "::" + featureName;
    }

    /**
     * Name string that uniquy identifies a Feature within a MAF application for use by MCS Analytics
     * @return
     */
    public String getApplicationFeatureName() {
        return mFeatureName;
    }

    
    /**
     * The utility will attempt to read the UUID value using a Cordova call: AdfmfJavaUtilities.evaluateELExpression("#{deviceScope.device.uuid}").
     * If it cannot be retrievd, a random ID is generated: java.util.UUID.randomUUID().toString();. If a MAF application wants to use a custom
     * value, use this method
     * @param clientUID
     */
    public void setClientUID(String clientUID) {
        this.clientUID = clientUID;
    }

    /**
     * The utility will attempt to read the UUID value using a Cordova call: AdfmfJavaUtilities.evaluateELExpression("#{deviceScope.device.uuid}").
     * If it cannot be retrievd, a random ID is generated: java.util.UUID.randomUUID().toString();. Get the UUID value here
     * @return
     */
    public String getClientUID() {
        return clientUID;
    }


    public void setDiagnostics(Diagnostics diagnostics) {
        this.mDiagnostics = diagnostics;
    }

    /**
     * Access to the Diagnostic instance associated with this mobile backend
     * @return Diagnostics
     */
    public Diagnostics getDiagnostics() {
        return mDiagnostics;
    }

    //Logging can be enabled or disabled at runtime
    public void setLoggingEnabled(boolean loggingEnabled) {
        this.mbeConfiguration.setLoggingEnabled(loggingEnabled);
    }

    public boolean isLoggingEnabled() {
        return this.mbeConfiguration.isLoggingEnabled();
    }
    
    
    /**
     * This method analyzes the exception for instances of AdfInvocationRuntimeException, AdfInvocation-Exception and, 
     * more broadly, AdfExceptions. If none of the two are found, it will look into the exception message for status 
     * codes known returned by the API called by the utility. If still the exception isn't identified as Oracle MCS, it 
     * will be rethrown with a status code of -1. 
     * 
     * @param e    the exception thrown by Oracle ADF. 
     * @param uri  the origin uri. This uri is used if the error message is composed based on error code findings in the exception
     *             message
     */
    private void handleExceptions(Exception e,String uri) throws ServiceProxyException {
        
         //Step 1: Is error AdfInvocationRuntimeException, AdfInvocationException or AdfException?        
        String exceptionPrimaryMessage      = e.getLocalizedMessage();
        String exceptionSecondaryMessage    = e.getCause() != null? e.getCause().getLocalizedMessage() : null;
         String combinedExceptionMessage =  "primary message:"+exceptionPrimaryMessage+(exceptionSecondaryMessage!=null?("; secondary message: "+exceptionSecondaryMessage):(""));
        
        //chances are this is the Oracle MCS error message. If so then ths message has a JSON format. A simple JSON parsing 
        //test will show if our assumption is true. If JSONObject parsing fails then apparently the message is not the MCS
        //error message
        //{
        //  "type":".....",
        //    *  "status": <error_code>,
        //    *  "title": "<short description of the error>",
        //    *  "detail": "<long description of the error>",
        //    *  "o:ecid": "...",
        //   *  "o:errorCode": "MOBILE-<MCS error number here>",
        //   *  "o:errorPath": "<URI of the request>"
        // }
        if(exceptionSecondaryMessage!=null){
            try {
                JSONObject jsonErrorObject = new JSONObject(exceptionSecondaryMessage);
                //if we get here, then its a Oracle MCS error JSON Object. Get the 
                //status code or set it to 0 (means none is found)
                int statusCode = jsonErrorObject.optInt("status", 0);
                throw new ServiceProxyException(statusCode, exceptionSecondaryMessage);
                
            } catch (JSONException jse) {
                //if parsing fails, the this is proof enough that the error message is not 
                //an Oracle MCS message and we need to continue our analysis
                
                this.getMbeConfiguration().getLogger().logFine("Exception message is not a Oracle MCS error JSONObject", this.getClass().getSimpleName(), "handleExcpetion");
             }            
          }
        
          //continue message analysis and check for known error codes for the references MCS API
        
            this.getMbeConfiguration().getLogger().logFine("Rest invocation failed with following message"+exceptionPrimaryMessage, this.getClass().getSimpleName(), "handleExcpetion");
        
            int httpErrorCode = -1; 
            String restoredOracleMcsErrorMessage = null;
        
            /*
             *  Try to identify an MCS failure from the exception message.
             */
            if(combinedExceptionMessage.contains("400")){
                httpErrorCode = 400; 
                restoredOracleMcsErrorMessage =
                OracleMobileErrorHelper.createOracleMobileErrorJson(400, "Invalid JSON payload", "The request failed the request body is not well-formed JSON or an exception occurred during processing.", uri);
            }
            else if(combinedExceptionMessage.contains("405")){
                httpErrorCode = 405; 
                restoredOracleMcsErrorMessage =
                OracleMobileErrorHelper.createOracleMobileErrorJson(401, "Unsupported Method", "The request failed because it uses a method that is not supported by the resource.",uri);                
            }
           
            
            else{
                this.getMbeConfiguration().getLogger().logFine("Request failed with Exception: "+e.getClass().getSimpleName()+"; message: "+e.getLocalizedMessage(), this.getClass().getSimpleName(), "handleExcpetion");
                throw new ServiceProxyException(e.getLocalizedMessage(), ServiceProxyException.ERROR);
            }
            //if we get here then again its an Oracle MCS error, though one we found by inspecting the exception message
            this.getMbeConfiguration().getLogger().logFine("Request succeeded successful but failed with MCS application error. HTTP response: "+httpErrorCode+", Error message: "+restoredOracleMcsErrorMessage, this.getClass().getSimpleName(), "handleExcpetion");
           throw new ServiceProxyException(httpErrorCode, restoredOracleMcsErrorMessage);
    }
}
