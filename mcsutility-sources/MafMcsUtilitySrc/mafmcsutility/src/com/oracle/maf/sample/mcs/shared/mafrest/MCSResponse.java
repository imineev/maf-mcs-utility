package com.oracle.maf.sample.mcs.shared.mafrest;

import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;

import com.oracle.maf.sample.mcs.shared.mbe.error.OracleMobileError;

import java.util.HashMap;

import oracle.adfmf.json.JSONException;
import oracle.adfmf.json.JSONObject;


/**
 *
 * The MCSResponse wraps the HTTP message response returned from Oracle MCS. This class provides a convenient method that
 * returns the MCS errror message as an easy to parse Java object. In addition this object contains the original request URL
 * and the MBE configuration object used to send the request.
 *
 * @author Frank Nimphius 
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public final class MCSResponse {
          
    private HashMap<String,String>  headers =   new HashMap<String,String>();
    /**
     * The response message returned for a request. The message format depends on the type of request and can be String (for
     * JSON object responses e.g. in custom API calls), binary[] for requests that return binary files from Storage
     */
    private Object                  message =   null;
    /**
     * The HTTP stats code returned by MCS or the network in case of application or network transport failure
     */
    private int                     httpStatusCode =    0;
    /**
     * A hint about the format of the response like application/json
     */
    private String                  mimeType = "";
    private String                  originalRequestUrl =        null;
    private MBEConfiguration        mbeConfig =         null;
    
    private OracleMobileError oracleErrorMessage =    null;
    
    /**
     * Adding an instance of MBEConfiguration to the Response allows the REST client to log messages on behalf of a 
     * specific MBE instance, which allows better analyzes of log entries in case of an error when multiple backends are 
     * used with this MCS MAF Utility. Generic logger is used in case where mbeConfig argument value is passed as null.
     * @param mbeConfig
     */
    public MCSResponse(MBEConfiguration mbeConfig) {
        super();
        this.mbeConfig = mbeConfig;
    }


    /**
     * *** INTERNAL METHOD ***
     * @param responseHeaders
     */
    public void setHeaders(HashMap<String,String> responseHeaders) {
        this.headers = responseHeaders;
    }

    /**
     * Headers of the MCS message added as key/value pairs in the Map
     * @return HashMap
     */
    public HashMap<String,String> getHeaders() {
        return headers;
    }

    /**
     * *** INTERNAL METHOD ***
     * The response message from returned for a request. This can be message or error related information
     */
    public void setMessage(Object response) {
        this.message = response;
    }

    /**
     * The response message returned for a request. The message format depends on the type of request and can be String (for
     * JSON object responses e.g. in custom API calls), binary[] for requests that return binary files from Storage
     *
     * Note that if the error message is set by MCS, then this method returns the JSON string, wheras getOracleErrorMessage()
     * returns an OracleError object the message is parsed into for easier access to the detail information it contains
     * 
     * @return Object  JSON or binary array message pyload
     */
    public Object getMessage() {
        return message;
    }


    /**
     * *** INTERNAL METHOD ***
     * 
     * @param responseStatus
     */
    public void setHttpStatusCode(int responseStatus) {
        this.httpStatusCode = responseStatus;
    }

    /**
     * The HTTP status code like 200, 201 ..., 400, 401, ... returned from the server. HTTP status codes are used
     * in Oracle MCS to report application errors along with an error message.
     * @return
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * *** INTERNAL METHOD ***
     * 
     * @param responseContent
     */
    public void setMimeType(String responseContent) {
        this.mimeType = responseContent;
    }

    public String getMimeType() {
        return mimeType;
    }

    /**
     * *** INTERNAL METHOD ***
     * @param requestUrl
     */
    public void setOriginalRequestUrl(String requestUrl) {
        this.originalRequestUrl = requestUrl;
    }

    /**
     * In MAF developers set the REST base URL through a named REST connection. For debugging and logging purposes this 
     * method contains the complete original request URL that consists of the base URL and the REST URI passed to the 
     * REST http call. 
     * 
     * @return
     */
    public String getOriginalRequestUrl() {
        return originalRequestUrl;
    }

    /**
     * Translates the JSON error message returned from MCS into a Java Object for developers to create
     * educated logging and exception messages. This method does not check if there are errors. Its up
     * to the application developer to interpret the returned status code to then decide whether or not
     * an error message is available
     *
     * @return OracleError. The individual error attribute might be empty if no information is found in the
     * error message
     */
    public OracleMobileError getOracleErrorMessage() {
            oracleErrorMessage = new OracleMobileError();
            //MCS error messages are added as a String in the REST response    
            if(message != null && (message instanceof String )){
                
                JSONObject jsonResponse;
                try {
                    jsonResponse = new JSONObject((String) message);
                
                    oracleErrorMessage.setType(jsonResponse.getString(OracleMobileError.MCS_ERROR_TYPE));
                    oracleErrorMessage.setStatus(jsonResponse.getInt(OracleMobileError.MCS_ERROR_STATUS));
                    oracleErrorMessage.setDetail(jsonResponse.getString(OracleMobileError.MCS_ERROR_DETAIL));
                    oracleErrorMessage.setTitle(jsonResponse.getString(OracleMobileError.MCS_ERROR_TITLE));
                    oracleErrorMessage.setOracleErrorCode(jsonResponse.getString(OracleMobileError.MCS_ERROR_ORACLE_ERROR_CODE));
                    oracleErrorMessage.setOracleErrorPath(jsonResponse.getString(OracleMobileError.MCS_ERROR_ORACLE_ERROR_PATH));
                    oracleErrorMessage.setOracleEcid(jsonResponse.getString(OracleMobileError.MCS_ERROR_ORACLE_ECID));
                    
                } catch (JSONException e) {
                    
                    if(this.mbeConfig != null){
                        mbeConfig.getLogger().logFine("JSON parsing failed for error payload: "+message, this.getClass().getSimpleName(),
                                       "getRestErrorInformation");
                    }
                    else{
                     //fall-back to generic (non mbe-specific) logger
                     mbeConfig.getLogger().logFine("JSON parsing failed for error payload: "+message, this.getClass().getSimpleName(),
                                       "getRestErrorInformation");
                    }
                }
            }
        return oracleErrorMessage;
    }
    
    /**
     * The MBE configuration object that contains the configuration for which the originate request was issues. This 
     * method is provided for debugging and logging use
     * 
     * @return
     */
    public MBEConfiguration getMbeConfig() {
        return mbeConfig;
    }
}
