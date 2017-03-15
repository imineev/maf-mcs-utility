package com.oracle.maf.sample.mcs.apis.analytics;

import com.oracle.maf.sample.mcs.apis.analytics.constants.AnalyticsHeaderConstants;
import com.oracle.maf.sample.mcs.apis.analytics.db.AnalyticsDB;
import com.oracle.maf.sample.mcs.apis.analytics.db.SavedMessage;
import com.oracle.maf.sample.mcs.shared.authorization.auth.Authorization;
import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.headers.HeaderConstants;
import com.oracle.maf.sample.mcs.shared.log.MBELogger;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSResponse;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRestClient;
import com.oracle.maf.sample.mcs.shared.mbe.MBE;
import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;
import com.oracle.maf.sample.mcs.shared.utils.DateUtil;
import com.oracle.maf.sample.mcs.shared.utils.MAFUtil;
import com.oracle.maf.sample.mcs.shared.utils.MapUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import oracle.adf.model.datacontrols.device.DeviceManager;
import oracle.adf.model.datacontrols.device.DeviceManagerFactory;

import oracle.adfmf.json.JSONArray;
import oracle.adfmf.json.JSONException;
import oracle.adfmf.json.JSONObject;


/**
 *  *** INTERNAL USE ONLY  ***
 *
 * The Upload task sends the analytic event to the MCS server. If a event message fails, then it is saved in SQLite for
 * next time the same mobile backend sends an analytic event. If again the message fails then it will be saved back into
 * SQLite for a next attempt.
 *
 * Aanalytic event that fail their posting to MCS because of an invalid JSON payload or invalid message format are not
 * saved in SQLite. Instead the event list is cleared and the payload logged for analysis.  Log messages are writte for
 * FINE and ERROR levels that developers should check during development.
 *
 * The class does not throw an exception because at this stage the client wont be able to change the request. Logging is
 * used instead.
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public final class UploadTask implements Runnable {

    ArrayList<Event> mEventList = null;
    Analytics mAnalytics = null;
    Session mSession = null;
    MBEConfiguration mbeConfig = null;
    MBE mobileBackend = null;
    Authorization authorization = null;
    private  HashMap<String,String> mHeaderMap = null;

    //default events
    private JSONObject mContextEvent = null;
    private JSONObject mSessionStartEvent = null;
    private JSONObject mSessionEndEvent = null;

    private MBELogger mLogger = null;

    public UploadTask(Analytics analytics, ArrayList<Event> eventList, Session session) {

        super();
        this.mEventList = eventList;
        this.mAnalytics = analytics;
        this.mSession = session;
        this.mobileBackend = analytics.getMbe();
        this.mbeConfig = this.mobileBackend.getMbeConfiguration();
        this.authorization = this.mobileBackend.getAuthorizationProvider();
        //get MBE specific logger
        mLogger = mbeConfig.getLogger();
        mLogger.logFine("New Upload Task created for " + eventList.size() + " event(s)", this.getClass().getSimpleName(),
                        "Constructor");
        
    }

    @Override
    public void run(){

        //if no events available, ignore request
        if (this.mEventList.size() < 1) {
            return;
        }
        try {

            this.mContextEvent = createSystemJson();
            this.mSessionStartEvent = createSessionStartJson();
            this.mSessionEndEvent = createSessionEndJson();

            mLogger.logFine("Attempting to post " + this.mEventList.size() + " custom events", this.getClass().getSimpleName(),"run");
            postEvents();

        } catch (ServiceProxyException serviceProxyException) {
            //log the error
            mLogger.logError("Exception occured when uploading event to server", this.getClass().getSimpleName(),"run");
            mLogger.logError(serviceProxyException.getMessage(), this.getClass().getSimpleName(), "run");
        }
    }

    /**
     * creates JSON string to indicate start of analytic session. The JSON paload looks like
     *
     * {
     *   "name":"sessionStart",
     *   "type":"system",
     *    "timestamp":"2013-04-12T23:20:55.052Z",
     *    "sessionID":"2d64d3ff-25c7-4b92-8e49-21884b3495ce"
     *    }
     * @return JSONObject
     */
    private JSONObject createSessionStartJson() {

        mLogger.logFine("creating session start JSON", this.getClass().getSimpleName(), "createSessionStartJson");

        JSONObject json = new JSONObject();
        try {
            json.put("name", "sessionStart");
            json.put("timestamp", DateUtil.getISOTimeStamp(mSession.getStartTime()));
            json.put("sessionID", mSession.getSessionId());
            json.put("type", "system");
            json.put("component", this.mobileBackend.getApplicationFeatureName());
        } catch (Exception ex) {
            mLogger.logError("Could not create JSON Object because of Exception", this.getClass().getSimpleName(), "createSessionStartJson");
            mLogger.logError(ex.getMessage(), this.getClass().getSimpleName(), "createSessionStartJson");
        }
        return json;
    }

    /**
     * Creates JSON Object signalling the end of the recorded session to the server. Payload looks like
     *
     * {
     *   "name":"sessionEnd",
     *   "type":"system",
     *   "timestamp":"2013-04-12T23:25:55.052Z",
     *   "sessionID":"2d64d3ff-25c7-4b92-8e49-21884b3495ce"
     *  }
     *
     * @return
     */
    private JSONObject createSessionEndJson() {

        mLogger.logFine("creating session end JSON", this.getClass().getSimpleName(), "createSessionEndJson");

        JSONObject json = new JSONObject();
        try {
            json.put("name", "sessionEnd");
            json.put("timestamp", DateUtil.getISOTimeStamp(mSession.getEndTime()));
            json.put("sessionID", mSession.getSessionId());
            json.put("type", "system");
            json.put("component", this.mobileBackend.getApplicationFeatureName());

        } catch (Exception ex) {
            mLogger.logError("Could not create session end JSON Object because of Exception", this.getClass().getSimpleName(), "createSessionEndJson");
            mLogger.logError(ex.getMessage(), this.getClass().getSimpleName(), "createSessionEndJson");
        }
        return json;
    }

    /**
     * System events are logged with a JSON payload similar to
     * {
     * "name":"context",
     * "type":"system",
     * "timestamp":"2013-04-12T23:20:54.345Z",
     * "properties":{
     * "latitude":"37.35687",
     * "longitude":"-122.11663",
     * "timezone":"-14400",
     * "carrier":"AT&T",
     * "model":"iPhone5,1",
     * "manufacturer":"Apple",
     * "osName":"iPhone OS",
     * "osVersion":"7.1",
     * "osBuild":"13E28"
     * }
     * }
     * @return JSON Object
     */
    private JSONObject createSystemJson() throws ServiceProxyException {
        mLogger.logFine("Initializing System JSON", this.getClass().getSimpleName(), "createSystemJson");

        TimeZone timeZone = TimeZone.getDefault();
        JSONObject json = new JSONObject();
        try {
            json.put("name", "context");
            json.put("sessionID", this.mSession.getSessionId());
            json.put("type", "system");

            JSONObject properties = new JSONObject();

            DeviceManager dm = DeviceManagerFactory.getDeviceManager();
            properties.put("model", dm.getModel());
            properties.put("manufacturer", MAFUtil.getOsVendor());
            properties.put("timezone", Integer.toString(timeZone.getRawOffset() / 1000));
            properties.put("osName", MAFUtil.getDeviceOS());
            properties.put("osVersion", MAFUtil.getDeviceOSVersion());
            properties.put("longitude", "" + this.mAnalytics.getLongitude());
            properties.put("latitude", "" + this.mAnalytics.getLatitude());
            json.put("properties", properties);
            //needs to be added here to ensure proper JSON formatting
            json.put("timestamp", getISOTimeStamp());

        } catch (JSONException jsonException) {
            mLogger.logError("Could not create system JSON Object because of Exception", this.getClass().getSimpleName(), "createSystemJson");
            mLogger.logError(jsonException.getMessage(), this.getClass().getSimpleName(), "createSystemJson");
            throw new ServiceProxyException(jsonException.getCause(), ServiceProxyException.ERROR);
        }

        mLogger.logFine("System JSON created", this.getClass().getSimpleName(), "createSystemJson");
        return json;

    }

    private String getISOTimeStamp() {
        return DateUtil.getISOTimeStamp(new Date());
    }

    /**
     * Send collected events to the MBE instance
     *
     * @throws ServiceProxyException
     */
    private void postEvents(){

        //get diagnostic headers
        Map<String, String> diagnosticsHeaders = this.mobileBackend.getDiagnostics().getHTTPHeaders();
        //get authorization headers
        Map<String, String> authHeaders = this.authorization.getHTTPAuthorizationHeader();

        //Initialize Headers
        this.mHeaderMap = new  HashMap<String,String>();


        mHeaderMap.put(HeaderConstants.CONTENT_TYPE_HEADER, "application/json");
        mHeaderMap.put(HeaderConstants.APPLICATION_KEY_HEADER, this.mbeConfig.getMobileBackendClientApplicationKey());
        //mHeaderMap.put(HeaderConstants.ORACLE_MOBILE_BACKEND_ID, this.mbeConfig.getMobileBackendIdentifier());
        mHeaderMap.put(AnalyticsHeaderConstants.ANALYTIC_MOBILE_DEVICE_ID_HEADER, this.mbeConfig.getMobileDeviceId());
        mHeaderMap.put(AnalyticsHeaderConstants.ANALYTIC_SESSION_ID_HEADER, this.mSession.getSessionId());

        //Populate Diagnostic Headers
        for (String header : diagnosticsHeaders.keySet()) {
            mHeaderMap.put(header, diagnosticsHeaders.get(header));
        }

        //Populate Authorization Headers
        for (String header : authHeaders.keySet()) {
            mHeaderMap.put(header, authHeaders.get(header));
        }
        sendRequest();
    }

    /**
     * Send the REST request to the Mobile Backend Analytics API
     */
    private void sendRequest() {

        mLogger.logFine("sending server request", this.getClass().getSimpleName(), "sendRequest");
        JSONArray jsonArray = null;

        jsonArray = new JSONArray();

        jsonArray.put(this.mContextEvent);

        jsonArray.put(this.mSessionStartEvent);

        mLogger.logFine("adding custom events ", this.getClass().getSimpleName(), "sendRequest");
        
        for (int indx = 0; indx < this.mEventList.size(); indx++) {
            jsonArray.put(createEventJson(mEventList.get(indx)));
        }

        jsonArray.put(mSessionEndEvent);

        //network access available
        if (MAFUtil.isNetworkAccess()) {
            mLogger.logFine("Network access available: ready to send", this.getClass().getSimpleName(), "sendRequest");
            //Create a request context to hold request configuration before calling MAF
            //REST Service Adapter to post events to the MBE
            MCSRequest request = new MCSRequest(this.mbeConfig);
            request.setConnectionName(this.mbeConfig.getMafRestConnectionName());

            request.setHttpHeaders(this.mHeaderMap);
            request.setPayload(jsonArray.toString());

            mLogger.logFine("Header map: " + MapUtils.dumpStringProperties(mHeaderMap), this.getClass().getSimpleName(),
                            "sendRequest");
            mLogger.logFine("Payload : " + jsonArray.toString(), this.getClass().getSimpleName(), "sendRequest");

            request.setHttpMethod(com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest.HttpMethod.POST);
            //add MBE Analytics base Uri
            request.setRequestURI(AnalyticsHeaderConstants.ANALYTICS_RELATIVE_URL);
            mLogger.logFine("Analytic URI : " + AnalyticsHeaderConstants.ANALYTICS_RELATIVE_URL,
                            this.getClass().getSimpleName(), "sendRequest");
            //retry limit is set to 1 as there is no business case known yet
            //that demands more frequent retries (MAF issues a synchronous
            //call and the more often retry happens the longer users need to
            //wait).
            request.setRetryLimit(1);
            MCSResponse mcsResponse = null;
            try {
                //request REST Response in String
                mcsResponse = MCSRestClient.sendForStringResponse(request);
                mLogger.logFine("REST API called : " + jsonArray.toString(), this.getClass().getSimpleName(),
                                "sendRequest");

                if (mcsResponse != null) {
                    int status = mcsResponse.getHttpStatusCode();

                    if (status == AnalyticsHeaderConstants.HTTP_202) {
                        mLogger.logFine("Rest call successful: " + mcsResponse.getMessage(),this.getClass().getSimpleName(), "sendRequest");
                        mLogger.logFine("Clearing event list" + this.mEventList.size() + " events",this.getClass().getSimpleName(), "sendRequest");
                        
                        //reset event list
                        mEventList = new  ArrayList<Event>();

                        //if this was successful, chances are follow-up calls will be successful too. So lets try and flush any pending messages
                        //in the SQLite database for this mobile backend.
                        checkForAndSendSavedMessages(request);

                    } else {
                        //sending of message failed. Possible reasons are HTTP error 400 and 405 returned from MCS in case
                        //of a malformed JSON object or a wrong method call
                        mLogger.logError("REST Invocation Failed in call to Analytics:  " + mcsResponse.getMessage(),this.getClass().getSimpleName(), "sendRequest");
                    }
                } else {
                    mLogger.logFine("Rest call successful: NO RESPONSE MESSAGE", this.getClass().getSimpleName(),
                                    "sendRequest");
                    mLogger.logFine("Clearing event list" + this.mEventList.size() + " custom events",
                                    this.getClass().getSimpleName(), "sendRequest");
                    mEventList.clear();
                }
            }
            //MAF may throw an exception for the HTTP 202 response. To ensure the MAF MCS Utility behaves correct, the catch block
            //parses the response message for HTTP 202
            catch (Exception e) {

                mLogger.logFine("Exception in REST call to Oracle MCS. If exception is in response to a HTTP 202 response then this is a false positive. Checking ...",
                                this.getClass().getSimpleName(), "sendRequest");
                //failure in the REST Service Adapter call
                String exceptionPrimaryMessage = e.getLocalizedMessage();
                String exceptionSecondaryMessage = e.getCause() != null ? e.getCause().getLocalizedMessage() : null;
                String combinedExceptionMessage =  "primary message:"+exceptionPrimaryMessage+(exceptionSecondaryMessage!=null?("; secondary message: "+exceptionSecondaryMessage):(""));


                if (combinedExceptionMessage.contains("202")) {
                    mLogger.logFine("Rest call finished successful" + exceptionSecondaryMessage != null ?exceptionSecondaryMessage : "", this.getClass().getSimpleName(), "sendRequest");
                    mLogger.logFine("Clearing event list " + this.mEventList.size() + " custom events",this.getClass().getSimpleName(), "sendRequest");
                    
                    mEventList = new  ArrayList<Event>();
                    
                    //check if there are saved messages and send them
                    checkForAndSendSavedMessages(request);
                    
                } else {
                    mLogger.logError("REST Invocation Failed with Exception", this.getClass().getSimpleName(),"sendRequest");
                    mLogger.logError("Exception cause is: " + combinedExceptionMessage, this.getClass().getSimpleName(),"sendRequest");


                    /*
                     * HTTP 400         The request failed because the payload of JSON message was malformed, or because of an exception that occurred during processing
                     * HTTP 405         The request failed because it uses a method that is not supported by the resource
                     */
                    if (combinedExceptionMessage.contains("400") || (combinedExceptionMessage.contains("405"))) {
                        mLogger.logError("REST Invocation Failed with MCS error 400 or 405. Check the validness of the JSON payload and the request URI",
                                         this.getClass().getSimpleName(), "sendRequest");
                        //clear the event list. We loose the events, but with the malformed payload or the invalid operation it makes
                        //no sense to try and replay the request. Instead logging the problem should help resolving the issues.
                        mLogger.logError("Failed payload is: " + jsonArray.toString(), this.getClass().getSimpleName(),
                                         "sendRequest");
                        
                        mEventList = new  ArrayList<Event>();
                        
                        mLogger.logError("Message queue is cleared because we cannot recover from error",
                                         this.getClass().getSimpleName(), "sendRequest");
                    } else {
                        //failure in the REST Service Adapter call
                        mLogger.logError("Events are locally saved for later post to server", this.getClass().getSimpleName(), "sendRequest");
                        //sending of message failed. Save event message for later attempt
                        saveMessagesForLaterPosting(this.mbeConfig.getMobileBackendIdentifier(), jsonArray.toString(),this.mHeaderMap);
                    }
                }
            }
        }
        //offline use case
        else {
            //sending of message failed. Reading data from request object to save it in SQLite for later
            mLogger.logFine("No network access available. Saving events for later post to server",this.getClass().getSimpleName(), "sendRequest");
            saveMessagesForLaterPosting(this.mbeConfig.getMobileBackendIdentifier(), jsonArray.toString(), this.mHeaderMap);

        }
    }


    /**
     * Method that is called if the sending of an analytic event failed. This method then saves the message in a SQLite
     * database for later retrieval. The later retrieval is when an analytic event for the same mobile backend is send
     * again
     *
     * @param mobileBackendId the backend identifier for which the message is saved
     * @param message the JSON payload of the completed session information
     * @param headers header parameters
     */
    private void saveMessagesForLaterPosting(String mobileBackendId, String message,  HashMap<String,String> headers) {

        StringBuffer messageHeaderBuffer = new StringBuffer();
        Set headerKeySet = headers.keySet();

        //read all headers and format them as "key:value,key2:value2,..." string
        for (Object key : headerKeySet) {
            if (messageHeaderBuffer.length() > 0) {
                messageHeaderBuffer.append(",");
            }
            messageHeaderBuffer.append((String) key + ":" + (String) headers.get(key));
        }

        mLogger.logFine("Messages to save: "+messageHeaderBuffer.toString(),this.getClass().getSimpleName(), "saveMessagesForLaterPosting");
        AnalyticsDB.getInstance().saveForLater(mobileBackendId, message, messageHeaderBuffer.toString());
        //clear event list as it is now saved in SQLite for later
        mEventList.clear();
    }


    /**
     * Method that checks if there are saved analytic events for the MBE. If there are saved events, the method tries to
     * send them to the server. If server posting fails, the messages are ueued back into SQLite
     * @param request pre-configured RequestObject must contain all send settings except header and payload
     */
    private void checkForAndSendSavedMessages(MCSRequest request) {

        //get all pending messages from SQLite if there are any
        List<SavedMessage> pendingMessages = AnalyticsDB.getInstance().getPendingMessages(this.mbeConfig.getMobileBackendIdentifier());
        //iterate over list and attempt to send one-by-one. If sending succeeds, remove message from list. Note that this 
        //process still runs within an asynchronous thread so that there is no deay to expect in the mobile client application
        for (int i = 0; pendingMessages != null && i < pendingMessages.size(); i++) {

            SavedMessage message = pendingMessages.get(i);

            request = new MCSRequest(this.mbeConfig);
            request.setConnectionName(this.mbeConfig.getMafRestConnectionName());

            request.setHttpHeaders(message.getHeaderMap());
            request.setPayload(message.getPayload());

            mLogger.logFine("Header map: " + MapUtils.dumpStringProperties(message.getHeaderMap()),this.getClass().getSimpleName(), "checkForAndSendSavedMessages");
            mLogger.logFine("Payload : " + message.getPayload(), this.getClass().getSimpleName(),"checkForAndSendSavedMessages");

            request.setHttpMethod(com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest.HttpMethod.POST);
            //add MBE Analytics base Uri
            request.setRequestURI(AnalyticsHeaderConstants.ANALYTICS_RELATIVE_URL);

            mLogger.logFine("Analytic URI : " + AnalyticsHeaderConstants.ANALYTICS_RELATIVE_URL,this.getClass().getSimpleName(), "checkForAndSendSavedMessages");

            request.setRetryLimit(0);

            MCSResponse mcsResponse = null;

            try {
                mcsResponse = MCSRestClient.sendForStringResponse(request);

                //if successful remove message from list
                if (mcsResponse != null && mcsResponse.getHttpStatusCode() == AnalyticsHeaderConstants.HTTP_202) {
                    pendingMessages.remove(i);
                } else {
                    mLogger.logFine("Sending saved message failed for payload: " + message.getPayload(), this.getClass().getSimpleName(), "checkForAndSendSavedMessages");
                    mLogger.logFine("Error Code: " + mcsResponse.getHttpStatusCode() + ", Message: " +mcsResponse.getMessage(), this.getClass().getSimpleName(),"checkForAndSendSavedMessages");
                }
            //sending of the message caused an exception. Next we need to tell wether this failing is caused by a misinterpreted HTTP status or if this is
            //an error - like malformed JSON payload - that we can or cannot recover from.
            } catch (Exception e) {
                //check if the exception is thrown by mistake
                String exceptionPrimaryMessage      = e.getLocalizedMessage();
                String exceptionSecondaryMessage    = e.getCause() != null? e.getCause().getLocalizedMessage() : null;
                String combinedExceptionMessage     = "primary message:"+exceptionPrimaryMessage+(exceptionSecondaryMessage!=null?("; secondary message: "+exceptionSecondaryMessage):(""));
                 
                //HTTP 202 indicates success. If MAF fails then this exception handler ensures the functionality 
                //in this section works as designed
                if(combinedExceptionMessage.contains("202")){
                    //the exception message shows that HTTP error 202 caused this. HTTP 202 however means that the 
                    //request succeeded. So We remove the saved event.
                    pendingMessages.remove(i);
                }
                //we don't need to return a response object and also don't really care for why the request failed as
                //we assume the payload in the SQLite database was syntactically correct
                else{
                    mLogger.logFine("Sending saved message failed for payload: " + message.getPayload(), this.getClass().getSimpleName(), "checkForAndSendSavedMessages");
                }
            }
        }

        //clear SQLite messages for this backend. Even in the case of failing to send saved messages, we empty the SQLite
        //table for the attempted events and then re-fill. This way we can remove single events that may have passed the
        //attempted sending.
        boolean isDataPurged = AnalyticsDB.getInstance().purgeAnalyticMessagesForMobileBackend(this.mbeConfig.getMobileBackendIdentifier());

        //pending messages should be empty. If not, save messages back into SQLite
        if (isDataPurged && pendingMessages.size() > 0) {
            //re-save information that could not be sent to the MCS server
            for (int i = 0; i < pendingMessages.size();
                 i++) {
                //Save the messages again in the hope that next time they can be sent to the server
                this.saveMessagesForLaterPosting(this.mbeConfig.getMobileBackendIdentifier(),
                                                 pendingMessages.get(i).getPayload(),
                                                 pendingMessages.get(i).getHeaderMap());
            }
        } else {
            mLogger.logError("Purge of saved messages for mobile backedn ID " + this.mbeConfig.getMobileBackendIdentifier() +
                             " failed", this.getClass().getSimpleName(), "checkForAndSendSavedMessages");
        }
    }


    /**
     * Create JSON message from Analytic event
     * @param event 
     * @return JSONObject
     */
    private JSONObject createEventJson(Event event) {

        JSONObject json = new JSONObject();
        try {
            json.put("name", event.getName());
            json.put("timestamp", DateUtil.getISOTimeStamp(event.getTimestamp()));
            json.put("sessionID", event.getSessionId());
            json.put("type", "custom");
            //unlike in Android, there is no "component" in MAF. The equivalent is
            //the application feature
            json.put("component", this.mobileBackend.getApplicationFeatureName());

            JSONObject params = new JSONObject();
            for (Map.Entry<String, String> entry : event.getProperties().entrySet()) {
                params.put(entry.getKey(), entry.getValue());
            }

            if (params.length() > 0)
                json.put("properties", params);

        } catch (Exception ex) {
            mLogger.logError(ex.getMessage(), this.getClass().getSimpleName(), "createEventJson");
        }

        return json;
    }
}
