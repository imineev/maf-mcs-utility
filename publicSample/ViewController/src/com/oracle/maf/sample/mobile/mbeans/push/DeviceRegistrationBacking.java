package com.oracle.maf.sample.mobile.mbeans.push;

import com.oracle.maf.sample.mobile.mbeans.utils.DataControlsUtil;

import oracle.adfmf.amx.event.ActionEvent;
import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;


/**
 *
 * Device registration enables push enabled MAF applications to receive notifications from Oracle
 * MCS. As a pre-requisite the MAF application must be signed with a push enabled p12 certificate
 * (for Apple) or have a Google sender Id (Android). You can read up on the MAF integration with
 * push notifications in the Oracle Mobile Application Framework documentation.
 *
 * The code that registers the MAF application with push providers in this sample is located in the
 * ApplicationController project. See: PushEventHandler in com.oracle.maf.sample.application.push as
 * well as the LifecycleListener class
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class DeviceRegistrationBacking {
    
    private static final String NOT_VISIBILE  = "visibility:hidden;";
    private static final String VISIBLE       = "visibility:visible;";
    
    String showRegistrationButtons = "visibility:hidden;";
    String showNotRegisteredMessage = "visibility:displayed;";
    
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public DeviceRegistrationBacking() {
        super();
    }
    
    //invoking this method makes the device known to MCS
    public void registerDeviceToMCS(ActionEvent event){        
        //no argument call to the MobileBackendDC
        DataControlsUtil.invokeDCNoArgumentMethod("registerDeviceWithMCS");        
    }

    //invoking this method unsubscribes the device from MCS notifications
    public void unRegisterDeviceFromMCS(ActionEvent event){
         //no argument call to the MobileBackendDC
        DataControlsUtil.invokeDCNoArgumentMethod("deRegisterDeviceFromMCS");
    }

    /**
     * Check if all the required settings are provided in the application preferences.
     * @return true if Google Sender Id, application mae and package and Apple bundle Id are provided
     */
    private boolean isAppReadyForPush() {
        Boolean isPushEnabled = (Boolean)AdfmfJavaUtilities.evaluateELExpression("#{preferenceScope.application.push.enablePush}");        
        return isPushEnabled.booleanValue();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }


    public void setShowRegistrationButtons(String showRegistrationButtons) {}

    /**
     * Allow mobile application to register to MCS if push is configured and enabled for the
     * sample application
     * 
     * @return CSS visibility String 
     */
    public String getShowRegistrationButtons() {
        
        boolean pushEnabled = isAppReadyForPush();
        
        if(pushEnabled == true){
            return VISIBLE;
        }
        else{
            return NOT_VISIBILE;
        }

    }

    public void setShowNotRegisteredMessage(String showNotRegisteredMessage) {}

    /**
     * Allow mobile application to register to MCS if push is configured and enabled for the
     * sample application. Otherwise show a message that push is not enabled
     * 
     * @return CSS visibility String 
     */
    public String getShowNotRegisteredMessage() {
        boolean pushEnabled = isAppReadyForPush();
        
        if(pushEnabled == false){
            return VISIBLE;
        }
        else{
            return NOT_VISIBILE;
        }
    }

}
