package com.oracle.maf.sample.mcs.apis.analytics;

import com.oracle.maf.sample.mcs.apis.analytics.db.AnalyticsDB;
import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.mbe.proxy.MBEServiceProxy;
import com.oracle.maf.sample.mcs.shared.utils.MAFUtil;
import com.oracle.maf.sample.mcs.shared.utils.MapUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import oracle.adf.model.datacontrols.device.DeviceManagerFactory;
import oracle.adf.model.datacontrols.device.Location;

import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;


/**
 *
 * Proxy class to access Mobile Cloud Service (MCS) analytic features and functionality. The Mobile Client SDK Analytics
 * captures Mobile Analytics Events and uploads them in batches to the Analytics Collector Service in MCS. Note that this
 * API executes events asynchonously so that there is no need to wrap it in an asynchronous method invocation.
 * <p>
 * Collecting analytic events are started with a call to startSession() and ended with a call to endSession(), which is
 * then when the event is sent to the MCS server for persistence. If sending of an analytic event message fails, it will
 * saved for later in an internal - encrypted - SQLite database
 * <p>
 * About Analytics:
 * <p>
 * The MAF MCS Utility logs these mobile events through a RESTful web service. The Collector API provides this web service,
 * which builds metric objects from the incoming JSON stream and feeds them to the MetricFeederAPI within MCS. The metric
 * objects are then stored as mobile events in the metric store, where they can later be retrieved for analysis.
 *<p>
 * Requirements:
 *<p>
 * If a batch of events has a context event, then it must begin with a context event that specifies the context for all
 * subsequent events until another context event occurs, such as the change in location.
 * <p>
 *
 *  All events must include these properties:
 *  <ul>
 *      <li>name - A client, or system-defined event name. This name can be up to 100 characters long.</li>
 *      <li>timestamp - The time of the event, specified in RFC 3339 format as UTC. Note: If this timestamp is formatted incorrectly, MCS will reject these events and not log them.</li>
 * </ul>
 * <p>
 * All events can optionally have a sessionID field in the JSON payload or define a Oracle-Mobile-Analytics-Session-Id header value</li>
 * <p>
 * All events can optionally have one of the following types: "custom", "system"</li>
 * <p>
 * The context event will have these properties:
 * <ul>
 * <li>latitude</li>
 * <li>longitude</li>
 * <li>timezone</li>
 * <li>carrier</li>
 * <li>model</li>
 * <li>manufacturer</li>
 * <li>osName</li>
 * <li>osVersion/li>
 * <li>osBuild</li>
 * </ul>
 * <p>
 *  Custom properties are any number or properties the client Utility chooses to include. Custom event properties must be strings (such as
 *  "cartContent":"BMW" defined for a PurchaseFailed event" Custom event properties can't be defined as numbers, booleans, nulls, arrays,
 *  or complex values.
 *  <p>
 *  Limits: The property key can be up to 500 characters long. The value can be 1000 characters long.
 *  <p>
 *  System-defined events are indicated by a "type" of "system" (such as sessionStart in the above example).
 *  <p>
 *  context - The Analytics Proxy automatically generates a context event for the start of every uploaded batch of events and whenever the context
 *  changes (typically when the mobile device's location changes)
 *  <p>
 *  sessionStart - This event marks the start of an Analytics session. The start of session can be either explicitly defined within the mobile application
 *  code using the startSession method, or implied by the logging of the first custom event. It's possible to have a sessionStart event without a corresponding
 *  sessionEnd event, and vice-versa.
 *  <p>
 *  sessionEnd - This event marks the end of an Analytics session. This event is optional and occurs only if it is explicitly defined within the mobile application
 *  code using the endSession method (which is what this MAF MCS Utility does)
 *  <p>
 *   Context properties
 *   <ul>
 *    <li>timezone - The mobile device's offset from UTC, in seconds (optional)</li>
 *    <li>carrier - The mobile device's carrier (that is, the plan carrier) (optional) --> the MAF MCS Utility is not able to detect this</li>
 *    <li>model - The mobile device model name (optional)</li>
 *    <li>manufacturer - The mobile device manufacturer (optional)</li>
 *    <li>osName - The mobile device operating system name (optional)</li>
 *    <li>osVersion - The mobile device operating system version (optional)</li>
 *    <li>osBuild - The mobile device operating system build (optional) --> MAF MCS Utility is not able to detect this</li>
 *    <li>latitude - The mobile device's GPS latitude (optional)</li>
 *    <li>longitude - The mobile device's GPS longitude (optional)</li>
 *    </ul>
 *
 *<p>
 * REST URI: /mobile/platform/analytics/events
 * <p>
 *
 * HTTP Codes returned from MCS
 * -----------------------------
 * HTTP 202 The events have successfully been logged
 * HTTP 400 The request failed because the payload of JSON message is not well-formed, or because an of exception that occurred during processing.
 * HTTP 405 The request failed because it uses a method that is not supported by the resource.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public final class Analytics extends MBEServiceProxy {
    
    private Session mSession = null;
    private List<Event> mEvents = null;
    
    private double mLongitude = 0;
    private double mLatitude = 0;
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public Analytics() {
        super();
       
        //create a list of events
        this.mEvents = new ArrayList<Event>();
        
        //get longitude and latitude information and set them to local 
        //variable
        setCurrentLocationIfPossible();
    }

    /**
     * Analytic events are colleted for a session (recording period) and then uploaded to MCS in a batch
     */
    public void startSession() {
        
            this.getMbe().getMbeConfiguration().getLogger().logFine("start session", this.getClass().getSimpleName(), "startSession");
            if(mSession == null) {
                this.getMbe().getMbeConfiguration().getLogger().logFine("creating new Session object", this.getClass().getSimpleName(), "startSession");
                mSession = new Session();
                this.getMbe().getMbeConfiguration().getLogger().logFine("new session created; Session ID:" +mSession.getSessionId(), this.getClass().getSimpleName(), "startSession");
            }
            else{
             this.getMbe().getMbeConfiguration().getLogger().logFine("Existing session found with ID: "+mSession.getSessionId(),this.getClass().getSimpleName(),"startSession");
            }
    }
    

    /**
     * Creates a new default scoped event and adds it to the outgoing queue. Event is configured with event name, session ID and
     * a generated timestamp
     * 
     * @param eventName custom name that identifies the application task this event is recorded for (e.g. placeOrder, cancelOrder, etc.)
     * @return Event the new default event object
     * @throws IllegalArgumentException if eventName argument is provided as null
     */
    public Event addEmptyEvent(String eventName) {
        
        if(eventName == null){
            throw new IllegalArgumentException("eventName argument in call to addEmptyEventToOutgoingQueue in Analytics cannot be null");    
        }
        
        this.getMbe().getMbeConfiguration().getLogger().logFine("adding new event for name: "+eventName, this.getClass().getSimpleName(), "addNewEventToOutgoingQueue");
        
        if(eventName == null) {
            this.getMbe().getMbeConfiguration().getLogger().logFine("event name is null. Throwing exception", this.getClass().getSimpleName(), "addNewEventToOutgoingQueue");
            throw new IllegalArgumentException("'name' cannot be null");
        }
        
        if (mSession == null) {            
            mSession = new Session();
            this.getMbe().getMbeConfiguration().getLogger().logFine("no current session found. Creating new session with ID: "+mSession, this.getClass().getSimpleName(), "addNewEventToOutgoingQueue");
        }
        Event event = new Event(eventName, mSession.getSessionId());
        this.mEvents.add(event);
        this.getMbe().getMbeConfiguration().getLogger().logFine("new event created and added to list", this.getClass().getSimpleName(), "addNewEventToOutgoingQueue");
        return event;
    }

    /**
     * Add event object to list of events to be published to the server
     * @param event a pre-created event object that describes the event to log
     * @throws IllegalArgumentException if the event argument is missing
     */
    public Event addCustomEvent(Event event) {
        if(event == null) {
            this.getMbe().getMbeConfiguration().getLogger().logError("event object cannot be NULL", this.getClass().getSimpleName(), "addExistingEventToOutgoingQueue");
            throw new IllegalArgumentException("Event argument in call to addEventToOutgoingQueue in Analytics cannot be null");
        }
        
        this.getMbe().getMbeConfiguration().getLogger().logFine("event object found. Session ID="+event.getSessionId()+" TimeStamp="+event.getTimestamp()+" Properties=" + MapUtils.dumpStringProperties(event.getProperties()), this.getClass().getSimpleName(), "addExistingEventToOutgoingQueue");
        
        if (mSession == null) {
            mSession = new Session();
            this.getMbe().getMbeConfiguration().getLogger().logFine("no current session found. Creating new session with ID: "+mSession, this.getClass().getSimpleName(), "addExistingEventToOutgoingQueue");
            
        }
        
        //add data for event if missing
        if(event.getTimestamp()==null){
          event.setTimestamp(new Date());
        }
        
        //add session ID if missing
        if(event.getSessionId()==null){
            event.setSessionId(mSession.getSessionId());    
        }
        
        this.mEvents.add(event);
        
        return event;
    } 
      
   /**  
     *  Ends the recording of an analytic event and sends the event object to the server. Takes the events in the current 
     *  queue and sends them to the server. Ensure analytics is enabled for the MBE instance as otherwise no messages are 
     *  sent. 
     * @return nothing. In the case of success the server response with the number of events it saved. Otherwise a HTTP 400 or HTTP 405 
     * error is returned that indicates that the JSON playload is invalid or a method that doesn't exist is called. In both cases there is
     * no way to recover and this the event messages are lost and the issue is logged. For other errors, the events are saved for later with
     * another message being logged. As there is no data returned to MAF, this method doesn't return a value.
     * 
     * @throws ServiceProxyException in case of an invocation or JSON parsing error. 
     */
    public void endSession() throws ServiceProxyException{
                  
        this.getMbe().getMbeConfiguration().getLogger().logFine("attempt to post events to server", this.getClass().getSimpleName(), "flushEventQueueToServer");
        if(mEvents.size() < 1) {
            this.getMbe().getMbeConfiguration().getLogger().logWarning(" - Events queue is empty. No server post necessary",this.getClass().getSimpleName(),"flushEventQueueToServer");
            return;
        }
        
        this.getMbe().getMbeConfiguration().getLogger().logFine("Check if analytics is enabled for MBE", this.getClass().getSimpleName(), "flushEventQueueToServer");    
        
        if(this.getMbe().getMbeConfiguration().isEnableAnalytics()) {
            this.getMbe().getMbeConfiguration().getLogger().logFine("Analytics is enabled", this.getClass().getSimpleName(), "flushEventQueueToServer");
            mSession.setEndTime(new Date());
            this.getMbe().getMbeConfiguration().getLogger().logFine("Preparing upload of events", this.getClass().getSimpleName(), "flushEventQueueToServer");
            
            Runnable uploadTask = new UploadTask(this, new ArrayList<Event>(mEvents),mSession);
            
            this.getMbe().getMbeConfiguration().getLogger().logFine("Clearing event queue", this.getClass().getSimpleName(), "flushEventQueueToServer");
            mEvents = new ArrayList<Event>();
            this.getMbe().getMbeConfiguration().getLogger().logFine("End of Analytic session", this.getClass().getSimpleName(), "flushEventQueueToServer");
            mSession = null;
            this.getMbe().getMbeConfiguration().getLogger().logFine("Uploading events", this.getClass().getSimpleName(), "flushEventQueueToServer");
            
            //get GEO Location if device supports it
            this.getMbe().getMbeConfiguration().getLogger().logFine("Trying to read GEO Location data", this.getClass().getSimpleName(), "Constructor");                                                        
            ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
            mExecutorService.execute(uploadTask);
        }
        else{
            this.getMbe().getMbeConfiguration().getLogger().logFine("Analytics is disabled for MBE", this.getClass().getSimpleName(), "flushEventQueueToServer");
        }
       return; 
    }
   
    /**
     * MAF reads langitude and longitude information from the mobile device. If the device does not support GEO
     * ocations, or if the longitude and latitude information cannot be accessed, then there is a 1 minute time
     * -out defined in the MAF framework, causing delays of 60 seconds for synchronous calls. To avoid this delay
     * for Analytic events, this calls the information in a separate thread and then saves it for the duration of
     * the Analytic class.
     * 
     * If no location can be determined, longitude and latitude values are set to 0. So in the worst case there is
     * no location GPS available
     * 
     */
    private void setCurrentLocationIfPossible(){
        
        Runnable locationRetriever = new Runnable(){

            @Override
            public void run() {
                if(MAFUtil.isGeoLocationAvailable()){            
                    getMbe().getMbeConfiguration().getLogger().logFine("GEO location permission available", this.getClass().getSimpleName(), "Constructor");
                    //allow location information to be up to 10 minutes old
                    int maxAllowedAgeofCachedLocationData = 600;
                    //position doesn't need to be highly accurate for analytics
                    boolean enableHighAccuracy = false;
                    //If the mobile application runs on e.g. an iOS simulator or a device that does not have GEO location features,
                    //an exception is thrown that we need to handle
                    try{
                        Location location = DeviceManagerFactory.getDeviceManager().getCurrentPosition(maxAllowedAgeofCachedLocationData,enableHighAccuracy);                                                                    
                        mLongitude = location.getLongitude();
                        mLatitude = location.getLatitude();
                        
                        getMbe().getMbeConfiguration().getLogger().logFine("Longitude: "+location.getLongitude()+" Latitude: "+location.getLatitude(), this.getClass().getSimpleName(), "Constructor");
                    }
                    catch(Exception e){
                        getMbe().getMbeConfiguration().getLogger().logFine("The application runs on a device that has no access to the GEO location (Simulator?). Longitude and Latitude set to 0.", this.getClass().getSimpleName(), "Constructor");        
                        mLongitude = 0;
                        mLatitude = 0;
                        
                    }
                               
                }
                else{
                    getMbe().getMbeConfiguration().getLogger().logFine("GEO Location data not available. Please check permission. Longitude and Latitude set to 0", this.getClass().getSimpleName(), "Constructor");
                    mLongitude = 0;
                    mLatitude = 0;
                }
            }
        };
        
        ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
        mExecutorService.execute(locationRetriever);
        
    }

    public void setSession(Session session) {
        this.mSession = session;
    }

    public Session getSession() {
        return mSession;
    }

    public void setEvents(List<Event> events) {
        this.mEvents = events;
    }

    public List<Event> getEvents() {
        return mEvents;
    }

    /**
     * The Analytics Service Proxy reads the user GEO location when it is instantated (usually first time it is accessed
     * The GEO location is added to the system event in each analytic session. This method allows application developers 
     * to overwrite and re-new the longitude infomation in this class anytime they like.
     * 
     * @param mLongitude
     */
    public void setMLongitude(double mLongitude) {
        double oldMLongitude = this.mLongitude;
        this.mLongitude = mLongitude;
        propertyChangeSupport.firePropertyChange("mLongitude", oldMLongitude, mLongitude);
    }

    /**
     * Longitude value kept internally in this class instance. This information is used in system events of the 
     * analytic session and can be changed by developers (or renewed, triggered by developers)
     * @return
     */
    public double getLongitude() {
        return mLongitude;
    }

    /**
     * The Analytics Service Proxy reads the user GEO location when it is instantated (usually first time it is accessed
     * The GEO location is added to the system event in each analytic session. This method allows application developers 
     * to overwrite and re-new the latitude infomation in this class anytime they like.
     * 
     * @param mLongitude
     */
    public void setMLatitude(double mLatitude) {
        double oldMLatitude = this.mLatitude;
        this.mLatitude = mLatitude;
        propertyChangeSupport.firePropertyChange("mLatitude", oldMLatitude, mLatitude);
    }
    
    /**
     * Latitude value kept internally in this class instance. This information is used in system events of the 
     * analytic session and can be changed by developers (or renewed, triggered by developers)
     * @return
     */
    public double getLatitude() {
        return mLatitude;
    }
    
    /**
     * The Analytics Service Proxy reads the user GEO location when it is instantated (usually first time it is accessed
     * The GEO location is added to the system event in each analytic session. This method allows application developers 
     * to trigger re-reading if GEO location information through the Analytic class. If the device does not provide GEO 
     * information, then langitude and latitude values are set to 0. The rereading of GEO information is asynchronously 
     * read, which means that there may be a delay (due to time out settings) in when the latitude and longitude values 
     * set with the new value. 
     * 
     * @param mLongitude
     */
    public void refreshGeoLocationInformation(){
        //reset internal lnituded and latitude values
        this.setCurrentLocationIfPossible();
    }
    
    /**
     * Housekeeping: The system automatically deletes all pending events for a MBE after they have been deileverd to MCS 
     * analytic engine. However, if there is a need to explicitly delete events saved in SQLite then this method can be 
     * called to delete entries for the MBE that this service proxy is attached to
     */
    public void purgeAnalyticMessagesForMobileBackend(){
        String backendId = this.getMbe().getMbeConfiguration().getMobileBackendIdentifier();
        this.getMbe().getMbeConfiguration().getLogger().logFine("Deleting all messages saved in SQLite for MBE "+backendId, this.getClass().getSimpleName(), "purgeAnalyticMessagesForMobileBackend");
        AnalyticsDB.getInstance().purgeAnalyticMessagesForMobileBackend(backendId);        
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
}
