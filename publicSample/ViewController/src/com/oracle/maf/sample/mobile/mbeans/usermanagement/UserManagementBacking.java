package com.oracle.maf.sample.mobile.mbeans.usermanagement;

import com.oracle.maf.sample.mobile.mbeans.utils.DataControlsUtil;

import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;
import oracle.adfmf.json.JSONException;
import oracle.adfmf.json.JSONObject;

/**
 * Managed beans that supports the user management API of MAF MCS Utility. The bean displays
 * user information for the authenticated MCS mobile user
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class UserManagementBacking {
        
    String userId = "";
    String username = "";
    String firstName = "";
    String lastName = "";
    String email = "";
    
    String displayErrorMessage = "";
    
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public UserManagementBacking() {
        super();
    }
    
    
    /**
     * populate the user information
     */
    public void initUserInfo(){
        
        
        //reset properties
        
        userId = "";
        username = "";
        firstName = "";
        lastName = "";
        email = "";
        displayErrorMessage = "";
        
        String jsonStringObject = (String) DataControlsUtil.invokeDCNoArgumentMethod("getUserInformation");        
        //update the UI with the changed values
        if(jsonStringObject!= null){
            try {
                JSONObject jsonObject = new JSONObject(jsonStringObject);
                
                this.setUserId(jsonObject.optString("id", ""));
                this.setUsername(jsonObject.optString("username",""));
                this.setFirstName(jsonObject.optString("firstName", ""));
                this.setLastName(jsonObject.optString("lastName", ""));
                this.setEmail(jsonObject.optString("email", ""));  
            } catch (JSONException e) {
                setDisplayErrorMessage("Request succeeded but failed in parsing the JSON response in the managed bean");
            }
        }
    }


    public void setUserId(String userId) {
        String oldUserId = this.userId;
        this.userId = userId;
        propertyChangeSupport.firePropertyChange("userId", oldUserId, userId);
    }

    public String getUserId() {
        return userId;
    }

    public void setUsername(String username) {
        String oldUsername = this.username;
        this.username = username;
        propertyChangeSupport.firePropertyChange("username", oldUsername, username);
    }

    public String getUsername() {
        return username;
    }

    public void setFirstName(String firstName) {
        String oldFirstName = this.firstName;
        this.firstName = firstName;
        propertyChangeSupport.firePropertyChange("firstName", oldFirstName, firstName);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setLastName(String lastName) {
        String oldLastName = this.lastName;
        this.lastName = lastName;
        propertyChangeSupport.firePropertyChange("lastName", oldLastName, lastName);
    }

    public String getLastName() {
        return lastName;
    }

    public void setEmail(String email) {
        String oldEmail = this.email;
        this.email = email;
        propertyChangeSupport.firePropertyChange("email", oldEmail, email);
    }

    public String getEmail() {
        return email;
    }


    public void setDisplayErrorMessage(String displayErrorMessage) {
        String oldDisplayErrorMessage = this.displayErrorMessage;
        this.displayErrorMessage = displayErrorMessage;
        propertyChangeSupport.firePropertyChange("displayErrorMessage", oldDisplayErrorMessage, displayErrorMessage);
    }

    public String getDisplayErrorMessage() {
        return displayErrorMessage;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
}
