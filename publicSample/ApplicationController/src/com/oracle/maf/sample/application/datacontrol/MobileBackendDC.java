package com.oracle.maf.sample.application.datacontrol;

import com.oracle.maf.sample.application.push.PushConstants;
import com.oracle.maf.sample.application.utils.DeviceUtil;
import com.oracle.maf.sample.mcs.apis.analytics.Analytics;
import com.oracle.maf.sample.mcs.apis.analytics.Event;
import com.oracle.maf.sample.mcs.apis.custom.CustomAPI;
import com.oracle.maf.sample.mcs.apis.notifications.Notifications;
import com.oracle.maf.sample.mcs.apis.policy.AppPolicies;
import com.oracle.maf.sample.mcs.apis.policy.Policy;
import com.oracle.maf.sample.mcs.apis.storage.Storage;
import com.oracle.maf.sample.mcs.apis.storage.StorageCollection;
import com.oracle.maf.sample.mcs.apis.storage.StorageInformation;
import com.oracle.maf.sample.mcs.apis.storage.StorageObject;
import com.oracle.maf.sample.mcs.apis.userinfo.User;
import com.oracle.maf.sample.mcs.apis.userinfo.UserInfo;
import com.oracle.maf.sample.mcs.shared.authorization.auth.Authorization;
import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.log.MBELogger;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSResponse;
import com.oracle.maf.sample.mcs.shared.mbe.MBE;
import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;
import com.oracle.maf.sample.mcs.shared.mbe.MBEManager;
import com.oracle.maf.sample.mcs.shared.mbe.config.BasicAuthMBEConfiguration;
import com.oracle.maf.sample.mcs.shared.mbe.config.OauthMBEConfiguration;
import com.oracle.maf.sample.mcs.shared.mbe.error.OracleMobileError;
import com.oracle.maf.sample.mcs.shared.mbe.error.OracleMobileErrorHelper;
import com.oracle.maf.sample.mcs.shared.utils.MapUtils;

import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import oracle.adf.model.datacontrols.device.DeviceManagerFactory;

import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;
import oracle.adfmf.json.JSONException;


/**
 * The MAF MCS Utility exposes an API for Oracle MAF applications to access Oracle Mobile Cloud Service. The utility is
 * architectured such that it allows you to work against multiple mobile backends in Oracle MCS in parallel. For a full
 * discussion of MAF MCS Utility see the "Mobile Application Framework MAF MCS Utility Developer Guide" whitepaper on OTN
 * <p>
 * Technically a data control in MAF may expose the functionality of a single MCS MobileBackend (MBE) or the functionality
 * of multiple MBEs. Logically it makes sense to do a 1:1 mapping to keep code sources clear and clean. This sample routes
 * all communications to the MCS MBE instance through the data control to ensure a single consistent point of interaction.
 * Technically - through the MBEManager - however it is possible to access a created MAF MCS Utility MBE instance directly
 * from a managed bean (or any other Java object in the same classloader (Feature)). The MAF MCS Utility is not designed for
 * use with data controls only. However, data controls helps organizing and structuring MAF application code when accessing
 * Oracle MCS remote MBEs.
 * <p>
 * Important: The first decision you make is where to configure MAF MCS Utility (on the application controller or on the view
 * controller project). If you configure this on the application controller project - like in this sample - you need to return
 * the query results to generic Java objects when the result needs to be returned to a managed bean. The ViewController project,
 * and thus managed bean therein, doesn't know of Java classes defined in the application project. Configuring MAF MCS Utilities
 * on the view layer project solves this "problem" but then instantiates one MBE Manager per MAF Feature, reducing the ability to
 * share fetched content throughout an application.
 * <p>
 * This sample has the MAF MCS Utility access configured on the appllication controller level and thus can share queried data
 * from MCS (even for multiple mobile backend access) across application features through the MBE Manager
 * <p>
 * --- ASYNCHRONOUS API CALLS ---
 * <p>
 * <pre>
 * By design, all MAF MCS Utility APIs, including cusom API, are synchronous. To execute APIs asynchrnous, you need to wrap them
 * in a Runnable object. To invoke a method asynchronosly, this is what you do
 *
 * public void doSomethingAsynch(){
 *
 *  Runnable mcsJob = new Runnable(){
 *
 *  //prepare the call
 *  public void run(){
 *        //1. access MBE instance --> ServiceProxy --> invoke method
 *        //2. handle response (e.g. update collections in MAF or save downloaded content)
 *
 *        //if data has been changed in MAF, refresh UI e.g. for list
 *        providerChangeSupport.fireProviderRefresh("listName");         //refresh UI in main thread
 *        AdfmfJavaUtilities.flushDataChangeEvent();
 *    }
 * }
 *
 *   //in your method, execute the code above using a separate thread
 *   ExecutorService executor = Executors.newFixedThreadPool(2);
 *   executor.execute(mcsJob);
 *   executor.shutdown();
 *}
 *</pre>
 * <p>
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class MobileBackendDC {

    //hold a reference to the MBE instance. Note that this handle needs to be released when removing the MBE object
    //from the MBEManager map. Otherwise the object would be pinned and not released from memory. Instead of holding
    //a reference to the MBE in the data control - and for sure better practice when working with multiple MBEs in a
    //MAF application you could call MBEManager.manager.getMobileBackend(name) to obtain a reference to a previously
    //created MBE
    private MBE mobileBackend = null;

    //using a MBE logger allows custom MAF applications to write log messages that then print in the context
    //of the specific MBE logs. In addition, because logging can be enabled / disabled for a mobile backend
    //through the MBEConfiguration, application logging can also be filtered for a specific MBE (in case there
    //is more than one used)
    private MBELogger logger = null;

    private boolean anonymousLogin = false;
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public MobileBackendDC() {
        super();
    }

    /*
     * *******************************************
     * ANALYTICS APIs
     * *******************************************
    */

    //track whether analytic event is queued
    private boolean isAnalyticEventOngoing = false;

    /**
     * Adds a custom event to the analytics event queue. Upon the first call to this method, a new analytic session will
     * be started. Note that you can add as many custom events as you like to the analytic event queue before posting it
     * to the server in a batch call.
     *
     * The HashMap can have an arbitrary  number of key/value pairs. Its not restricted to the key names and values used
     * in this sample. Custom analytic events describe an action and  thus may have many key/value pairs. The key names
     * are also free for you to choose. Thinks "business process logging" when defining custom events. Like with logging,
     * you want to keep the number of events reasonable and not too fine grain
     *
     * This method can be called from managed beans (in response to user view interaction) or the method activity in a
     * task flow (for declarative event gathering). This sample however uses managed beans only.
     * @param customEventName for Analytic custom events, the name can be freely chosen to match the mobile business case
     * @param properties HashMap with custom key/value pairs
     */
    public void addCustomAnalyticEvent(String customEventName,
                                       HashMap<String, String> properties) {
        //log properties content
        logger.logFine("Properties passed to method:  " + MapUtils.dumpStringProperties(properties),
                       this.getClass().getSimpleName(), "addCustomAnalyticEvent");
        //reset UI information and error message

        this.resetMessages();

        //try and compute a total
        try {
            Double singlePrice = Double.parseDouble(properties.get("UnitPrice"));
            Double volume = Double.parseDouble(properties.get("Quantity"));

            Double total = singlePrice * volume;
            String totalString = total.toString();

            properties.put("Total", totalString);

        }
        //we need to worry about NumberFormatException. However, this sample is supposed to work no matter who the
        //user is and thus - to preare for demonstrations that put non-numeric values into the fields - we do catch
        //and handle anything on the way.
        catch (Exception e) {
            //the MBE specific logger can be used to print messages that are specific for the MBE and not e.g. for the MAF application. As
            // developer you should use a MAF application specific logger for your application related logging and the loggers defined in
            //the MBE instance for MBE specific logging. This way you can enable/disable logging at runtime through the MBE settings.
            logger.logError("Product Id " + properties.get("ProductId") +
                            " provided quantity or unit price as a non numeric value", this.getClass().getSimpleName(),
                            "addCustomAnalyticEvent");
            properties.put("Total", "Undefined");
        }

        //a mobile backend is assumed to be available in thsi sample. In custom applications you may
        //want to check if you cannot be sure that the MBE instance you want to access exists
        Analytics analyticsProxy = this.mobileBackend.getServiceProxyAnalytics();

        if (isAnalyticEventOngoing == false) {
            //start an analytic session. This adds a system event and a context event to the event queue. Note that sessions
            //cannot be nested and thus need to be sequentially. There is no strict definition in MCS as of what a session is
            //and developers may decide that an analytic session is equal to the application session. However, sessions are
            //published to the server as a whole, which means they are collected in memory until the content can be flushed
            //when the session ends.
            analyticsProxy.startSession();
            isAnalyticEventOngoing = true;
        }

        //create event object. The event name is mandatory and is set to "mafmcsutility-food", "mafmcsutility-electronics" or "mafmcsutility-apparel"
        //in this sample. In your custom MAF applications you use event names to group messages by a specific mobile application or business context.
        //E.g. /you may device an event name as "online purchase" and then use the event properties to define details like ("productId"->"1234",
        //"productName"-> "Stainless Steel Waterbottle", "singlePrice"->"2.45", "quantityOrdered"->"2", "discount"->"5%"). Other event names could then
        //be "payment" with properties details as ("paymentType"-> "credit", "cardHolder"->"J.B. Good", "cardType"->"Amex","Card Number"->"123456789",
        //"valid"->"12-2017")
        //
        //The event date is added by the system if provided as null. If you  don't need to pass custom properties (just a ping to the
        //analytic event engine) then you can add an empty Event

        Event customEvent = new Event(customEventName, null, properties);
        analyticsProxy.addCustomEvent(customEvent);

        this.logDcInfoForUserInterfaceDisplay("Added product with Id " + properties.get("ProductId") +
                                              " to outgoing analytic event queue");

    }

    /**
     * Flush the event queue to the server. This also will ens the analytic session for the recording. The next queueing of
     * an analytic event starts a new session. Note that its the MAF application developer that - when using MAF MCS Utils -
     * deciced how long a session lasts, when it starts and when it ends.
     *
     * This method can be called from managed beans (in response to user view interaction) or the method activity in a
     *  task flow (for declarative event gathering). This sample however uses managed beans only.
     */
    public void postEventsToServer() {
        this.resetMessages();
        Analytics analyticsProxy = this.mobileBackend.getServiceProxyAnalytics();
        try {
            analyticsProxy.endSession();
            this.logDcInfoForUserInterfaceDisplay("Events processed.");
        } catch (ServiceProxyException e) {
            logDcErrorForUserInterfaceDisplay("Failed to send analytic events to MCS. Please check the log files");
        }
        //even if sending of the event failed, set the ongoing event flag back. If events
        //could not be sent to the server due to a message format error, then the message
        //is dismissed and lst (see the logs). If the events failed to send to the server
        //due to a network error then they are saved in SQLite for later sending.
        isAnalyticEventOngoing = false;
    }
    
    /*
     * *******************************************
     * CUSTOM APIs
     * *******************************************
    */
    
    /*
     * The custom API allows you to call any REST API on the MCS server. The MAF MCS Utility will automatically
     * add the mobile backend Id and the required authorization header to the request. The returned response 
     * object contains the RAW JSON payload for you - the MAF developer - to further process (e.g. to write into
     * a Java entity)
     */
    
    
    /**
     * This method demonstrates how to use the custom API Service Proxy in MAF MCS Utility. By default, custom APIs are 
     * processed synchronously. You can use a Java Thread (see sample on top of this file) to invoke it asynchronously.
     * @param mafConnection The name of the MAF REST connection containing the MCS endpoint information for a MBE
     * @param requestURI    The complete URI pointing to the custom MCS API to invoke
     * @param httpMethod    GET, PUT, POST, DELETE or HEAD as a string
     * @param payload       For POST and PU T requests, the String payload expected on the server side custom API
     * @param httpHeaders   Any http header that needs to be passed for the custom API. MAF MCS Utility automatically adds Authorization, Oracle-Mobile-Backend-Id and accept: application/json by default.
     * @return String response of the custom API. Note that if the custom API returns binary data, you need to invoke sendReceiveBytes(request) instead of the Service Proxy method used in this method
     */
    public String invokeCustomMcsAPI(String mafConnection, String requestURI, String httpMethod, String payload, HashMap httpHeaders){
        
        this.resetMessages();
        
        //Showcase asynchronous invocation of MAF MCS Utility API calls. Note that the same construct used here can be used with 
        //any API exposed on the MBE instances. If the business case requires synchronous invocation, remove the Runnable part of 
        //this code
        Runnable mcsJob = new Runnable(){
            
        public void run(){ 
          try {
                  
                  //get access to the service proxy for custom API calls
                  CustomAPI customApi = mobileBackend.getServiceProxyCustomApi();            
                  
                  //MAF MCS Utility provides to configuration objects, MCSRequest and MCSResponse, that guide you
                  //through setting up the MCS request and to read the MCS response. First, a MCSRequest object is
                  //created
                  MCSRequest request = new MCSRequest(mobileBackend.getMbeConfiguration());
                  
                  //the MSF REST connection as defined in Application Resources --> Connections --> REST. In this 
                  //public sample the REST connection name is "MCSUTILRESTCONN"
                  request.setConnectionName(mafConnection);            
                  
                  //save the custom MCS API URI
                  request.setRequestURI(requestURI);            
                  
                  //set the request method
                  request.setHttpMethod(httpMethod);            
                  //ensure null is translated to "" as we need a string
                  request.setPayload(payload==null?"":payload);
                  request.setRetryLimit(0);
                  
                  //define the headers that need to be send with the custom API request
                  HashMap<String,String> headers = new HashMap<String,String>();
                  
                  if(httpHeaders!=null){
                      //this HashMap should contain all the header information specific to the custom API call.
                      //If the custom API returns anything different from application/json, then this map needs
                      //to contain the Accept : <mime type> information e.g. Accept  image/png for images. If 
                      //the POST or PUT are used, the Content-Type header needs to be set accordingly
                      headers.putAll(httpHeaders);
                  }
                              
                  request.setHttpHeaders(headers);  
                  
                  //this call returns a String response. For binary responses (binary[]) you use
                  //customApi.sendReceiveBytes(request) instead.
                  MCSResponse response = customApi.sendForStringResponse(request);
                  
                  String jsonResponse = (String) response.getMessage(); 
              
                  setCustomAPIResponse(jsonResponse);
                  
              } catch (ServiceProxyException e) {
                  if (e.isApplicationError()) {
                      //if this is a well formatted Oracle Mobile error, we can display a user friendly error message
                      try {
                          OracleMobileError mobileError = OracleMobileErrorHelper.getMobileErrorObject(e.getMessage());
                          //print short description of error
                          logDcErrorForUserInterfaceDisplay(mobileError.getTitle());
                      } catch (JSONException f) {
                          logDcErrorForUserInterfaceDisplay(e.getMessage());
                      }
                  } else {
                      logDcErrorForUserInterfaceDisplay(e.getMessage());
                  }
              }
              //ensure main thread is synchronized with result
              AdfmfJavaUtilities.flushDataChangeEvent();
          }
        };  
        
        
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(mcsJob);
        executor.shutdown();
        
        //in case of an error you don't receive a response string
        return null;
    }
    
    private String customAPIResponse = "";


    public void setCustomAPIResponse(String customAPIResponse) {
        String oldCustomAPIResponse = this.customAPIResponse;
        this.customAPIResponse = customAPIResponse;
        propertyChangeSupport.firePropertyChange("customAPIResponse", oldCustomAPIResponse, customAPIResponse);
    }

    public String getCustomAPIResponse() {
        return customAPIResponse;
    }


    /*
  * *******************************************
  * Device Registration
  * *******************************************
  */

    /*
     * Device registration requires pre-requisite setup. You need a Google and Apple Developer account to obtain
     * a sender Id (Google) and a push enabled Apple p12 certificate (with no password) for Apple.
     *
     * For iOS deployment you need to set the application bundle Id in the application properties to the value in
     * the p12 certificate. Application --> Application Properties --> Deployment --> iOS (MAF for iOS) --> Edit
     * iOS Options --> Application Bundle Id. The p12 certificate then is used to sign the deployed application to iOS
     *
     * For Google the application package name and application name needs to be configured in the application preferences
     * as found in maf-application.xml --> Application --> Id
     *
     * Configure the sample application preferences and provide:
     *
     * - the Google sender Id
     * - Apple application bundle id
     * - application package name and application name
     * - Enable application preferences to receive push information
     * - MCS MBE "Client Application IDs for Android and for iOS
     *
     * Then restart the application.
     */

    /**
     * Register device with MCS. This registration requires the full package name and application name (also for Android),
     * the application bundle id for Apple as well a enabling the sample application properties
     */
    public void registerDeviceWithMCS() {

        this.resetMessages();
        
        //the application preferences allow to enable/disable device registration to Apple and Google. However, 
        //the registration must be enabled when the application starts for the MAF framework to obtain the push
        //provider token
        boolean pushEnabledForApplication = (Boolean) AdfmfJavaUtilities.getELValue(PushConstants.PUSH_ENABLED);

        if (pushEnabledForApplication == true) {

            //get the _token retrieved from Google or Apple
            String _token = (String) AdfmfJavaUtilities.getELValue(PushConstants.PUSH_DeviceTOKEN);
            
            //If we have a _token, set it to the MBE configuration for the Notifications service proxy to 
            //access when registering and de-registering the application to MCS
            if(_token != null && !_token.isEmpty()){
                this.mobileBackend.getMbeConfiguration().setDeviceToken(_token);
            }
            else{
                //no _token -- cancel request
                this.logDcErrorForUserInterfaceDisplay("No device token found. MCS device registration cancelled.");
                return;
            }

            //get iOS
            if (DeviceUtil.isIOS()) {
                String appleBundleId = (String) AdfmfJavaUtilities.getELValue(PushConstants.APPLE_BUNDLE_ID);
                this.mobileBackend.getMbeConfiguration().setAppleBundleId(appleBundleId);
            }
            //else Android
            else {
                String packageName = (String) AdfmfJavaUtilities.getELValue(PushConstants.GOOGLE_PACKAGE_NAME);
                this.mobileBackend.getMbeConfiguration().setGooglePackageName(packageName);
            }

            //Showcase asynchronous invocation of MAF MCS Utility API calls. Note that the same construct used here can be used with 
            //any API exposed on the MBE instances            
            Runnable mcsJob = new Runnable(){

              public void run(){                                               
                 try {
                     Notifications notification = mobileBackend.getServiceProxyNotifications();

                     //registration with MCS returns a JSON String with the registration information
                     //Example response:
                     //
                     //{
                     //   "id": "8a8a1eff-83c3-41b4-bea8-33357962d9a7",
                     //   "notificationToken": "c7645c692e143855054b40c3621d4c262ce1f97f0fd62a844bef34eab991758b",
                     //   "mobileClient": {
                     //   "id": "com.company.mycert.FiFTechnician",
                     //   "version": "1.0",
                     //   "platform": "IOS"
                     //   },
                     //     "modifiedOn": "2014-05-05T12:09:33.281Z"
                     //  }
                     String registrationInfo = notification.registerDeviceToMCS();

                     //set value in memory for display on DeviceRegistration page
                     AdfmfJavaUtilities.setELValue(PushConstants.MCS_REGISTRATION_STRING, registrationInfo);

                   } catch (ServiceProxyException e) {
                     if (e.isApplicationError()) {
                         //if this is a well formatted Oracle Mobile error, we can display a user friendly error message
                         try {
                             OracleMobileError mobileError = OracleMobileErrorHelper.getMobileErrorObject(e.getMessage());
                             //print short description of error
                             logDcErrorForUserInterfaceDisplay(mobileError.getTitle());
                         } catch (JSONException f) {
                             logDcErrorForUserInterfaceDisplay(e.getMessage());
                         }
                     } else {
                         logDcErrorForUserInterfaceDisplay(e.getMessage());
                     }
                   }
                 
                  //ensure main thread is synchronized with result
                  AdfmfJavaUtilities.flushDataChangeEvent();
              }
           };
          
            ExecutorService executor = Executors.newFixedThreadPool(2);
            executor.execute(mcsJob);
            executor.shutdown();
           
        }
        //push is not enabled for this application
        else {
            this.mobileBackend.getMbeConfiguration().getLogger().logFine("Sample has push disabled in preferences. No MCS device registration performed.",
                                                                         this.getClass().getSimpleName(),
                                                                         "registerDeviceWithMCS");
        }
    }

    /**
     * deRegister device from MCS. The de-registration is called after registration so that there is no need to verify
     * if the push settings are available in the MAF MCS Utility configuration
     */
    public void deRegisterDeviceFromMCS() {
        
        //Showcase asynchronous invocation of MAF MCS Utility API calls. Note that the same construct used here can be used with 
        //any API exposed on the MBE instances            
        
        this.resetMessages();
        
        Runnable mcsJob = new Runnable(){

          public void run(){  
              try {

                  Notifications notification = mobileBackend.getServiceProxyNotifications();
                  
                  notification.deregisterDeviceFromMCS();
                  
                  //unlike when registering a device with MCS, there is no JSON payload returned from the request. Instead
                  //a simple text message is returned indicating the success of the operation. So we do the same here by 
                  //adding oru own message
                  logDcInfoForUserInterfaceDisplay("Application has been deregistered from MCS");
                  AdfmfJavaUtilities.setELValue(PushConstants.MCS_REGISTRATION_STRING, "");

              } catch (ServiceProxyException e) {
                  if (e.isApplicationError()) {
                      //if this is a well formatted Oracle Mobile error, we can display a user friendly error message
                      try {
                          OracleMobileError mobileError = OracleMobileErrorHelper.getMobileErrorObject(e.getMessage());
                          //print short description of error
                          logDcErrorForUserInterfaceDisplay(mobileError.getTitle());
                      } catch (JSONException f) {
                          logDcErrorForUserInterfaceDisplay(e.getMessage());
                      }
                  } else {
                      logDcErrorForUserInterfaceDisplay(e.getMessage());
                  }
              }
              //ensure main thread is synchronized with result
              AdfmfJavaUtilities.flushDataChangeEvent();
          }
        };
        
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(mcsJob);
        executor.shutdown();
    }


 /*
  * *******************************************
  * STORAGE APIs
  * *******************************************
  */
    /*
     * Storage is the most complex API in Oracle MCS and there is something to know about it to better understand 
     * how how to use the Storage proxy service in MAF MCS Utility. 
     * 
     * In Oracle MCS, there are 2 types of collections: shared and isolated. In a shared collection, all objects are
     * saved independent of which user saved them. As a result, object Ids must be unique in this space, including ids
     * that an applications can set itself using the POST command. In an isolated scope, user objects are saved in the
     * same collection but in "virtual" stripes per user. Objects thus need to be unique in the context of a user. To
     * be able to access objects in an isolated scope, the object must be identified by an object Id and the  userId 
     * (not the username). If no user id is passed as a query parameter with the request then the Id of the authenticated
     * user is used in MCS. 
     * 
     * To simplify the access of collection objects that are owned (created) by other users (assuming the authenticated 
     * user has read_all or write_read_all access) the StorageCollection object in MAF MCS Utility has a property called
     * objectOwnerUserID. If this property is not set, or set to null, then all access to isolated collections in for the
     * authenticated user. If the objectOwnerUserID is set, which can can be done upon creating the StorageCollection 
     * instance in the Storage object, or at runtime using the objectOwnerUserID() method, then all object operations in
     * the isolated collection (create, update, delete, downloads) are for this user context. 
     * 
     * An alternative option to create/delete/update objects owned by other users is to call the StorageCollections 
     * <method name>WithURI methods that accept a canonical link (URI) as argument. The canonical link can be accessed
     * from the StorageObject class that is returned in a list for queries performed through the StorageCollection class.
     * 
     * Note that even without setting the objectOwnerUserID in a StorageCollection, isolated collections can be queried
     * for all user objects if the authenticated user has read_all permission. No requirements like this exist for shared
     * collections. For shared collections all users have access to all objects and the StorageCollection object in MAF
     * MCS Utility knows how to distinuish the shared from the isolated collection usecase. 
     */

    /*
     * To better diagnose and report errors to the user, this data control saves the current Storage collection in a member
     * variable. This allows to expose collection detail queries on the data control. If you do the same in your application,
     * ensure the StorageCollection is queried before querying detail collections.
     */

    //current selected storage collection
    StorageCollection currentStorageCollection = null;

    //flag indicating change of the current storage collection. This leads to a refresh of the collection objects
    //next time the collection objects are requested by a page
    private boolean currentStorageCollectionChangedFlag = true;

    private boolean showObjectsOwnedByOtherUsers = false;

    //set to share missing access violations with the CollectionDetails view AMX page so it can hide all
    //create object controls
    private boolean internalUseOnly_missingCollectionPrivileges = false;

    public void setInternalUseOnly_missingCollectionPrivileges(boolean missingCollectionAccessPrivileges) {
        boolean oldMissingCollectionAccessPrivileges = this.internalUseOnly_missingCollectionPrivileges;
        this.internalUseOnly_missingCollectionPrivileges = missingCollectionAccessPrivileges;
        propertyChangeSupport.firePropertyChange("missingCollectionAccessPrivileges",
                                                 oldMissingCollectionAccessPrivileges,
                                                 missingCollectionAccessPrivileges);
    }

    public boolean isInternalUseOnly_missingCollectionPrivileges() {
        return internalUseOnly_missingCollectionPrivileges;
    }


    //List of storage objects queried from MCS or held in-memory
    List<StorageObject> storageObjectList = new ArrayList<StorageObject>(); ;

    /*
     * methods listed in their call sequence within the sample
     *
     * 1. A list of collections in MCS MBE is called and displayed
     * 2. Fetch information for selected collection and build StorageCollection proxy object
     * 3. Fetch information about content (objects) in the collection
     * 4. Update collection object (binary)
     *
     */

    /**
     * Reads the collection information for the Storage API associated with the MBE the application is authenticated to. This
     * method is invoked from Storage.amx through a direct data control binding.
     *
     * @return StorageInformation object that contains information about the collection as well as a list of collections
     */
    public StorageInformation getStorageInformation() {

        this.resetMessages();

        Storage storageProxy = this.mobileBackend.getServiceProxyStorage();
        StorageInformation storageInformation = null;
        try {
            //query 20 collection items starting from index 0 if available
            storageInformation = storageProxy.queryStorageInformation(0, 20);

        } catch (ServiceProxyException e) {
            if (e.isApplicationError()) {
                //if this is a well formatted Oracle Mobile error, we can display a user friendly error message
                try {
                    OracleMobileError mobileError = OracleMobileErrorHelper.getMobileErrorObject(e.getMessage());
                    //print short description of error
                    logDcErrorForUserInterfaceDisplay(mobileError.getTitle());
                } catch (JSONException f) {
                    logDcErrorForUserInterfaceDisplay(e.getMessage());
                }
            } else {
                logDcErrorForUserInterfaceDisplay(e.getMessage());
            }
        }

        return storageInformation;

    }

    /**
     * Reads the collection from MCS for a MBE based on the collection ID. The StorageColletion is a proxy object that
     * gives programs access to the Storage objects in the collection. This method is invoked from the StorageOptions
     * AMX page through a managed bean access to the data control. The managed bean is StorageOptionsBacking and has all
     * the access code in it.
     *
     * @param collectionId a valid collection Id (note that this is the ID, not the name)
     * @return StorageCollection, proxy that provides access to information about objects in a collection as well as methods to update a collection
     */
    public StorageCollection findCollectionById(String collectionId) {

        this.resetMessages();

        //collection Id is required
        if ((collectionId != null) && (!collectionId.isEmpty())) {
            //use of the typed interface to access a service proxy
            Storage storageProxy = this.mobileBackend.getServiceProxyStorage();
            try {
                
                //this API obtains a StorageCollection object configured for the authenticated user. This means that 
                //though the user can query all collection objects (also those owned by a different user in case of 
                //isolated collections (assuming the user has READ_ALL access), all object create, update and delete
                //operations are invoked for this user. To Create, update and delete objects of other users in isolated
                //collections, you call storageProxy.querySingleCollectionForUserId(collectionId, {id of other user - 
                //not username }) or call setObjectOwnerUserID({id of other user - not username }); on the collection 
                //instance at runtime. 
                //
                //you get the user ID of the authenticated user in a call to mobileBackend.getMbeConfiguration().getAuthenticatedUserMCSUserId()
                //Also, the StorageCollection has a method to reset its object owner user id to the id of the authenticated user.
                //NOTE: if you authenticate through MAF then you need to manually set the authenticatedUserMCSUserId in the MBEConfiguration object.
                //For this, you invoke the UserInfo servuce proxy and obtain the User object for the current authenticated user. From the object you
                //then get the user Id to set in the MBE Configuration. 
                currentStorageCollection = storageProxy.querySingleCollection(collectionId);
                
                //indicate successful change of stoare selection
                currentStorageCollectionChangedFlag = true;
                return currentStorageCollection;
                
            } catch (ServiceProxyException e) {
                if (e.isApplicationError()) {
                    //if this is a well formatted Oracle Mobile error, we can display a user friendly error message
                    try {
                        OracleMobileError mobileError = OracleMobileErrorHelper.getMobileErrorObject(e.getMessage());
                        //print short description of error
                        logDcErrorForUserInterfaceDisplay(mobileError.getTitle());
                    } catch (JSONException f) {
                        logDcErrorForUserInterfaceDisplay(e.getMessage());
                    }
                } else {
                    logDcErrorForUserInterfaceDisplay(e.getMessage());
                }
            }
        } else {
            logDcErrorForUserInterfaceDisplay("collectionId value cannot be null or empty");
        }

        return currentStorageCollection;
    }


    /**
     * This method depends on the availability of a current StoragObject queried from MCS. This sample ensure this object
     * is queried when accessing the CollectionDetails AMX page. The reason for not using the nested collection directly
     * from the StorageObject exposed in the data control is to fail gracefully and show permission failures as inline page
     * messages. Ensure that if a collection is isolated, you have write-read-all and read-all
     *
     * @params alwaysRequery If set to true, the collection objects are queried from MCS. If set to false, previously
     *         fetched objects are returned
     * @return list of storage objects queried for the selected collection.
     */
    public List<StorageObject> fetchCurrentCollectionStorageObjects(boolean alwaysRequery) {
        this.resetMessages();
        //query collection objects if list is empty or if alwaysRequery is set to true
        if (currentStorageCollection != null) {

            //set missing permission back to none
            this.setInternalUseOnly_missingCollectionPrivileges(false);
            //re-query
            if (alwaysRequery == true || currentStorageCollectionChangedFlag == true) {

                try {
                    //the objects query allows you to paginate through objects in MCS. For this sample, we only fetch the first 25
                    //objects. If the collection is isolated we query it for all users. Ensure the authenticated user has read-all
                    //or write_read_all privileges. If the collection is isolated then by default it will query the objects of the 
                    //authenticated user. You can change this with a switch on the collection page in the demo after which you see
                    //objects owned by all users.
                    storageObjectList = currentStorageCollection.queryStorageObjectsByRange(0, 25, this.showObjectsOwnedByOtherUsers, null);

                    //indicate storage objects to be quereied for changed current storage collection
                    currentStorageCollectionChangedFlag = false;
                } catch (ServiceProxyException e) {
                    //chances are the user privileges are not sufficient for the call (e.g. if the collection is isolated and the
                    //user does not have read_all or write_read_all permissions). In this case, instead of having MAF show an alert
                    //the error message is printed to the page (a hopefully much better user exeperience)
                    if (e.isApplicationError()) {
                        //if this is a well formatted Oracle Mobile error, we can display a user friendly error message
                        try {
                            OracleMobileError mobileError =
                                OracleMobileErrorHelper.getMobileErrorObject(e.getMessage());
                            //print short description of error
                            logDcErrorForUserInterfaceDisplay(mobileError.getDetail());
                        } catch (JSONException f) {
                            logDcErrorForUserInterfaceDisplay(e.getMessage());
                        }

                        //since this is an application error, we don't have a new storageObjectList to populate.
                        //Thus setting list back to empty list.
                        storageObjectList = new ArrayList<StorageObject>();

                        //set permission information as chances are high the error is due to missing privileges
                        this.setInternalUseOnly_missingCollectionPrivileges(true);
                    } else {
                        logDcErrorForUserInterfaceDisplay(e.getMessage());
                        //request fails, so return empty ist
                        storageObjectList = new ArrayList<StorageObject>();
                    }
                }
            }
            //collection has not been changed and alwaysRequery is false too
            else {
                //do nothing
            }
        } else {
            logDcErrorForUserInterfaceDisplay("No Storage collection selected");
        }
        return storageObjectList;
    }



    /**
     * Updates the collection object identified by the canonical link with the new content. For objects in the 
     * isolated colection, the canonical link, which you obtain from the StorageObject instance for the object
     * contains the ?userId query parameter to indicate the user that owns the object. 
     * 
     * @param canonicalURI The id of the object to update
     * @param displayName name of the object as it should be shown on the screen
     * @param contentType a valid MIME type
     * @param byteContent byte[] of the uploaded document or image
     * @return JSON object string with the attributes and the canonical link of the updated object
     */
    public String updateCollectionObject(String canonicalURI, String displayName, String contentType, byte[] byteContent) {

        this.resetMessages();

        if (currentStorageCollection != null) {

            try {

                //call MAF MCS UTility to update the storage object. The storage update will be updated unconditionally as the
                //HashMap for the eTag parameters is passed in as null. So no optimistic locking!
                StorageObject updateStorageObject = currentStorageCollection.updateCollectionObjectWithURI(canonicalURI, displayName, contentType, byteContent, null);
                String json = updateStorageObject.toJSONString();
                this.logDcInfoForUserInterfaceDisplay("Update successful.");
                return json;
            } catch (ServiceProxyException e) {

                //check if this is an MCS error message or a REST Service transport layer message
                if (e.isApplicationError()) {
                    //if this is a well formatted Oracle Mobile error, we can display a user friendly error message
                    try {
                        OracleMobileError mobileError = OracleMobileErrorHelper.getMobileErrorObject(e.getMessage());
                        //print short description of error
                        logDcErrorForUserInterfaceDisplay(mobileError.getDetail());
                    } catch (JSONException f) {
                        logDcErrorForUserInterfaceDisplay(e.getMessage());
                        logDcErrorForUserInterfaceDisplay(e.getMessage());
                    }
                }
                else{
                    //system error e.g. NPE
                    
                }
            }
        } else {
            logDcErrorForUserInterfaceDisplay("No Storage collection selected");
        }
        return null;
    }


    /**
     * Creates a new Storage object in Oracle MCS using the POST method (which means the object Id is generated by
     * MCS and not defined by the application.
     *
     * @param displayName name of the object as it should be shown on the screen
     * @param contentType a valid MIME type
     * @param byteContent byte[] of the uploaded document or image
     * @return JSON object string with the attributes and the canonical link of the object
     */
    public String createCollectionObject(String displayName, String contentType, byte[] byteContent) {

        this.resetMessages();

        if (currentStorageCollection != null) {

            try {

                //The StorageCollection object is a proxy class that creates, updates, deletes - and also uploads 
                //and downloads object content in the context of an MCS user, which usually is the authenticated 
                //user. Assuming the authenticated user has WRITE_ALL permission to an isolated collection she can
                //change the user context in which new objects are created, updated or deleted in a call to 
                //storageProxy.querySingleCollectionForUserId(collectionId, {id of other user -  //not username }) 
                //when creating a new StorageCollection object, or later by invoking setObjectOwnerUserID({id of other 
                //user - not username }), on the storage collection instance. 
                //
                //Example: 
                //To set the user id for the user that you want to create - update - delete objects in an isolated collection,
                //you can create the CollectionStorage by calling 
                //
                //storageProxy.querySingleCollectionForUserId("<a valid collectionId>", "6fa9bb4d-fa70-4a5c-8e29-5c544fa0d6dd")
                //To set the user id for which you want to create - delete - or update objects in an isolated collection
                //at runtime, you call currentStorageCollection.setObjectOwnerUserID("6fa9bb4d-fa70-4a5c-8e29-5c544fa0d6dd");
                //
                //you get the user ID of the authenticated user in a call to 
                //mobileBackend.getMbeConfiguration().getAuthenticatedUserMCSUserId()
                //
                //The method call below created the new object for the authenticated user. 
                StorageObject updateStorageObject = currentStorageCollection.createObject(displayName, contentType, byteContent);
                String json = updateStorageObject.toJSONString();
                
                this.logDcInfoForUserInterfaceDisplay("Object  successfuly created.");
                return json;

            } catch (ServiceProxyException e) {

                //check if this is an MCS error message or a REST Service transport layer message
                if (e.isApplicationError()) {
                    //if this is a well formatted Oracle Mobile error, we can display a user friendly error message
                    try {
                        OracleMobileError mobileError = OracleMobileErrorHelper.getMobileErrorObject(e.getMessage());
                        //print short description of error
                        logDcErrorForUserInterfaceDisplay(mobileError.getDetail());
                    } catch (JSONException f) {
                        logDcErrorForUserInterfaceDisplay(e.getMessage());
                    }
                }
                else{
                    logDcErrorForUserInterfaceDisplay("Object create operation returned: "+e.getMessage());
                }
            }
        } else {
            logDcErrorForUserInterfaceDisplay("No Storage collection selected");
        }
        return null;
    }

    /**
     * Method that deletes the referenced object from the MCS Storage. 
     * 
     *
     * @param canonicalUri The canonical URI as obtained from the StorageObject that contains the object Id and (for isolated collections) the owning user Id
     * @param ownerId  The id that identifies the owner of a collection
     */
    public void deleteObjectFromCollection(String canonicalUri) {

        this.resetMessages();

        if (currentStorageCollection != null) {
            try {
                //call MAF MCS Utility to delete the storage object by the canonical URI. By not providing eTag parameters, the
                //delete is performed and not only if the condition defined by the eTag parameter is met
                currentStorageCollection.removeCollectionObjectWithURI(canonicalUri, null);
                this.logDcInfoForUserInterfaceDisplay("Object with URI \"" + canonicalUri + "\" successfuly deleted.");

            } catch (ServiceProxyException e) {
                //check if this is an MCS error message or a REST Service transport layer message
                if (e.isApplicationError()) {
                    //if this is a well formatted Oracle Mobile error, we can display a user friendly error message
                    try {
                        OracleMobileError mobileError = OracleMobileErrorHelper.getMobileErrorObject(e.getMessage());

                        //print short description of error
                        logDcErrorForUserInterfaceDisplay(mobileError.getDetail());
                    } catch (JSONException f) {
                        logDcErrorForUserInterfaceDisplay(e.getMessage());
                    }
                }
            }
        }
    }


    /**
     * This method downloads as collection object and passes the file URL (absolute file path and name) to the caller
     * (managed bean in this sample). The caller then decides what to do with the downloaded content. The code below
     * saves the file in the mobile device download directory for display.
     * 
     * For isolated collections, this operation is performed in the context of the authenticated user, or if a object owning
     * user is set in the StorageCollection, in the context fo this user
     * 
     * To set the user id for the user that you want to create - update - delete objects in an isolated collection,
     * you can create the CollectionStorage by calling 
     * 
     * storageProxy.querySingleCollectionForUserId("a valid collectionId", "6fa9bb4d-fa70-4a5c-8e29-5c544fa0d6dd")
     * 
     * To set the user id for which you want to create - delete - or update objects in an isolated collection
     * at runtime, you call currentStorageCollection.setObjectOwnerUserID("6fa9bb4d-fa70-4a5c-8e29-5c544fa0d6dd");
     *
     * If you don't set the objectOwnerUserId in the StorageCollection, then all operations are for the authenticated user,
     * which means that attempts to update object of other user objects fails. To update other user objects you first need to 
     * set the objectOwnerUserId in the StorageCollection instance
     * 
     *
     * 
     * @param canonicalURI canonical link to the object. This information can be obtained from Storage Object class
     * @param mimeType the mime type of the object expected for download: e.g. image/*, image/jpeg, image/png, application/pdf etc.
     * @param targetFileName The filename and extension to store the downloaded content under e.g. myImage.png
     * @return String with the directory path and file name or null (if file could not be created)
     */
    public String downloadStorageObject(String canonicalURI, String mimeType, String targetFileName) {
        this.resetMessages();

        if (currentStorageCollection != null) {

            try {
                //call MAF MCS Utility to download the storage object
                byte[] downloadedContent = currentStorageCollection.downloadByteContentForObjectUri(canonicalURI, mimeType);
               
                //write to file. Define new file in download directory
                String outFile = AdfmfJavaUtilities.getDirectoryPathRoot(AdfmfJavaUtilities.DownloadDirectory) + "/"+targetFileName;
                
                FileOutputStream fos = new FileOutputStream(outFile);
                fos.write((byte[]) downloadedContent);
                fos.close();

                this.logDcInfoForUserInterfaceDisplay("Object with URI " + canonicalURI + " successfuly downloaded.");
                return outFile;

            } catch (ServiceProxyException e) {
                //check if this is an MCS error message or a REST Service transport layer message
                if (e.isApplicationError()) {
                    //if this is a well formatted Oracle Mobile error, we can display a user friendly error message
                    try {
                        OracleMobileError mobileError = OracleMobileErrorHelper.getMobileErrorObject(e.getMessage());

                        //print short description of error
                        logDcErrorForUserInterfaceDisplay(mobileError.getDetail());
                    } catch (JSONException f) {
                        logDcErrorForUserInterfaceDisplay(e.getMessage());
                    }
                }
            } catch (IOException e) {
                logDcErrorForUserInterfaceDisplay(e.getMessage());
            }
        }
        return null;
    }


    /**
     * displays objects owned by other users when querying the collection for collection objects
     * Background: In isolated collections, objects that are created through MAF or MCS platform APIs are created
     * in the context of a user. If a user has read_all and read_write_all access granted to an isolated collection,
     * then she can query objects owened by other users as well. For this you set the showObjectsOwnedByOtherUsers
     * in the sample. This is not applicable for shared collections because here all objects are accessible to all 
     * users by default. 
     * 
     * @param showObjectsOwnedByOtherUsers
     */
    public void setShowObjectsOwnedByOtherUsers(boolean showObjectsOwnedByOtherUsers) {
        boolean oldShowObjectsOwnedByOtherUsers = this.showObjectsOwnedByOtherUsers;
        this.showObjectsOwnedByOtherUsers = showObjectsOwnedByOtherUsers;
        propertyChangeSupport.firePropertyChange("showObjectsOwnedByOtherUsers", oldShowObjectsOwnedByOtherUsers,
                                                 showObjectsOwnedByOtherUsers);
    }

    public boolean isShowObjectsOwnedByOtherUsers() {
        return showObjectsOwnedByOtherUsers;
    }

    /*
  * *******************************************
  * USER MANAGEMENT APIs
  * *******************************************
 */

    /**
     * Username/password basedbasic authentication to the MCS MBE
     * @param username
     * @param password
     * @return true/false
     */
    public Boolean authenticateUser(String username, String password) {
        
        Boolean authenticationSuccess = Boolean.FALSE;
        //reset existing DC messages in UI
        resetMessages();

        //ensure a MBE object is created for the selected authentication type. Note that this 
        //call is "expensive" in that it re-creates the MBE object each time you authenticate.
        //in a production MAF application you may not need to re-create the MBE object each 
        //time you authenticate as you probably only support basic authentication or Oauth, so 
        //that a previously created MBE object can be reused        
        prepareMCSAccess();

        if (this.mobileBackend != null) {
            mobileBackend.getMbeConfiguration().getLogger().logFine("Username/password based authentication invoked from DC",this.getClass().getSimpleName(),"authenticateUser");

            //The Authorization object is returned based on the authorization type set when creating the
            //MBE Configuration object.
            Authorization authorization = this.mobileBackend.getAuthorizationProvider();

            try {
                //Important: You can perform authentication on the MAF Feature level too. However, if so then no authorization 
                //headers are set by MAF MCS Utility. So the code line below is important and mandatory if no authentication to 
                //MCS is performed by Oracle MAF.
                //
                //ServiceProxyException is thrown if authentication fails. 
                authorization.authenticate(username, password);
                
                //save username for display on pages
                AdfmfJavaUtilities.setELValue("#{applicationScope.mafmcsutilauthenticateduser}", username);
                
                authenticationSuccess = Boolean.TRUE;
                
            } catch (Exception e) {
                authenticationSuccess = Boolean.FALSE;
                //The ServiceProxyException knows to distinguish between application errors (MCS errors) and exceptions
                //that are thrown e.g. by the REST transport layer. If the exception represents an application error then
                //chances are the error message contains the Mobile Backend error object, which is a JSON structure you can
                //parse into a Java object as shown below
                if (e instanceof ServiceProxyException && ((ServiceProxyException) e).isApplicationError()) {
                    //try to get MCS mobile error object.
                    try {
                        OracleMobileError errorObject = OracleMobileErrorHelper.getMobileErrorObject(e.getMessage());
                        logDcErrorForUserInterfaceDisplay(errorObject.getTitle());

                    } catch (JSONException f) {
                        //not an MCS error object
                        logDcErrorForUserInterfaceDisplay(e.getMessage());
                    }
                } else {
                    logDcErrorForUserInterfaceDisplay(e.getMessage());
                }
            }
        } else {
            //set message to memory for UI to diaplay error
            String message = "Error: Mobile Backend not initialized.";
            logDcErrorForUserInterfaceDisplay(message);
        }

        //refresh first demo screen in case anonymous authentication hid the user management menu
        this.setAnonymousLogin(false);
        return authenticationSuccess;
    }

    /**
     * Anonymous login uses the MCS MBE anonymous key to authorize the request. Note tha this authentication has the
     * least privileges but works with Analytics and custom API if the latter doesn't require a role to be applied to
     * the authenticated user
     * @return true/false
     */
    public Boolean anonymousLogin() {
        Boolean authenticationSuccess = Boolean.FALSE;
        //reset existing DC messages in UI
        resetMessages();
        
        //ensure a MBE object is created for the selected authentication type. Note that this 
        //call is "expensive" in that it re-creates the MBE object each time you authenticate.
        //in a production MAF application you may not need to re-create the MBE object each 
        //time you authenticate as you probably only support basic authentication or Oauth, so 
        //that a previously created MBE object can be reused     
        prepareMCSAccess();
        
        if (this.mobileBackend != null) {
            mobileBackend.getMbeConfiguration().getLogger().logFine("Anonymous authentication invoked from DC",
                                                                    this.getClass().getSimpleName(), "anonymousLogin");                         
            
            Authorization authorization = this.mobileBackend.getAuthorizationProvider();
            try {
                //Important: You can perform authentication on the MAF Feature level too. However, if so then no authorization 
                //headers are set by MAF MCS Utility. So the code line below is important and mandatory if no authentication to 
                //MCS is performed by Oracle MAF.
                //
                //ServiceProxyException is thrown if authentication fails. 
                authorization.authenticateAsAnonymous();
                
                AdfmfJavaUtilities.setELValue("#{applicationScope.mafmcsutilauthenticateduser}", "anonymous");
                authenticationSuccess = Boolean.TRUE;
            } catch (ServiceProxyException e) {
                authenticationSuccess = Boolean.FALSE;

                //is exception dur to an MCS response error ?
                if (e.isApplicationError()) {
                    //if this is a well formatted Oracle Mobile error, we can display a user friendly error message
                    try {
                        OracleMobileError mobileError = OracleMobileErrorHelper.getMobileErrorObject(e.getMessage());
                        //print short description of error
                        logDcErrorForUserInterfaceDisplay(mobileError.getTitle());
                    } catch (JSONException f) {
                        logDcErrorForUserInterfaceDisplay(e.getMessage());
                    }
                } else {
                    logDcErrorForUserInterfaceDisplay(e.getMessage());
                }
            }
        }
        //refresh first demo screen in case un-anonymous authentication showed the user management menu
        this.setAnonymousLogin(true);
        return authenticationSuccess;
    }


    /**
     * Logn status needs to be determined by the system. Thus this method only allows to refresh the property status
     * @param anonymousLogin
     */
    public void setAnonymousLogin(boolean anonymousLogin) {
        boolean oldAnonymousLogin = this.anonymousLogin;
        this.anonymousLogin = anonymousLogin;
        propertyChangeSupport.firePropertyChange("anonymousLogin", oldAnonymousLogin, anonymousLogin);
    }


    /**
     * Check if authentication was anonymous
     * @return
     */
    public boolean isAnonymousLogin() {
        if (this.mobileBackend != null) {
            mobileBackend.getMbeConfiguration().getLogger().logFine("Checking if authentication is \"anonymous\"",
                                                                    this.getClass().getSimpleName(),
                                                                    "isAnonymousLogin");
            Authorization authorization = this.mobileBackend.getAuthorizationProvider();
            this.anonymousLogin = authorization.isAnonymousAuthentication();
        }
        return this.anonymousLogin;
    }

    /**
     * Logout only removes authentication from the user. It does not remove the MBE from the MBE Manager
     */
    public void logoutFromMBE() {

        resetMessages();

        if (this.mobileBackend != null) {
            mobileBackend.getMbeConfiguration().getLogger().logFine("logout called", this.getClass().getSimpleName(),
                                                                    "logoutFromMBE");
            Authorization authorization = this.mobileBackend.getAuthorizationProvider();
            try {
                authorization.logout();
            } catch (ServiceProxyException e) {
                mobileBackend.getMbeConfiguration().getLogger().logError("Logout fails with message: " + e.getMessage(),
                                                                         this.getClass().getSimpleName(),
                                                                         "logoutFromMBE");
            }
        }
    }

    /**
     * Initializes a mobile backend through configuration in the application preferences. If this public sample application
     * works as designed then the application does *NOT* get here without all the MCS related settings to be provided. Note
     * that the prepareMCSAccess() method is called from the sample immediately when the preferences are well set and the
     * demo is ready to start. This method does not check validness of the user provided prefrences. If prefrences are provided
     * wrong then the application fails
     */
    public void prepareMCSAccess() {

        //MCSUTILRESTCONN is a REST connection defined in the connections.xml file of this sample MAF application. For
        //MAF developers to test this sample with their MCS instance, the information in the connection needs cleared
        //and overridden.
        String mobileBackendUrl = (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.mcs.mobileBackendURL}");
        
        //clear
        AdfmfJavaUtilities.clearSecurityConfigOverrides("MCSUTILRESTCONN");
        
        //override
        AdfmfJavaUtilities.overrideConnectionProperty("MCSUTILRESTCONN", "restconnection", "url", mobileBackendUrl);
                
        //authtype = basic or oauth?
        String authType =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.auth.authtype}");
                
        //check whether authtype is "basic"
        if(authType.equalsIgnoreCase("BASIC")){
            prepareBasicAuthMBEInstance();
        }
        //else, "oauth" as we don't support another option yet
        else{
            prepareOAuthMBEInstance();
        }
        
        //log settings
        boolean loggingEnabled =  (Boolean) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.more.loggingEnabled}");
        this.mobileBackend.setLoggingEnabled(loggingEnabled);    
    }
    
    /**
     * The MAF MCS Utility supports basic authentication and oAUTH. This method, when invoked creates an MBE instance 
     * based on the basic authentication configuration saved in the application scope configuration bean
     */
    private void prepareBasicAuthMBEInstance(){
        
        
        //basicAuth settings
        String mobileBackendId =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.httpBasicAuth.mobileBackendId}");
        String mbeAnonymousKey =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.httpBasicAuth.mbeAnonymousKey}");        
        String applicationKey =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.mcs.mobileBackendApplicationKey}");     
        Boolean loggingEnabled =  (Boolean) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.more.loggingEnabled}");     
        
        
        /*
        *  ACCESS TO MAF MCS UTILITY TO CREATE BACKEND INSTANCE REGISTER MBE UNDER THE NAME OF THE PROVIDED MCS MBE ID
        */

        //MBE instance need to be configured to support a specific authentication mechanism. 
        MBEConfiguration mbeConfiguration = new BasicAuthMBEConfiguration("MCSUTILRESTCONN", 
                                                                          mobileBackendId,
                                                                          mbeAnonymousKey,
                                                                          applicationKey);

        //enable analytics for MBE
        mbeConfiguration.setEnableAnalytics(true);

        //logging can be enabled / disabled at runtime for a MBE instance. Note that logging for an MBE requires
        //a. logging to be enabled on the MVE configfuration (if set to false, no log messages are attempted to be written)
        //b. logging to be enabled for the MAF application (if enabled log level doesn't match level of log message then no message is written)
        mbeConfiguration.setLoggingEnabled(loggingEnabled);

        //try to identify the device so that analytics can distinguish between the devices owned by a person
        mbeConfiguration.setMobileDeviceId(DeviceManagerFactory.getDeviceManager().getName());
        this.mobileBackend = MBEManager.getManager().createOrRenewMobileBackend(mobileBackendId, mbeConfiguration);
        
        //access the MBE specific logger instance
        this.logger = mobileBackend.getMbeConfiguration().getLogger();  
    }
    
    
    /**
     * The MAF MCS Utility supports basic authentication and oAUTH. This method, when invoked creates an MBE instance 
     * based on the OAUTH authentication configuration saved in the application scope configuration bean
     */
    private void prepareOAuthMBEInstance(){
        
        
        
        //OAUTHENDPOINTCONN is a REST connection defined in the connections.xml file of this sample MAF application. For
        //MAF developers to test this sample with OAUTH configured on their MCS instance, the information in the connection 
        //needs cleared and overridden.        
        String oauthtokenendpoint =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.oAUTHAuthentication.oauthtokenendpoint}");
        
        //clear
        AdfmfJavaUtilities.clearSecurityConfigOverrides("OAUTHENDPOINTCONN");
        
        //override
        AdfmfJavaUtilities.overrideConnectionProperty("OAUTHENDPOINTCONN", "restconnection", "url", oauthtokenendpoint);

        
        //more oauth settings
        String clientId =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.oAUTHAuthentication.clientId}");
        String clientSecret =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.oAUTHAuthentication.clientSecret}");
        String identityDomain =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.oAUTHAuthentication.identityDomain}");
        
        String applicationKey =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.mcs.mobileBackendApplicationKey}");     
        Boolean loggingEnabled =  (Boolean) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.more.loggingEnabled}");   

        //MBE instance need to be configured to support a specific authentication mechanism. 
        MBEConfiguration mbeConfiguration = new OauthMBEConfiguration("MCSUTILRESTCONN","OAUTHENDPOINTCONN", clientId,
                                                                          clientSecret,
                                                                          applicationKey,
                                                                          identityDomain);

        //enable analytics for MBE
        mbeConfiguration.setEnableAnalytics(true);

        //logging can be enabled / disabled at runtime for a MBE instance. Note that logging for an MBE requires
        //a. logging to be enabled on the MVE configfuration (if set to false, no log messages are attempted to be written)
        //b. logging to be enabled for the MAF application (if enabled log level doesn't match level of log message then no message is written)
        mbeConfiguration.setLoggingEnabled(loggingEnabled);

        //try to identify the device so that analytics can distinguish between the devices owned by a person
        mbeConfiguration.setMobileDeviceId(DeviceManagerFactory.getDeviceManager().getName());
        this.mobileBackend = MBEManager.getManager().createOrRenewMobileBackend(clientId, mbeConfiguration);
        
        //access the MBE specific logger instance
        this.logger = mobileBackend.getMbeConfiguration().getLogger();  
        
        
    }



    /**
     * Returns the Application Policy key/value pairs developers optionally have defined on the Mobile Backend in 
     * Oracle Mobile Cloud Service. The application policy information can be used to share content across mobile
     * clients. This could include the application version number, a welcome string, a license string and many more.
     * The App policy types can be String, Number and Boolean
     *
     * @return JSON formatted user information
     */
    public ArrayList<Policy> getAppPolicy() {
        
        this.resetMessages();

        AppPolicies appPolicy = this.mobileBackend.getServiceProxyAppPolicy();
        try {
            ArrayList<Policy> policies = appPolicy.getAppPoliciesList();
            return policies;

        } catch (ServiceProxyException e) {
             logDcErrorForUserInterfaceDisplay(e.getMessage());
        }
        
        //return empty list in case of error
        return new ArrayList<Policy>();
    }


    /**
     * Returns the information stored in the MCS realm about the current authenticated user. This function is not
     * available if authentication is anonymous. The minimum set of information in the JSON string are "id", "username",
     * "email", "firstName", "lastName", "createdBy","createdOn","modifiedBy","modifiedOn" (if a value for the attributes
     * is provided). Other properties can be changed for a realm with custom properties like address, age, birthday, or
     * whatever a mobile application needs to know about the authenticated user
     *
     * @return JSON formatted user information
     */
    public String getUserInformation() {
        String userInfoJson = "";

        this.resetMessages();

        UserInfo userInfo = this.mobileBackend.getServiceProxyUserInfo();

        try {
            User user = userInfo.getCurrentUserInformation();
            userInfoJson = user.toJSONString();

        } catch (ServiceProxyException e) {
            if (e.isApplicationError()) {
                //if this is a well formatted Oracle Mobile error, we can display a user friendly error message
                try {
                    OracleMobileError mobileError = OracleMobileErrorHelper.getMobileErrorObject(e.getMessage());
                    //print short description of error
                    logDcErrorForUserInterfaceDisplay(mobileError.getTitle());
                } catch (JSONException f) {
                    logDcErrorForUserInterfaceDisplay(e.getMessage());
                }
            } else {
                logDcErrorForUserInterfaceDisplay(e.getMessage());
            }
        }
        return userInfoJson;
    }

    /*
     * *******************************************
     * HELPER METHODS - CONVENIENCE METHODS
     * *******************************************
    */

    /**
     * Messages are published to UI through shared application scope property. These messages need to be reset frequently
     */
    private void resetMessages() {
        AdfmfJavaUtilities.setELValue("#{applicationScope.dataControlError}", "");
        AdfmfJavaUtilities.setELValue("#{applicationScope.dataControlInfo}", "");
    }

    /**
     * Attempt to write messages to a shared memory scope for the UI to display errors
     * @param message
     */
    private void logDcErrorForUserInterfaceDisplay(String message) {
        AdfmfJavaUtilities.setELValue("#{applicationScope.dataControlError}", message);
    }

    /**
     * Attempt to write messages to a shared memory scope for the UI to display information
     * @param message
     */
    private void logDcInfoForUserInterfaceDisplay(String info) {
        AdfmfJavaUtilities.setELValue("#{applicationScope.dataControlInfo}", info);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
}
