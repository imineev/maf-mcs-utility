package com.oracle.maf.sample.mcs.apis.userinfo;

import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.headers.HeaderConstants;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSResponse;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRestClient;
import com.oracle.maf.sample.mcs.shared.mbe.error.OracleMobileErrorHelper;
import com.oracle.maf.sample.mcs.shared.mbe.proxy.MBEServiceProxy;
import com.oracle.maf.sample.mcs.shared.utils.MapUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import oracle.adfmf.json.JSONException;
import oracle.adfmf.json.JSONObject;

/**
 *
 * *** INTERNAL FRAMEWORK CLASS ***
 * ***  Access through MBE only ***
 *
 * The UserInfo allows to read user information stored in the MCS security realm for the authenicated user and also
 * to update a set of allowed attributes
 *
 * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class UserInfo extends MBEServiceProxy {

    private static int STATUS_RESPONSE_OK   = 200;
    private static int STATUS_UPDATE_OK     = 204;
    
    private static final String USER_INFO_RELATIVE_URL = "/mobile/platform/users";  
    
    public UserInfo() {        
        super();
    }

    /**
     * Query information about the authenticated user stored in the MBE realm for the authenticated user. The mobile 
     * backend ID is added to the request. The method returns and instance of User. For an error that is based on an
     * exeption with no known error code in the message the error code is returned as 0 and the message is the string
     * contained in the exception
     * 
     * @return onSuccess instance of "com.oracle.maf.sample.mcs.apis.userinfo.User"  
     * @throws ServiceProxyException if MCS returns an error or if the REST call fails on the transport layer
     */
    public User getCurrentUserInformation() throws ServiceProxyException {               
        
        //get the authenticated user information (you cannot yet get the information of other users)
        String uri = USER_INFO_RELATIVE_URL +"/~";
        
        this.getMbe().getMbeConfiguration().getLogger().logFine("Get user information for URI: "+uri, this.getClass().getSimpleName(), "getCurrentUserInformation");
        
        //prepare REST call
        MCSRequest requestObject = new MCSRequest(this.getMbe().getMbeConfiguration());
        requestObject.setConnectionName(this.getMbe().getMbeConfiguration().getMafRestConnectionName());
        requestObject.setHttpMethod(com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest.HttpMethod.GET);      

        requestObject.setRequestURI(uri);
        
         HashMap<String,String> httpHeaders = new  HashMap<String,String>();
        //httpHeaders.put(HeaderConstants.ORACLE_MOBILE_BACKEND_ID,this.getMbe().getMbeConfiguration().getMobileBackendIdentifier());
        
        requestObject.setHttpHeaders(httpHeaders);
        //no payload needed for GET request
        requestObject.setPayload("");

        try {
            MCSResponse mcsResponse = MCSRestClient.sendForStringResponse(requestObject);
            this.getMbe().getMbeConfiguration().getLogger().logFine("Successfully queried user information from MCS. Response Code: "+mcsResponse.getHttpStatusCode()+" Response Message: "+mcsResponse.getMessage(), this.getClass().getSimpleName(), "getCurrentUserInformation");
            
            //handle request success
            if (mcsResponse != null && mcsResponse.getHttpStatusCode() == STATUS_RESPONSE_OK) {               
                User userObject = new User();                
                JSONObject jsonObject = new JSONObject((String) mcsResponse.getMessage());                
                populateUserObjectFromJsonObject(userObject, jsonObject);                
                return userObject;                
            } else if (mcsResponse != null){
                //if there is a mcsResponse, we pass it to the client to analyze the problem
                this.getMbe().getMbeConfiguration().getLogger().logFine("Returning onError because of MCS application error with Status Code: "+mcsResponse.getHttpStatusCode()
                                  +" and message: "+mcsResponse.getMessage(), this.getClass().getSimpleName(), "getCurrentUserInformation");
                 throw new ServiceProxyException(mcsResponse.getHttpStatusCode(), (String) mcsResponse.getMessage(), mcsResponse.getHeaders());
            }
        } catch (Exception e) {
            this.getMbe().getMbeConfiguration().getLogger().logFine("Exception thrown. Class: "+e.getClass().getSimpleName()+", Message="+e.getMessage(), this.getClass().getSimpleName(), "getCurrentUserInformation");
            this.getMbe().getMbeConfiguration().getLogger().logFine("Delegating to exception handler", this.getClass().getSimpleName(), "getCurrentUserInformation");
            handleExceptions(e,uri);                    
        }
        //should not get here
        return null;
    }

    /**
     * This method analyzes the exception for instances of AdfInvocationRuntimeException, AdfInvocation-Exception and, 
     * more broadly, AdfExceptions. If none of the two are found, it will look into the exception message for status 
     * codes known returned by the API called by the utility. If still the exception isn't identified as Oracle MCS, it 
     * will be rethrown with a status code of -1. 
     * 
     * @param e    the exception thrown by Oracle ADF. 
     * @param uri  the origin uri. This uri is used if the error message is composed based on error code findings in the exception
     *             message
     */
    private void handleExceptions(Exception e,String uri) throws ServiceProxyException {
        
         //Step 1: Is error AdfInvocationRuntimeException, AdfInvocationException or AdfException?        
        String exceptionPrimaryMessage      = e.getLocalizedMessage();
        String exceptionSecondaryMessage    = e.getCause() != null? e.getCause().getLocalizedMessage() : null;
         String combinedExceptionMessage =  "primary message:"+exceptionPrimaryMessage+(exceptionSecondaryMessage!=null?("; secondary message: "+exceptionSecondaryMessage):(""));
        
        //chances are this is the Oracle MCS erro message. If so then ths message has a JSON format. A simple JSON parsing 
        //test will show if our assumption is true. If JSONObject parsing fails then apparently the message is not the MCS
        //error message
        //{
        //  "type":".....",
        //    *  "status": <error_code>,
        //    *  "title": "<short description of the error>",
        //    *  "detail": "<long description of the error>",
        //    *  "o:ecid": "...",
        //   *  "o:errorCode": "MOBILE-<MCS error number here>",
        //   *  "o:errorPath": "<URI of the request>"
        // }
        if(exceptionSecondaryMessage!=null){
            try {
                JSONObject jsonErrorObject = new JSONObject(exceptionSecondaryMessage);
                //if we get here, then its a Oracle MCS error JSON Object. Get the 
                //status code or set it to 0 (means none is found)
                int statusCode = jsonErrorObject.optInt("status", 0);
                throw new ServiceProxyException(statusCode, exceptionSecondaryMessage);
                
            } catch (JSONException jse) {
                //if parsing fails, the this is proof enough that the error message is not 
                //an Oracle MCS message and we need to continue our analysis
                
                this.getMbe().getMbeConfiguration().getLogger().logFine("Exception message is not a Oracle MCS error JSONObject", this.getClass().getSimpleName(), "getCurrentUserInformation");
             }            
          }
        
          //continue message analysis and check for known error codes for the references MCS API
        
            this.getMbe().getMbeConfiguration().getLogger().logFine("Rest invocation failed with following message"+exceptionPrimaryMessage, this.getClass().getSimpleName(), "getCurrentUserInformation");
        
            int httpErrorCode = -1; 
            String restoredOracleMcsErrorMessage = null;
        
            /*
             *  Try to identify an MCS failure from the exception message.
             */
            if(combinedExceptionMessage.contains("400")){
                httpErrorCode = 400; 
                restoredOracleMcsErrorMessage =
                OracleMobileErrorHelper.createOracleMobileErrorJson(400, "Invalid JSON payload", "One of the following problems occurred: " +
                                                               "the user does not exist, the JSON is invalid, or a property was not found.", uri);
            }
            else if(combinedExceptionMessage.contains("401")){
                httpErrorCode = 401; 
                restoredOracleMcsErrorMessage =
                OracleMobileErrorHelper.createOracleMobileErrorJson(401, "Authorization failure", "The user is not authorized to retrieve the information for another user.",uri);                
            }
            else if(combinedExceptionMessage.contains("403")){
                httpErrorCode = 404; 
                restoredOracleMcsErrorMessage =
                OracleMobileErrorHelper.createOracleMobileErrorJson(403, "Functionality is not supported", "Functionality is not supported.",uri);  
            }
            else if(combinedExceptionMessage.contains("403")){
                httpErrorCode = 404; 
                restoredOracleMcsErrorMessage =
                OracleMobileErrorHelper.createOracleMobileErrorJson(404, "User not found", "The user with the specified ID does not exist.",uri);
            }
            
            else{
                this.getMbe().getMbeConfiguration().getLogger().logFine("Request failed with Exception: "+e.getClass().getSimpleName()+"; message: "+e.getLocalizedMessage(), this.getClass().getSimpleName(), "handleExcpetion");
                throw new ServiceProxyException(e.getLocalizedMessage(), ServiceProxyException.ERROR);
            }
            //if we get here then again its an Oracle MCS error, though one we found by inspecting the exception message
            this.getMbe().getMbeConfiguration().getLogger().logFine("Request succeeded successful but failed with MCS application error. HTTP response: "+httpErrorCode+", Error message: "+restoredOracleMcsErrorMessage, this.getClass().getSimpleName(), "handleExcpetion");
           throw new ServiceProxyException(httpErrorCode, restoredOracleMcsErrorMessage);
    }

    /**
     * Method parses the respons estring from Oracle MCS and populates a User object with the values
     * @param userObject empty User object
     * @param jsonObject String jsonObject from MCS response
     * @throws oracle.adfmf.json.JSONException if parsing fails
     */
    private void populateUserObjectFromJsonObject(User userObject,
                                                  JSONObject jsonObject) throws oracle.adfmf.json.JSONException {
        HashMap<String,Object> properties = new HashMap<String,Object>();
        Iterator keys = jsonObject.keys();
        
        while (keys.hasNext()){
            String key = (String) keys.next();
            
            //write content of the default realm into Java properties
            //and all the other properties into a HashMap
            switch (key) {
            case User.USER_ID:
                userObject.setUserId(jsonObject.getString(User.USER_ID));
               break;
            case User.USER_NAME:
               userObject.setUsername(jsonObject.getString(User.USER_NAME));
               break;
            case User.FIRST_NAME:
               userObject.setFirstName(jsonObject.getString(User.FIRST_NAME));
                   break;
                case User.LAST_NAME:
                userObject.setLastName(jsonObject.getString(User.LAST_NAME));
                   break;
                case User.EMAIL:
                   userObject.setEmail(jsonObject.getString(User.EMAIL));
                   break;                    
            default:
                //save property into MAP
                properties.put(key, jsonObject.get(key));                        
            }
        }
        
        //update user object with auxillary properties
        userObject.setProperties(properties); 
        
        this.getMbe().getMbeConfiguration().getLogger().logFine("Properties Map contains: "+MapUtils.dumpObjectProperties(properties), this.getClass().getSimpleName(), "getCurrentUserInformation");
    }
        
     /**
      * Update the user information stored for the authenticated user in the realm associated with the mobile backend. It is not
      * possible to change the user passwoed and the username. Custom properties specified in the HashMap must match to properties
      * in the realm. Note that due to missing privileges or properties - like username - that cannot be updated, this operation may
      * fail. So best to ensure users only attempt updating information they are allowed to to avoid longer cycles of trial-and-error
      *  
      * @return updated User object
      * @throws ServiceProxyException if MCS request fails with application error or REST transport layer exception  
      * @throws IllegalArgumentException if userPropertiesAndValues is null
     */
    public User updateCurrentUserInformation( HashMap<String,String> userPropertiesAndValues) throws ServiceProxyException, IllegalArgumentException{

        if(userPropertiesAndValues == null){
            throw new IllegalArgumentException("MCSCallback object and/or userPropertiesAndValues cannot be null in call to updateCurrentUserInformation method in UserInfo");
        }
        
        MCSResponse mcsResponse = null;
        
        //Open JSON object
        StringBuffer jsonString = new StringBuffer("{");
        Set keys = userPropertiesAndValues.entrySet();
        for (Object key : keys){
            
            //add delimiting comma if String buffer is not empty
            if(jsonString.length() >1){
                jsonString.append(",");
            }
            
            Object propertyValue = userPropertiesAndValues.get((String) key);
            //add "key":"value" or "key":object
            jsonString.append("\""+key+"\":"+(propertyValue instanceof String? "\""+propertyValue+"\"" : propertyValue));            
        }
        
        //close JSON object
        jsonString.append("}");
        
        String uri = USER_INFO_RELATIVE_URL +"/"+this.getMbe().getMbeConfiguration().getAuthenticatedUsername();
        
        
        //prepare REST call
        MCSRequest requestObject = new MCSRequest(this.getMbe().getMbeConfiguration());
        requestObject.setConnectionName(this.getMbe().getMbeConfiguration().getMafRestConnectionName());
        
        requestObject.setHttpMethod(com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest.HttpMethod.PUT);      

        requestObject.setRequestURI(uri);
        
         HashMap<String,String> httpHeaders = new  HashMap<String,String>();
        //httpHeaders.put(HeaderConstants.ORACLE_MOBILE_BACKEND_ID,this.getMbe().getMbeConfiguration().getMobileBackendIdentifier());        
        requestObject.setHttpHeaders(httpHeaders);
        
        requestObject.setPayload(jsonString.toString());

        try {                        
            mcsResponse = MCSRestClient.sendForStringResponse(requestObject);
            this.getMbe().getMbeConfiguration().getLogger().logFine("Success! Response Code: "+mcsResponse.getHttpStatusCode()+". message: "+mcsResponse.getMessage(), this.getClass().getSimpleName(), "updateCurrentUserInformation");
            if (mcsResponse != null && mcsResponse.getHttpStatusCode() == STATUS_UPDATE_OK) {
               
                User userObject = new User();                
                JSONObject jsonObject = new JSONObject((String) mcsResponse.getMessage());                
                populateUserObjectFromJsonObject(userObject, jsonObject);                
                return userObject;            
                
            } else {
               this.getMbe().getMbeConfiguration().getLogger().logFine("MCS application error reported back to MCS: "+mcsResponse.getHttpStatusCode()+". message: "+mcsResponse.getMessage(), this.getClass().getSimpleName(), "updateCurrentUserInformation");
              throw new ServiceProxyException(mcsResponse.getHttpStatusCode(), (String) mcsResponse.getMessage(), mcsResponse.getHeaders());
           }                                            
        }
        catch(Exception e){
           this.getMbe().getMbeConfiguration().getLogger().logFine("Exception thrown. Class: "+e.getClass().getSimpleName()+", Message="+e.getMessage(), this.getClass().getSimpleName(), "getCurrentUserInformation");
           this.getMbe().getMbeConfiguration().getLogger().logFine("Delegating to exception handler", this.getClass().getSimpleName(), "getCurrentUserInformation");
           this.handleExceptions(e, uri);
       }
        //we should not get here
        return null;
    }
    
}
