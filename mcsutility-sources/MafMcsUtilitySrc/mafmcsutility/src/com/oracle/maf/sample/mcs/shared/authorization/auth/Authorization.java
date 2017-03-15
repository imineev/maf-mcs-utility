package com.oracle.maf.sample.mcs.shared.authorization.auth;

import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;

import java.util.HashMap;


/**
 * APIs exposed on a Mobile Backend in Oracle MCS are protected by default. Access to the APIs is for username/password authenticated
 * users aand for anonymous authenticated users. Note that not all APIs allow anoamous access. Anonymous authenticated access exists
 * for example for the Analytics API and may exist for custom APIs. All APIs however require authentication (anonymous or use based)
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public interface Authorization{
    
    /**
     * @param username  A valid MCS username / password pair. The username must be within a security realm known by the MCS MBE
     * @param password  The user password is provided by the Shared Identity Management (SIM) in Oracle cloud and sent to mobile users after registration
     * @throws IllegalArgumentException Method throws an exception if username or password values are null or empty
     * @throws ServiceProxyException If an error occured in the communication between a mobile client and MCS, then a ServiceProxyException is thrown
     */
    public void authenticate(String username, String password) throws IllegalArgumentException,ServiceProxyException;
    
    /**
     * Uses the MBE anonymous key to perform client authentication
     * @throws ServiceProxyException If an error occured in the communication between a mobile client and MCS, then a ServiceProxyException is thrown
     */
    public void authenticateAsAnonymous() throws ServiceProxyException;
    
    /** 
     * @throws ServiceProxyException ServiceProxyException If an error occured in the communication between a mobile client and MCS, then a ServiceProxyException is thrown
     */
    public void logout() throws ServiceProxyException;
    
    /**
     * Method to access the Authorization header created for the MBE instance
     * @return HashMap<String,String> with Authorization key entry
     */
    public HashMap<String,String> getHTTPAuthorizationHeader();
    
    /**
     * @return Authenticated username
     * @throws ServiceProxyException
     */
    public String getUsername();
    
    /**
     * Return information about whether authentication is performed with anonmyous credentials
     * @return true if authentication was anonymous, false otherise
     */
    public boolean isAnonymousAuthentication();
}
