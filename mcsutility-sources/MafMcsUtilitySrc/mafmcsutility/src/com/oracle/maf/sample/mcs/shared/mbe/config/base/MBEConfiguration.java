package com.oracle.maf.sample.mcs.shared.mbe.config.base;

import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.log.MBELogger;
import com.oracle.maf.sample.mcs.shared.mbe.constants.MBEConstants;

import oracle.adf.model.datacontrols.device.DeviceManagerFactory;

import oracle.adfmf.dc.ws.rest.RestServiceAdapter;
import oracle.adfmf.framework.api.AdfmfContainerUtilities;
import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.framework.api.Model;
import oracle.adfmf.framework.exception.IllegalArgumentException;
import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;
import oracle.adfmf.share.security.SecurityContext;


/**
 *
 * *** FOR INTERNAL USE ONLY ***
 *
 * Each MobileBackend instance needs to be configured with a basic set of information regarding the server side
 * Mobile Cloud Server (MCS) Mobile Backend (MBE) it represents. The MBEConfiguration object cannot be shared
 * between MBE (Mobile Backend Instance) in MAF MCS Utility but is accessible at runtime for each MBE instance.
 * <p>
 * The MBEConfiguration object allows MAF developers to enable/disable features like sending analytic events,
 * MBE specific logging (to filter log messages for a specific MBE during dignosing application problems). The
 * configuration file also exposes the configuration under which a MBE currently executes.
 *
 * MBEConfiguration is for MAF MCS internal use and backward compatibility to MAF MCS Utility 2.1.3. Developers 
 * should use the BasicAuthMBEConfiguration or OauthMBEConfiguration classes that both extend MBEConfiguration. 
 * The two classes support basic authentication as well as OAUTH authentication, which differ in their configuration
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class MBEConfiguration {

    private String mafApplicationName = AdfmfContainerUtilities.getApplicationInformation().getName();
    private String mafApplicationId = AdfmfContainerUtilities.getApplicationInformation().getId();
    private String mafApplicationVendorName = AdfmfContainerUtilities.getApplicationInformation().getVendor();
    private String mafApplicationVersion = AdfmfContainerUtilities.getApplicationInformation().getVersion();
    
    //time stamp value of the OAUTh expiration time in MS
    private long oauthTokenExpiryTimeInMilliSeconds = 0;

    /**
     * Unique identifier for the device to allow e.g. analytics to distinguish different devices. UUID with Cordva
     * seems not to be reliable for identifying client devices. Due to security settings, this information  may not 
     * even be available. This utility attempts to obtain the device UUID. In case this fails the device ID is set
     * a default value obtained by DeviceManagerFactory.getDeviceManager().getName(); which brings up the name of the 
     * device as defined by the user (e.g. John's iPhone). 
     * 
     * Application developers can override this value to access whatever he/she feels is the right value to set to identify a device.
     */        
    private String mobileDeviceId = DeviceManagerFactory.getDeviceManager().getName();
    private String mMobileBackendClientApplicationKey = null;

    /* required */
    private String mafRestConnectionName = null;

    /* required
     *
     * MCS generatess authentication keys for mobile backends in the form of a backend ID and an anonymous access key (or
     * the client Id and client secret if OAUTH is used). The values are unique for a mobile backend. All mobile client 
     * applications that are registered for a mobile backend share the credentials. MAF applications need to make the keys 
     * available to the MBE at runtime, which is why they are stored in the MBE configuration object.
     * 
     * The mobile backend identifier is either the mobile backend Id used woth basic authentication or the client Id used
     * with OAUTH2
     */
    private String mobileBackendIdentifier = null;


    /*
     * required. The anonymous key either is the anonymous key used with basic authentication, or the client secret that is 
     * used with OAUTH authentication. The variable is referenced internally when performing unauthenticated authorization.
     */
    private String mAnonymousKey = null;


    /* required for OAUTH */
    private String mOauthEndpointConnectionName = null;
    private String mOauthIdentityDomain = null;
    
    
    /**
     * Analytic events are not sent to the srever if this flag is set to false, which is the default setting
     */
    private boolean mEnableAnalytics = false;

    
    /*
     * For registering the mobile client to receove ush notifications from Apple, we need to pass the bundle Id
     * to MCS. Note that the bundle Id is not equivalent to the application Id as it is determined by the Apple 
     * certificate used to configure push on the Apple notification server
     */
    private String mAppleBundleId = null;
    
    //Google wants is package name here. The MAF application ID becomes the Google bundle ID unless changed in the deployment
    //profile. If this is the case, then this variable must be set to the custom value
    private String mGooglePackageName = null;

    /*
     * In Oracle MAF, the framework registers the mobile device with Google Cloud Messaging (GCM) or Apple Push Notification 
     * Service (APNS) to receive a device token. Upon successful registration the device token can be obtained in the push 
     * listener (a class that implements import oracle.adfmf.framework.event.EventListener) "onOpen" method
     */
    private String mDeviceToken = null;

    /*
     * Client logging is disabled by default
     */
    private boolean mLoggingEnabled = MBEConstants.LOGGING_DISABLED;

    private MBELogger mLogger = null;

    /**
     * Authentication is performed on the MAF feature level. However, developers can override the
     * feature level authentication by manual authentication using e.g the BasicAuthorization class
     */
    private boolean manualAuthenticationFlag = false;
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }


    /**
     * Oracle MCS v 1.1 supports basic authentication oand OAUTH
     */
    public enum AuthenticationType {
        BASIC_AUTH{
            @Override
            public String toString() {
                return MBEConstants.BASIC_AUTH;
            }
        },
        OAUTH_AUTH{
            @Override
            public String toString() {
                return MBEConstants.OAUTH_AUTH;
            }
        }
    }
    
    //default auth type configured for this MBE Configuration
    private String mAuthtype = MBEConstants.BASIC_AUTH;
    
    //For manual authentication the Authorization header parameter needs to be added to each REST request. This 
    //parameter either holds the basic auth or oauth token
    private String mAuthorizationToken = null;
    
    //the authenticated username
    private String mAuthenticatedUsername = null;
    
    //authenticatedUserMCSUserId : the id that is saved in MCS for a specific user. This ID is required to query collections and objects 
    //on behalt of the user
    String authenticatedUserMCSUserId = null;

    private MBEConfiguration() {
        super();
    }
    
    //Instance initialization block runs before the constructor is called each time this class is instantiated. Note 
    //that it doesn't matter which constructor is called. This utilty class uses the constructor to obrain the UUID of
    //the device. If we don't get a value back, then the default value is kept. Else the obtained UUID value is set
    {
        Object uuid = (String) AdfmfJavaUtilities.getELValue("#{deviceScope.device.uuid}");
        if(uuid != null){
            try{
               String uidString = (String) uuid;
                //only add UUID if the string has content. 
                if(!uidString.isEmpty()){
                   this.setMobileDeviceId(uidString);
                }
            }
            //just in case the UUID object cannot be casted into a string
            catch(Exception e){
                //do nothing here as the default value is set already
            }
        }
    }
    
    
    /**
     * *** FOR INTERNAL USE ONLY ***
     * 
     * Creates an instance of Mobile Backend Configuration. This constructor is used by 
     * the MBE object to create a copy of the configuration that is not dependent on the 
     * external object passed in to it
     */ 
    protected MBEConfiguration(MBEConfiguration mbeConfig){     
        mafRestConnectionName = mbeConfig.getMafRestConnectionName();
        mobileBackendIdentifier = mbeConfig.getMobileBackendIdentifier();
        mAnonymousKey = mbeConfig.getAnonymousKey();
        mMobileBackendClientApplicationKey = mbeConfig.getMobileBackendClientApplicationKey();
        mafRestConnectionName = mbeConfig.getMafRestConnectionName();
        mEnableAnalytics = mbeConfig.isEnableAnalytics();
        mAppleBundleId = mbeConfig.getAppleBundleId();
        mGooglePackageName = mbeConfig.getGooglePackageName();
        mLoggingEnabled = mbeConfig.isLoggingEnabled();
        mLogger = mbeConfig.getLogger();
        manualAuthenticationFlag = mbeConfig.isManualAuthentication();
        mAuthtype = mbeConfig.getAuthtype();
        mAuthorizationToken = mbeConfig.getAuthorizationToken();
        mAuthenticatedUsername = mbeConfig.getAuthenticatedUsername();
        mobileDeviceId = mbeConfig.getMobileDeviceId();
        mafApplicationName = mbeConfig.getMafApplicationName();
        mafApplicationId = mbeConfig.getMafApplicationId();
        mafApplicationVendorName = mbeConfig.getMafApplicationVendorName();
        mafApplicationVersion = mbeConfig.getMafApplicationVersion();
        mDeviceToken = mbeConfig.getDeviceToken();
        mOauthIdentityDomain = mbeConfig.getOauthIdentityDomain();
        
        oauthTokenExpiryTimeInMilliSeconds = mbeConfig.getOauthTokenExpiryTimeInMilliSeconds();
        mOauthEndpointConnectionName = mbeConfig.getOauthEndpointConnectionName();
        
        this.mLogger.logFine("Protected contructor called", this.getClass().getSimpleName(),
                             "MBEConfiguration(MBEConfiguration mbeConfig)");
    }
       
    /**
     * Creates an instance of Mobile Backend Configuration for basic authentication. Each instance contains the configuration for an instance
     * of MobileBackend, allowing the MobileBackend instance to access the remore MBE in MCS. 
     * 
     * <b>Note</b> that new MAF MCS Utility development projects should use the BasicAuthMBEConfiguration object. This 
     * MBEConfiguration constructor for basic authentication is left in for backward compatibility with MAF MCS Utility
     * version 2.1.3, which supported basic authentication only. 
     *
     * @param mafRestConnectionName Name of the REST connection creat in the MAF application. The REST connection must contain the base URL of the MBE
     * @param mobileBackendId Unique string identifier for the remote Mobile Backend in MCS. Use the MCS portal to obtain the ID
     * @param mbeAnonymousKey Unique string identifier used with analytics and diagnostics. Use the MCS portal to obtain the ID
     * @param mbeClientApplicationKey application client key created in the MCS MBE for Android and iOS. The key is required for the Analytic feature. Note that the key values are different for Android and iOS. Ensure the correctkey to be passed at runtime.
     *
     * @throws IllegalArgumentException if mafRestConnectionName or mobileBackendIdentifier is null or empty. Note that these two vaues are the minimal configuration required to instantiate an MBE instance
     */
    public MBEConfiguration(String mafRestConnectionName, String mobileBackendId, String mbeAnonymousKey, String mbeClientApplicationKey) throws IllegalArgumentException {
   
        this.mafRestConnectionName = mafRestConnectionName;
        this.mobileBackendIdentifier = mobileBackendId;
        this.mAnonymousKey = mbeAnonymousKey;
        this.mMobileBackendClientApplicationKey = mbeClientApplicationKey;
        this.mAuthtype = MBEConstants.BASIC_AUTH;             
    
        if (mafRestConnectionName == null || mafRestConnectionName.isEmpty() || mobileBackendId == null || mobileBackendId.isEmpty()) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
            illegalArgumentException.setMessage("mobileBackendId and mafRestConnectionName arguments cannot be null or empty in call to MBEConfiguration consructor." +
                                                "mobileBackendId is \"" + mobileBackendId +"\""+                    
                                                "REST connection name  is \"" +mafRestConnectionName+"\"");     
            throw illegalArgumentException;
        }

        //initialize logger with LOG_NONE. The log level can be change through a call to setLogLevel
        this.mLogger = new MBELogger(this);
    }
    
    /**
     * Creates an instance of Mobile Backend Configuration for OAUTH authentication. Each instance contains the configuration for an instance
     * of MobileBackend, allowing the MobileBackend instance to access the remore MBE in MCS.
     *
     * @param mafRestConnectionName Name of the REST connection creat in the MAF application. The REST connection must contain the base URL of the MBE
     * @param oAuthTokenEndpoint , the URL MAF MCS Utility can use to perform OAUTH authenticaion. If authentication is through MAF then this value can be set to null
     * @param oAuthClientId Unique string identifier for the remote OAUTH client (MBE)
     * @param oAuthClientKey client secret used for authenticating MAF MCS Utility MBE to the MCS OAUTH server
     * @param mbeClientApplicationKey application client key created in the MCS MBE for Android and iOS. The key is required for the Analytic feature. Note that the key values are different for Android and iOS. Ensure the correctkey to be passed at runtime.
     * @param identityDomain The Oracle Cloud identity domain (not the realm) that the user is associated with 
     *
     * @throws IllegalArgumentException if mafRestConnectionName or mobileBackendIdentifier is null or empty. Note that these two vaues are the minimal configuration required to instantiate an MBE instance
     */
    protected MBEConfiguration(String mafRestConnectionName, String oauthEndpointConnectionName, String oAuthClientId, String oAuthClientKey, String mbeClientApplicationKey, String identityDomain) throws IllegalArgumentException {

        if (mafRestConnectionName == null || mafRestConnectionName.isEmpty() || oAuthClientId == null || 
            oAuthClientId.isEmpty()  || oAuthClientKey == null || oauthEndpointConnectionName.isEmpty() ||
            oAuthClientKey == null || oauthEndpointConnectionName.isEmpty()) {
            
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
            illegalArgumentException.setMessage("oAuthClientId, oauthEndpointConnectionName, oAuthClientKey, and mafRestConnectionName arguments cannot be null or empty in call to MBEConfiguration consructor." +
                                                "oAuthClientId is \"" + oAuthClientId +" \"REST connection name  is \""+mafRestConnectionName+" oauthEndpointConnectionName is \""+oauthEndpointConnectionName+"/");     
            throw illegalArgumentException;
            
        }

        this.mafRestConnectionName = mafRestConnectionName;
        this.mAnonymousKey = oAuthClientKey;
        this.mobileBackendIdentifier = oAuthClientId;
        this.mMobileBackendClientApplicationKey = mbeClientApplicationKey;
        this.mOauthEndpointConnectionName = oauthEndpointConnectionName;
        this.mAnonymousKey = oAuthClientKey;
        this.mOauthIdentityDomain = identityDomain;
        
        this.mAuthtype = MBEConstants.OAUTH_AUTH;             
        
        //initialize logger with LOG_NONE. The log level can be change through a call to setLogLevel
        this.mLogger = new MBELogger(this);
    }
    
    
    public String getMafApplicationName() {
        return mafApplicationName;
    }

    public String getMafApplicationId() {
        return mafApplicationId;
    }


    /**
     * Sets the authentication type. 
     */
    private void setAuthtype(String authtype) throws ServiceProxyException{

        if(authtype.equalsIgnoreCase(AuthenticationType.BASIC_AUTH.toString())){
            this.mAuthtype = AuthenticationType.BASIC_AUTH.toString();            
            this.mLogger.logFine("Authentication type set to: "+mAuthtype, this.getClass().getSimpleName(), "setAuthtype");
        }
        else if(authtype.equalsIgnoreCase(AuthenticationType.OAUTH_AUTH.toString())){
            this.mAuthtype = AuthenticationType.OAUTH_AUTH.toString();
            this.mLogger.logFine("Authentication type set to: "+mAuthtype, this.getClass().getSimpleName(), "setAuthtype");
        }
        else{
            throw new ServiceProxyException("Unsupported authentication type: "+authtype, ServiceProxyException.WARNING);
        }
        this.mLogger.logFine("Authentication type is: "+mAuthtype, this.getClass().getSimpleName(), "setAuthtype");        
    }

    /**
     * OAUTH or BASIC authentication. The authentication can be through the MAF Feature (security provider) or
     * manual, by a call to "authenticate" in the authorizationProvider object.
     *
     * @return "basic" or "oauth"
     */
    public String getAuthtype() {
        return mAuthtype;
    }

    /**
     * Each MBE operates a mobile backend on the MCS server. The backend ID allows the MobileBackend instance of the
     * MAF MCS Utility to invoke methods on the MBE instance
     * @return
     */
    public String getMobileBackendIdentifier() {
        return mobileBackendIdentifier;
    }


    /**
     *
     * This is an unique identifier shared by all client applications created in your mobile backend. It is used as an authentication token when your mobile application makes REST calls into an Anonymous API, The secret key
     * either is the anonymous string generated by MCS MBE for anonymous login or the client secret created for OAUTH2
     * @return String
     */
    public String getAnonymousKey() {
        return mAnonymousKey;
    }

    /**
     *
     * This is an unique identifier shared by all client applications created in your mobile backend. It is used as an authentication token when your mobile application makes REST calls into an Anonymous API
     * @parameter mAnonymousKey  The anonymous key of the MBE instance. You getObjectsInfo this value through teh MCS portal ui. Note that this value may change in the course of the MCS lifecycle
     */
    public void setAnonymousKey(String mAnonymousKey) {
        this.mAnonymousKey = mAnonymousKey;
    }

    public String getMafRestConnectionName() {
        return mafRestConnectionName;
    }

    /**
     * Application vendor name is what the MAF application developer specifies in the map-application
     * configuration file
     * @return
     */
    public String getMafApplicationVendorName() {
        return mafApplicationVendorName;
    }

    public String getMafApplicationVersion() {
        return mafApplicationVersion;
    }

    /**
     * Each mobile backend has its own base URL. The base URL is used within the REST calls to address the MBE. The complete
     * URL for REST calls is composed out of this base URL and the URI with the resource name and query params. The SDK reads
     * the base URL from the provided MAF REST connection
     * @return base URL
     */
    public String getMobileBackendBaseURL() {
        RestServiceAdapter restServiceAdapter = Model.createRestServiceAdapter();
        restServiceAdapter.setConnectionName(this.getMafRestConnectionName());
        String mobileBackendBaseURL = null;
        try {
            mobileBackendBaseURL = restServiceAdapter.getConnectionEndPoint(this.getMafRestConnectionName());
        } catch (Exception e) {
            this.mLogger.logError("Failure in reading connection endpoint from REST connection: " +
                                        e.getMessage(), this.getClass().getSimpleName(), "getMobileBackend()");
            e.printStackTrace();
        }
        return mobileBackendBaseURL;
    }

    public MBELogger getLogger() {
        //create logger if it does not exist
        if (this.mLogger == null) {
            mLogger = new MBELogger(this);
        }

        return mLogger;
    }


    /**
      * Unique identifier for the device to allow e.g. analytics to distinguish different devices. UUID with Cordva
      * seems not to be reliable for identifying client devices. Due to security settings, this information  may not 
      * even be available. In addition, and on iOS, UUID values may vary for different application. Therefore this 
      * utility does not attempt to do the right thing and instead leaves it up to the application developer to access
      * whatever he/she feels is the right value to set to identify a device (e.g. one could use DeviceManager.getName 
      * in combination with a user profile preference value)
      *
      * @param mobileDeviceId the device ID that application developers want to be used for a specific device at runtime
      */
    public void setMobileDeviceId(String mobileDeviceId) {
        this.mobileDeviceId = mobileDeviceId;
    }

    /**
     * A random number or device ID that helps identifying requests coming from different devices. This value is not
     * created by the utility and must be provided by the MAF application developer
     * @return
     */
    public String getMobileDeviceId() {
        return mobileDeviceId;
    }

    /**
     * Android or IOS applicatons need to be registsered in MCS MBE for push notifications. Each configuration creates a 
     * key that needs to be provided by the application when registering to receive notifications from MCS. If the MAF 
     * aplication runs on an Android device then the registration for the Android Push notification is required, if it 
     * runs on iOS, the key defined for the iOS push needs to be provided
     * @return
     */
    public String getMobileBackendClientApplicationKey() {
        return mMobileBackendClientApplicationKey;
    }

    /**
     * Enable log messages to be written. Logging can be enabled at runtime for a specific mobile backend and
     * its configuration
     * @param isLoginEnabled
     */
    public void setLoggingEnabled(boolean isLoginEnabled) {
        this.mLoggingEnabled = isLoginEnabled;
    }

    public boolean isLoggingEnabled() {
        return mLoggingEnabled;
    }

    /**
     * *** INTERNAL METHOD ***
     * 
     * You can configure authentication on the Feature level in MAF. However, if usecase dictates to
     * perform manual authentication using the Authorization classes of this utility, then, by setting 
     * the user credentials in this class, the calls are authenticated by the RestClient. 
     * 
     * @param authorizationToken The base 64 encoded basic or OAuth token passed as the Authorization header parameter
     */
    public void setAuthorizationToken(String authorizationToken) {
        this.mAuthorizationToken = authorizationToken;
    }

    /**
     * The authentication token is used when the MAF MC UTIL does not leverage MAF feature security but manually
     * performs the user authentication. In this case, the authorizationProvider header needs to be added with each MCS
     * REST request.
     *
     * @return a base 64 encoded token that identifies the user by "Basic username:password" or "Bearer ...." token
     */
    public String getAuthorizationToken() {
        return mAuthorizationToken;
    }


    /**
     * *** INTERNAL METHOD ***
     *
     * Flag that indicates that MBE authentication is through the MAF MCS Utilty and not the MAF Feature declarative
     * authentication. The consequence is that the Authorization header and token needs to be added to the request by
     * MAF MCS Utility. For MAF Feature authentication, MAF would set the request authorizationProvider header.
     *
     * <b>Internal API</b>: applications must not call this method
     *
     * @param manualAuthentication true to indicate manual authentication to MCS
     */
    public void setManualAuthentication(boolean manualAuthentication) {
        this.manualAuthenticationFlag = manualAuthentication;
    }

    public boolean isManualAuthentication() {
        return manualAuthenticationFlag;
    }

    /**
     * *** INTERNAL METHOD ***
     * 
     * Used in combination with manualAuthenticationFlag to set the username of the authenticated user
     * @param authenticatedUsername
     */
    public void setAuthenticatedUsername(String authenticatedUsername) {
        this.mAuthenticatedUsername = authenticatedUsername;
    }

    /**
     * Returns the authenticated username or null if no user has been authenticated
     * @return
     */
    public String getAuthenticatedUsername() {

        //is user authenticated in MAF or manually by the MAF application. If session is not authenticated
        //in MAF for MCS, get the username from MAF
        if (!this.isManualAuthentication()) {
            //using EL to access the security context for simplicity
            SecurityContext securityContext = (SecurityContext) AdfmfJavaUtilities.getELValue("#{securityContext}");

            if (Boolean.parseBoolean(securityContext.isAuthenticated()) == true) {
                //user session is authenticated in the MAF feature
                mAuthenticatedUsername = securityContext.getUserName();
            } else {
                this.mLogger.logFine("MAF user session not authenticated.", this.getClass().getSimpleName(),
                                     "getAuthenticatedUsername");
                //user session is not authenticated
                mAuthenticatedUsername = "Undefined";
            }
        } else {
            //the authentication is through the MAF MCS utility and thus the username was set manually
            this.mLogger.logFine("Authentication is through MAF MCS Utility. Read username from MBE Config file.",
                                 this.getClass().getSimpleName(), "getAuthenticatedUsername");
        }
        this.mLogger.logFine("Returned username: " + mAuthenticatedUsername, this.getClass().getSimpleName(),
                             "getAuthenticatedUsername");
        return mAuthenticatedUsername;
    }

    /**
     * Allow developers to set a custom value for the Google package name. The information is sent to MCS when 
     * registering the MAF application for push notification sent by MCS. The default package name is set to the
     * MAF application "application Id" value (which commonly matched the Google package name). 
     * 
     * @param googlePackageName
     */
    public void setGooglePackageName(String googlePackageName) {
        this.mGooglePackageName = googlePackageName;
    }

    /**
     * Returns the Google package name used with push messages. The default package name is set to the
     * MAF application "application Id" value (which commonly matched the Google package name). 
     * 
     * @return String with the current package name
     */
    public String getGooglePackageName() {
        
        if(this.mGooglePackageName!=null && !this.mGooglePackageName.isEmpty()){
            return mGooglePackageName;
        }
        else{
         //return MAF application Id.
         return this.getMafApplicationId();             
        }
    }

    /**
     * The Apple bundle ID of an application matches the Id of the Apple certificate registered for push. The bundle ID 
     * is not equivalent to the MAF application ID and is specified in the iOS deployment profile used to deploy MAF to 
     * an iOS device or the IPA file
     * 
     * @param appleBundleId
     */
    public void setAppleBundleId(String appleBundleId) {
        this.mAppleBundleId = appleBundleId;
    }

    /**
     * The Apple bundle ID of an application matches the Id of the Apple certificate registered for push. The bundle ID 
     * is not equivalent to the MAF application ID and is specified in the iOS deployment profile used to deploy MAF to 
     * an iOS device or the IPA file
     * @return
     */
    public String getAppleBundleId() {
        return mAppleBundleId;
    }

    /**
     * Analytic events are collected by the MBE. However, events can only be flushed (sent in batches) to the server
     * if the MBE is configured to send events. Calling this method with an argument of true enables analytic messages,
     * whereas false (the default setting) disables analytic messages. In the latter case, events are dismissed and not
     * saved at all.
     *
     * @param enableAnalytics
     */
    public void setEnableAnalytics(boolean enableAnalytics) {
        this.mEnableAnalytics = enableAnalytics;
    }

    /**
     * Analytic events are collected by the MBE. However, events can only be flushed (sent in batches) to the server 
     * if the MBE is configured to send events. The Analytic object in this MCS UTIL library will not send any event
     * to the server if the returned boolean value is false
     * 
     * @return true/false
     */
    public boolean isEnableAnalytics() {
        return mEnableAnalytics;
    }

    /**
     * In Oracle MAF, the framework registers the mobile device with Google Cloud Messaging (GCM) or Apple Push Notification 
     * Service (APNS) to receive a device token. Upon successful registration the device token can be obtained in the push 
     * listener (a class that implements import oracle.adfmf.framework.event.EventListener) "onOpen" method. Once you have 
     * the token available, it needs to be passed to this configuration object so it can be added to the registration and 
     * de-registration of notifications in MCS
     * 
     * @param deviceToken
     */
    public void setDeviceToken(String deviceToken) {
        this.mDeviceToken = deviceToken;
    }

    /**
     * In Oracle MAF, the framework registers the mobile device with Google Cloud Messaging (GCM) or Apple Push Notification 
     * Service (APNS) to receive a device token. Upon successful registration the device token can be obtained in the push 
     * listener (a class that implements import oracle.adfmf.framework.event.EventListener) "onOpen" method. 
     * 
     * This method returns the token that is provided to the MAF MCS Utility for registering or de-registering clients to MCS
     */
    public String getDeviceToken() {
        return mDeviceToken;
    }

    /**
     * The argument to this object must be the user id of the authenticated user as it is known by MCS. You get this value from 
     * a call to the UserInfo proxy service. You get this information from calling the UserInfo proxy service
     * 
     * @param authenticatedUserMCSUserId
     */
    public void setAuthenticatedUserMCSUserId(String authenticatedUserMCSUserId) {
        String oldAuthenticatedUserMCSUserId = this.authenticatedUserMCSUserId;
        this.authenticatedUserMCSUserId = authenticatedUserMCSUserId;
        propertyChangeSupport.firePropertyChange("authenticatedUserMCSUserId", oldAuthenticatedUserMCSUserId,
                                                 authenticatedUserMCSUserId);
    }
    
    /**
     * If set, returns the user id of the authenticted user as known by MCS. This value is required for Storage operations on isolated
     * collection.
     * @return
     */
    public String getAuthenticatedUserMCSUserId() {
        return authenticatedUserMCSUserId;
    }


    /**
     * sets the MCS connection name for the OAUTH endpoint URL
     */
    public void setOauthEndpointConnectionName(String oauthEndpointConnectionName) {
        this.mOauthEndpointConnectionName = oauthEndpointConnectionName;
    }
    
    /**
     * gets the name of the MAF named connection that holds the OAUTH endpoint URL
     * @return
     */
    public String getOauthEndpointConnectionName() {
        return mOauthEndpointConnectionName;
    }


    /**
     * Sets the OAUTH client ID as the mobile backedn identifier. The client ID is also required to perform the
     * actual OAUH authentication
     * @param oauthClientId
     */
    public void setOauthClientId(String oauthClientId) {
        this.mobileBackendIdentifier = oauthClientId;
    }

    /**
     * returns the mobile backend identifier, which is the client ID used for OAUTH. The backend identifier is 
     * also used in client side logging statements
     * @return
     */
    public String getOauthClientId() {
        return this.getMobileBackendIdentifier();
    }

    /**
     * Sets the time stamp for when the OAUTH token expires, allowing applications to check the validness of the 
     * token before issuing a client request to the server 
     * @param oauthTokenExpiryTimestamp
     */
    public void setOauthTokenExpiryTimeInMilliSeconds(long oauthTokenExpiryTimestamp) {
        this.oauthTokenExpiryTimeInMilliSeconds = oauthTokenExpiryTimestamp;
    }

    /**
     * Gets the time stamp for when the OAUTH token expires, allowing applications to check the validness of the 
     * token before issuing a client request to the server 
     * @return the time in ms when the OAUTH token expires
     */
    public long getOauthTokenExpiryTimeInMilliSeconds() {
        return oauthTokenExpiryTimeInMilliSeconds;
    }


    /**
     * An Identity Domain is a logical namespace for users and groups. Identity domains are used to identify and 
     * distinguish different sets of users. An identity domain can represent users from a particular company, or 
     * from a department within that company. In a cloud environment, identity domains can distinguish system users 
     * from tenant users.
     * 
     * @param oauthIdentityDomain
     */
    public void setOauthIdentityDomain(String oauthIdentityDomain) {
        this.mOauthIdentityDomain = oauthIdentityDomain;
    }

    /**
     * An Identity Domain is a logical namespace for users and groups. Identity domains are used to identify and 
     * distinguish different sets of users. An identity domain can represent users from a particular company, or 
     * from a department within that company. In a cloud environment, identity domains can distinguish system users 
     * from tenant users.
     * 
     * @return the identity domain string configured for the OAUTH authenticated MBE
     */
    public String getOauthIdentityDomain() {
        return mOauthIdentityDomain;
    }

}
