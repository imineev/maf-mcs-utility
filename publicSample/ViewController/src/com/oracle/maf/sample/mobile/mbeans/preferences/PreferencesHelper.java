package com.oracle.maf.sample.mobile.mbeans.preferences;

import oracle.adf.model.datacontrols.device.DeviceManagerFactory;

import oracle.adfmf.amx.event.ValueChangeEvent;
import oracle.adfmf.framework.FeatureContext;
import oracle.adfmf.framework.api.AdfmfContainerUtilities;
import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;


/**
 * Managed bean class that support the task flow decision process of whether to navigate to the demo start view and
 * whether or not features show as enabled or not on the feature detail views.
 * <p>
 * Sample supports iOS and Android.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class PreferencesHelper {
    
    private String tabMenuSelection = "about";
    private String authTypeSwitcherState = "basic";
    private boolean enableAuthTypeSwitcher = false;
    
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public PreferencesHelper() {
        super();
        
        Object switcherStateObj = AdfmfJavaUtilities.getELValue("#{preferenceScope.application.auth.authtype}");
        authTypeSwitcherState = switcherStateObj == null? "basic" : (String) switcherStateObj;
        
    }
        
    
    /**
     * For this dample to work, application preferences need to be configured to access Oracle MCS. If no preferences
     * are are configured, navigation will be to a data input view, to the demo start otherwise
     * @return true/false
     */
    public boolean isHasPreferences(){
        
        boolean hasRequiredPreferencesSet = false;
        
        //get shared preferences 
        String mobileBackendUrl = (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.mcs.mobileBackendURL}");        
        String mbeApplicationKey = (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.mcs.mobileBackendApplicationKey}");
        
        //we need a mobile backendURL and the applicationKey to be provided. Otherwise has required preference should evaluate to false
        hasRequiredPreferencesSet = mobileBackendUrl != null && !mobileBackendUrl.isEmpty() ?
                                    mbeApplicationKey != null && !mbeApplicationKey.isEmpty() : false;
            
        
        //determine the type of authentication configured for the MBE
        String authType =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.auth.authtype}");
        
        //authtype must be set to "basic" or "oauth" as defined in the mobile application preferences
        if(hasRequiredPreferencesSet && (authType.equalsIgnoreCase("basic")|| authType.equalsIgnoreCase("oauth"))){
                                    
            if(authType.equalsIgnoreCase("basic")){
                
                //we are here, which means that hasRequiredPreferencesSet must have a value of true. Next we need to check the
                //value of the mobile backend Id and anonymous key needed for HTPP basic authentication
                hasRequiredPreferencesSet = checkBasicAuthPreferences();
                
                //next cross-check if the preferences for Oauth are available too. If so then the login screen shows
                //the authentication type switcher enabled. If not, the switcher will be disabled
                boolean isOauthEnabled = this.checkOauthPreferences();
                this.setEnableAuthTypeSwitcher(isOauthEnabled);
              
            }
            else{
              
             //we are here, which means that hasRequiredPreferencesSet must have a value of true and the authentication type is "oauth". 
             //Next we need to check the oauth token endpoind, the client Id, the client secret and the identity domain properties that 
             //are required for oauth authentication
             hasRequiredPreferencesSet = checkOauthPreferences();
                
             //next cross-check if the preferences for Basic authentication are available too. If so then the login screen shows
             //the authentication type switcher enabled. If not, the switcher will be disabled
             boolean isBasicEnabled = this.checkBasicAuthPreferences();
             this.setEnableAuthTypeSwitcher(isBasicEnabled);
            }
            
        }
            
        return hasRequiredPreferencesSet;
    }

    /**
     * Verifies that all application preference items for Oauth authentication have been set in the preference panel
     * @return true (they are set), false (they are not set)
     */
    private boolean checkOauthPreferences() {
        
        boolean hasRequiredPreferencesSet = false;
        
        String oauthtokenendpoint =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.oAUTHAuthentication.oauthtokenendpoint}");
        String clientId =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.oAUTHAuthentication.clientId}");
        String clientSecret =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.oAUTHAuthentication.clientSecret}");
        String identityDomain =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.oAUTHAuthentication.identityDomain}");
           
        hasRequiredPreferencesSet =  oauthtokenendpoint!=null && !oauthtokenendpoint.isEmpty()?
                                    (clientId!=null && !clientId.isEmpty()?
                                    (clientSecret!=null && !clientSecret.isEmpty()?
                                    identityDomain!=null && !identityDomain.isEmpty() :false) : false):false;
        return hasRequiredPreferencesSet;
    }


    /**
     * Verifies that all application preference items for basic Http authentication have been set in the preference panel
     * @return true (they are set), false (they are not set)
     */
    private boolean checkBasicAuthPreferences() {
        
        boolean hasRequiredPreferencesSet = false;
        
        //basicAuth settings
        String mobileBackendId =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.httpBasicAuth.mobileBackendId}");
        String mbeAnonymousKey =  (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.httpBasicAuth.mbeAnonymousKey}");
        
        hasRequiredPreferencesSet =  mobileBackendId!=null && !mobileBackendId.isEmpty()?
                                    mbeAnonymousKey!=null && !mbeAnonymousKey.isEmpty() : false;
        return hasRequiredPreferencesSet;
    }
    
    
    /**
     * <b> Push PRE-REQUISITES </b>
     * <p>
     * The GCM Sender ID is required for applications to receive push notifications from Apple. The registration is upon
     * application start. Without this information no messages can be received by this sample. You get the sender Id and 
     * a project key when registering the application on the Google DCM site.
     * <p>
     * For receiving push notification from Apple you need to 
     *  - register the application with Apple
     *  - change the application bundle Id in the deployment profile to match the ID used when registering the application 
     *  - Upon deployment, sign the application with the p12 certificate (certificate should not require a password) obtained 
     *  - when registering the application. 
     *  <p>
     *  <b> Enable push notifications in MCS</b>
     *  For MCS to send push messages,  ensure a client application is created for Android and iOS. The client application for
     *  Android requires the application package name and application name as the ID, along with the project key and the sender
     *  Id you obtained from Google.
     *  <p>
     *  For iOS, define the ID as the application bundled Id and upload the p12 certificate  
     *  <p>
     *  <p>
     *  For more information see the MAF and MCS product documentation
     * @return
     */
    public boolean isPushEnabled(){
        boolean pushEnabled = false;
        
        //Rule 1= if OS == Android && gcmSenderId == null or empty ==> false. True otherwise. 
        //Rule 2= if OS == iOS && applicationBundleId == null or empty ==> false. True otherwise.
        
        if(!isIOS()){
            String gcmSenderId = (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.push.gcmSenderId}");            
            pushEnabled = (gcmSenderId!=null && !gcmSenderId.isEmpty())? true : false;
        }
        else if(isIOS()){
             String iOsApplicationBundleId = (String) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.push.iosApplicationBundleId}");
             pushEnabled = (iOsApplicationBundleId!=null && !iOsApplicationBundleId.isEmpty())? true : false;
        }
        return pushEnabled;
    }
        
    /**
     * Check if this application runs on iOS and if, return true. If not return false
     * @return
     */
    public boolean isIOS(){
        return DeviceManagerFactory.getDeviceManager().getOs().toUpperCase() == "IOS";
    }
        
    //satisfy the Java properties API
    public void setHasPreferences(boolean b){};
    public void setPushEnabled(boolean b){};
    public void setIOS(boolean b){}

    /**
     * When the boolean swicth "Enable Receiving Push Notifications" in the preference screen is changed 
     * to true, show an alert to inform user that notifications require a restart of the application.
     * @param valueChangeEvent
     */
    public void onEnablePushBooleanSwitch(ValueChangeEvent valueChangeEvent) {
        
        //alert user to restart applicatio for push to work
        if(((Boolean)valueChangeEvent.getNewValue()).booleanValue() == true){
           Object errorMsg = AdfmfContainerUtilities.invokeContainerJavaScriptFunction(FeatureContext.getCurrentFeatureId(),"popupUtilsShowPopup", new Object[] {"_popShowId" });
        }
    }


    public void setTabMenuSelection(String tabMenuSelection) {
        String oldTabMenuSelection = this.tabMenuSelection;
        this.tabMenuSelection = tabMenuSelection;
        propertyChangeSupport.firePropertyChange("tabMenuSelection", oldTabMenuSelection, tabMenuSelection);
    }

    public String getTabMenuSelection() {
        return tabMenuSelection;
    }

    /**
     * Switches the value of the deck component, displaying a different area of the settings
     * @param valueChangeEvent
     */
    public void onTabMenuSelect(ValueChangeEvent valueChangeEvent) {
        setTabMenuSelection((String)valueChangeEvent.getNewValue());
    }
    
    public void onAuthTypeSelect(ValueChangeEvent valueChangeEvent) {
        this.setAuthTypeSwitcherState((String)valueChangeEvent.getNewValue());
    }

    /**
     * Switches the value of the deck component, displaying configuration settings for basic authentication
     * and oauth authentication
     * @param valueChangeEvent
     */
    public void setAuthTypeSwitcherState(String authType) {

        String oldAuthType = this.authTypeSwitcherState;
        this.authTypeSwitcherState = authType;
        
        propertyChangeSupport.firePropertyChange("authTypeSwitcherState", oldAuthType, authType);
    }


    public String getAuthTypeSwitcherState() {
        return this.authTypeSwitcherState;
    }


    public void setEnableAuthTypeSwitcher(boolean enableAuthTypeSwitcher) {
        boolean oldEnableAuthTypeSwitcher = this.enableAuthTypeSwitcher;
        this.enableAuthTypeSwitcher = enableAuthTypeSwitcher;
        propertyChangeSupport.firePropertyChange("enableAuthTypeSwitcher", oldEnableAuthTypeSwitcher,
                                                 enableAuthTypeSwitcher);
    }

    public boolean isEnableAuthTypeSwitcher() {
        return enableAuthTypeSwitcher;
    }

    public void setPropertyChangeSupport(PropertyChangeSupport propertyChangeSupport) {
        PropertyChangeSupport oldPropertyChangeSupport = this.propertyChangeSupport;
        this.propertyChangeSupport = propertyChangeSupport;
        propertyChangeSupport.firePropertyChange("propertyChangeSupport", oldPropertyChangeSupport,
                                                 propertyChangeSupport);
    }

    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

}
