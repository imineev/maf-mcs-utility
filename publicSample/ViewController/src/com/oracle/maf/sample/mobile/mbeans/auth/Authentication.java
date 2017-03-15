package com.oracle.maf.sample.mobile.mbeans.auth;

import com.oracle.maf.sample.mobile.logger.McsSampleLogger;
import com.oracle.maf.sample.mobile.mbeans.utils.DataControlsUtil;

import java.util.ArrayList;

import oracle.adfmf.framework.exception.AdfException;
import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;


/**
 *
 * The sample application authenticates to Oracle MCS usig basic authentication. Authentication could be performed on the
 * MAF feature level (declarative authentication with a login provider defined and associated in the maf-application.xml
 * file), or programmatically using the user management API in Oracle MCS through the MAF MCS Utility.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class Authentication {
    
    
    private McsSampleLogger logger = new McsSampleLogger();
    
    
    private boolean mbeAuthenticated = false;
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    
    private String username         = null;
    private String password         = null;
    private String loginMessage     = "";
    
    private boolean anonymousLogin = false;

    public Authentication() {
        super();
    }
    
    /**
     * Handle username based or anonymous login
     * @return
     */
    public String performAuthentication(){
        if(this.isAnonymousLogin()){
            return this.anonymousLogin();
        }else{
            return this.login();
        }
    }
    
    /**
     * logs users out from MBE. It does not remove the MBE instance from the MCS Utility MBE Manager
     * @return
     */
    public String logoutFromMBE(){        
        DataControlsUtil.invokeDCNoArgumentMethod("logoutFromMBE");
        mbeAuthenticated = false;        
        
        return "logout";
    }
    
    /**
     * Though the MAF MCS Utility can be references from a managed bean as well, a personal preferences is to 
     * always work through the data control if a secific function is already exposed on the data control. 
     * @return String, "success" upon login success, which is the navigation case to the demo start page. 
     */
    private String login(){
        if(this.username == null || this.username.isEmpty()
           || this.password == null || this.password.isEmpty()){
            throw new AdfException("Username and/or password cannot be null or empty",AdfException.ERROR);
        }        
        logger.logFine("User login attempt for user: "+username, this.getClass().getSimpleName(), "login");
                
        ArrayList<String> parameterNames = new ArrayList<String>();
        parameterNames.add("username");
        parameterNames.add("password");
        ArrayList<Object> parameterValues = new ArrayList<Object>();
        parameterValues.add(this.username);
        parameterValues.add(this.password);
        ArrayList<Class> parameterTypes = new ArrayList<Class>();
        parameterTypes.add(String.class);
        parameterTypes.add(String.class);
        
        Boolean authSuccess =  (Boolean) DataControlsUtil.invokeOnDataControl("authenticateUser", parameterNames, parameterValues, parameterTypes);
        mbeAuthenticated = authSuccess.booleanValue();        
            
        if(mbeAuthenticated){
            //navigate to demo page
            this.setPassword(null);
            return "success";
        }
        return null;
    }
    
    private String anonymousLogin(){
        logger.logFine("Anonymous login attempt for user: "+username, this.getClass().getSimpleName(), "login");
        Boolean loginOutcome =  (Boolean) DataControlsUtil.invokeDCNoArgumentMethod("anonymousLogin");
        
        this.setMbeAuthenticated(loginOutcome.booleanValue());
        
        if(this.isMbeAuthenticated() == true){
            //navigate to demo page
            this.setPassword(null);
            return "success";
        }
        return null;
    }
    
    public void setUsername(String username) {        
        String oldUsername = this.username;
        this.username = username;
        propertyChangeSupport.firePropertyChange("username", oldUsername, username);
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String password) {
        String oldPassword = this.password;
        this.password = password;
        propertyChangeSupport.firePropertyChange("password", oldPassword, password);
    }

    public String getPassword() {
        return password;
    }
    
    public void setMbeAuthenticated(boolean mbeAuthenticated) {
        boolean oldMbeAuthenticated = this.mbeAuthenticated;
        this.mbeAuthenticated = mbeAuthenticated;
        propertyChangeSupport.firePropertyChange("mbeAuthenticated", oldMbeAuthenticated, mbeAuthenticated);
    }

    public boolean isMbeAuthenticated() {
        return mbeAuthenticated;
    }


    public void setLoginMessage(String loginMessage) {
        String oldLoginMessage = this.loginMessage;
        this.loginMessage = loginMessage;
        propertyChangeSupport.firePropertyChange("loginMessage", oldLoginMessage, loginMessage);
    }

    public String getLoginMessage() {
        return loginMessage;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    public void setAnonymousLogin(boolean anonymousLogin) {
        boolean oldAnonymousLogin = this.anonymousLogin;
        this.anonymousLogin = anonymousLogin;
        propertyChangeSupport.firePropertyChange("anonymousLogin", oldAnonymousLogin, anonymousLogin);
    }

    public boolean isAnonymousLogin() {
        return anonymousLogin;
    }

}
