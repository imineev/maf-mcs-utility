package com.oracle.maf.sample.mcs.apis.userinfo;

import java.util.Iterator;
import java.util.Map;

import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;

/**
 *
 * User Object that holds the Mobile Cloud Service (MCS) user information for the authenticated
 * user. The authentication context is not per Mobile Backend (MBE) but the cloud service as a
 * whole.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class User {
    
    public static final String USER_ID = "id";
    public static final String USER_NAME = "username";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String EMAIL = "email";
    

    private String userId = null;
    private String username = null;
    private String firstName = null;
    private String lastName = null;
    private String email = null;
    private Map<String,Object> properties;
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);


    public User() {
        super();
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

    public void setProperties(Map<String, Object> properties) {
        Map<String, Object> oldProperties = this.properties;
        this.properties = properties;
        propertyChangeSupport.firePropertyChange("properties", oldProperties, properties);
    }


    /**
     * HashMap that holds all properties for which the User obejct does not have setter/getter parameters (Java
     * properties). Developers should know about the additional realm attribute names to directly access their
     * information directly 
     * @return Map with Strin/Object pairs
     */
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    /**
     * Composes a JSON String representation of this object. The minimum set of information in the JSON string are
     * "id", "username", "email", "firstName", "lastName".
     * Other properties can be changed for a realm with custom properties like address, age, birthday, or whatever a 
     * mobile application needs to know about the authenticated user
     * 
     * @return JSON String represnetatio of this object 
     */
    public String toJSONString(){
        StringBuffer sb = new StringBuffer("{");
        sb.append("\"id\":\""+this.getUserId()+"\"");
        if(this.getUsername()!=null) sb.append(",\"username\":\""+this.getUsername()+"\"");
        if(this.getFirstName()!=null) sb.append(",\"firstName\":\""+this.getFirstName()+"\"");
        if(this.getLastName()!=null) sb.append(",\"lastName\":\""+this.getLastName()+"\"");
        if(this.getEmail()!=null) sb.append(",\"email\":\""+this.getEmail()+"\"");       
        
        //check if there are additional properties
        Map<String,Object> properties = this.getProperties();
        
        if(properties != null){
            Iterator keyIter = properties.keySet().iterator();
            
            while(keyIter.hasNext()){
                String keyName = (String) keyIter.next();
                Object value = properties.get(keyName);
                //treat JSO array links different
                if(value!=null && (keyName.equalsIgnoreCase("links") || !(value instanceof String))){
                    sb.append(",\""+keyName+"\":"+value);     
                }
                else if(value!=null){
                    sb.append(",\""+keyName+"\":\""+value+"\"");     
                }
            }
        }
        sb.append("}");
      
      return sb.toString();  
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
}
