package com.oracle.maf.sample.application.push;

import java.util.logging.Level;

import javax.el.ValueExpression;

import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.framework.event.Event;
import oracle.adfmf.framework.event.EventListener;
import oracle.adfmf.framework.exception.AdfException;
import oracle.adfmf.util.Utility;

/**
 * The PushEventListener receives push notifications sent from Google Cloud Messaging (GCM) Service or Apple Push Notification
 * Service (APNS). This hander does not send message but only receives messages
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class PushEventListener implements EventListener {
    
    public PushEventListener() {   
        super();
    }


    /**
     * 
     * Method invoked by MAF framework in response to receiving notifications from Google or Apple  
     * @param event - event object passed in from PUSH receiving
     */
    public void onMessage(Event event) {

        String msg = event.getPayload();
                
        //set push message to application scope memory attribute for 
        //display in the view. This shows the raw mwwssage format.
        AdfmfJavaUtilities.setELValue(PushConstants.PUSH_MESSAGE,msg);  
        
        /* *** NOTIFICATION HANDLING HINT ***
         * 
         * The message payload structure differs between Apple and Google notifications. If the message was received in 
         * an iOS device, you can extract the notification alert with the code shown below:
         * 
         * HashMap payload      = (HashMap) JSONBeanSerializationHelper.fromJSON(HashMap.class, msg);        
         * String alertMessage  = (String) payload.get("alert");  
         * ... parse alertMessage content. E.g. if alert is a JSON string, you can do
         * 
         * JSONObject alertJson = new JSONObject(alertMessage); 
         * String jsonProperty  = alertJson.get...("property_name");
         * 
         * The same on Google would look like
         * 
         * JSONObject jsonObject  = new JSONObject(msg);
         * JSONObject alertJson   = jsonObject.getJSONObject("alert"); 
         * String jsonProperty  = alertJson.get...("property_name");
         * 
         */
        
    }


    public void onError(AdfException adfException) {
        Utility.ApplicationLogger.logp(Level.WARNING, this.getClass().getSimpleName(), "PushEventHandler::onError",
                                       "Message = " + adfException.getMessage() + "\nSeverity = " +
                                       adfException.getSeverity() + "\nType = " + adfException.getType());

        // Write the error into app scope
        AdfmfJavaUtilities.setELValue(PushConstants.PUSH_ERROR, adfException.toString());

    }


    /**
     * Upon successful registration to the notification service, this method obtains the unique token that identifies
     * this device to the push provider. This token is needed to register the device with Oracle MCS for MCS to send 
     * notifications to this device via the Googe and Apple push providers
     * @param token
     */
    public void onOpen(String token) {
                
        // Clear error in app scope
        AdfmfJavaUtilities.setELValue(PushConstants.PUSH_ERROR, null);
        
        // Write the token into app scope        
        AdfmfJavaUtilities.setELValue(PushConstants.PUSH_DeviceTOKEN,token);
    }
    
}
