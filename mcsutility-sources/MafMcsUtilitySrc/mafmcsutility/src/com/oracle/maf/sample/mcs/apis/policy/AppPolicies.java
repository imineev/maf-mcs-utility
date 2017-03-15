package com.oracle.maf.sample.mcs.apis.policy;

import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.headers.HeaderConstants;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSResponse;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRestClient;
import com.oracle.maf.sample.mcs.shared.mbe.proxy.MBEServiceProxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import oracle.adfmf.framework.api.JSONBeanSerializationHelper;
import oracle.adfmf.json.JSONObject;

/**
 *
 * *** INTERNAL FRAMEWORK CLASS ***
 * ***  Access through MBE only ***
 *
 * The AppPolicy class allows client applications built with MAF to read server side defined key/value pairs saved for
 * the MBE. The application policies are properties that mobile backend developers use e.g. to provide title strings,
 * license agreements, application version information and similar.
 *
 * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */

public class AppPolicies extends MBEServiceProxy {
    
    private static int STATUS_RESPONSE_OK   = 200;
    
    public AppPolicies() {
        super();
    }
    
    
    /**
     * Method reads the Mobile Backend application specific policy.
     * 
     * Using the following call, you can retrieve all of the app policies associated with a mobile backend.
     * 
     * GET {BaseURL}/mobile/platform/appconfig/client
     * 
     * The response body is a JSON object containing all of the app policies configured for that mobile backend. 
     * For example, the server response might look something like this:
     * {
     *    "AppWelcomeMessage":"Hello",
     *    "BackgroundImage":"/mobile/platform/storage/collections/appObjects/objects/bgImage42"
     *  }
     *  
     * @return HashMap a map containing key-value pairs with application configuration properties defined on the mobile
     * backend. 
     * @throws ServiceProxyException for MCS error and transport layer exceptions
     */
    public HashMap<String,Object> getAppPoliciesMap() throws ServiceProxyException{
        
        String uri = "/mobile/platform/appconfig/client";
        
        this.getMbe().getMbeConfiguration().getLogger().logFine("Reading application policies from MCS backend: "+uri, this.getClass().getSimpleName(), "getAppPoliciesMap");
        
        //prepare REST call
        MCSRequest requestObject = new MCSRequest(this.getMbe().getMbeConfiguration());
        requestObject.setConnectionName(this.getMbe().getMbeConfiguration().getMafRestConnectionName());
        requestObject.setHttpMethod(MCSRequest.HttpMethod.GET);      

        requestObject.setRequestURI(uri);
        
         HashMap<String,String> httpHeaders = new  HashMap<String,String>();
         //httpHeaders.put(HeaderConstants.ORACLE_MOBILE_BACKEND_ID,this.getMbe().getMbeConfiguration().getMobileBackendIdentifier());
        
        requestObject.setHttpHeaders(httpHeaders);
        //no payload needed for GET request
        requestObject.setPayload("");

        try {
            MCSResponse mcsResponse = MCSRestClient.sendForStringResponse(requestObject);
            this.getMbe().getMbeConfiguration().getLogger().logFine("Successfully queried application policies from MCS backend. Response Code: "+mcsResponse.getHttpStatusCode()+" Response Message: "+mcsResponse.getMessage(), this.getClass().getSimpleName(), "getAppPoliciesMap");
            
            //handle request success
            if (mcsResponse != null && mcsResponse.getHttpStatusCode() == STATUS_RESPONSE_OK) { 
                
                //get the returned JSON object and parse it into a HashMap. 
                JSONObject jsonObject = new JSONObject((String) mcsResponse.getMessage());  
                //The JSON Object contains String, Number and Boolean values. Use MAF helper JSONBeanSerializationHelper
                //to parse the JSON object into a HashMap
                HashMap<String, Object> applicationPoliciesMap = (HashMap<String, Object>) JSONBeanSerializationHelper.fromJSON(HashMap.class, jsonObject);
                return applicationPoliciesMap;

            } else if (mcsResponse != null){
                //if there is a mcsResponse, we pass it to the client to analyze the problem
                this.getMbe().getMbeConfiguration().getLogger().logFine("MCS application returns error with status code: "+mcsResponse.getHttpStatusCode()
                                                     +" and message: "+mcsResponse.getMessage(), this.getClass().getSimpleName(), "getAppPoliciesMap");
                 throw new ServiceProxyException(mcsResponse.getHttpStatusCode(), (String) mcsResponse.getMessage(), mcsResponse.getHeaders());
            }
        } catch (Exception e) {
            this.getMbe().getMbeConfiguration().getLogger().logFine("Exception thrown. Class: "+e.getClass().getSimpleName()+", Message="+e.getMessage(), this.getClass().getSimpleName(), "getAppPoliciesMap");
            this.getMbe().getMbeConfiguration().getLogger().logFine("Delegating to exception handler", this.getClass().getSimpleName(), "getAppPoliciesMap");
            throw new ServiceProxyException(e.getMessage(),ServiceProxyException.ERROR);         
        }
        
        //the call should never get here
        return new HashMap<String,Object>();
    }
    
    /**
     * Method that queries the MCS MBE policies through HashMap<String,Object> getAppPoliciesMap(), saving the response in 
     * a List of Policy objects. A Policy object has a name and value property
     * 
     * @return ArrayList<Policy>
     * @throws ServiceProxyException
     */
    public ArrayList<Policy> getAppPoliciesList() throws ServiceProxyException{
        
        this.getMbe().getMbeConfiguration().getLogger().logFine("Calling getAppPoliciesMap method", this.getClass().getSimpleName(), "getAppPoliciesList");
        
        HashMap<String,Object> policiesMap = this.getAppPoliciesMap();
        
        this.getMbe().getMbeConfiguration().getLogger().logFine("Policy map retrieved with length: "+policiesMap.size(), this.getClass().getSimpleName(), "getAppPoliciesList");
        
        ArrayList<Policy> policyList = new ArrayList<Policy>();
        
        Set keys = policiesMap.keySet();
        
        this.getMbe().getMbeConfiguration().getLogger().logFine("Creating policy list using entities", this.getClass().getSimpleName(), "getAppPoliciesList");
        
        for(Object key : keys){
            Policy p = new Policy();
            p.setPolicyName((String) key);
            p.setPolicyValue(policiesMap.get(key));
            policyList.add(p);
        }
        
        return policyList;
    }
        
}
