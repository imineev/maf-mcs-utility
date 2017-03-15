package com.oracle.maf.sample.mcs.shared.mbe.error;

import oracle.adfmf.json.JSONException;
import oracle.adfmf.json.JSONObject;


/**
 * Helper class that creates an OracleMobileError entity object from an Oracle error message. It is also used by the
 * MAF MCS Utility class to reconstruct MCS error messages based on the returned http status code. In reconstructing
 * messages, the utility creates the JSONObject returned by Oracle MCS in response to application errors.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 *
 */
public class OracleMobileErrorHelper {
    private OracleMobileErrorHelper() {
    }
    
    
    /**
     * *** INTERNAL USE ONLY ***
     * 
     * Method that parses information into an Oracle JSON error message
     * @param errorCode int value e.g. 400, 401 etc.
     * @param title Short description
     * @param detail Long description
     * @param errorPath URI that produced the error
     * @return String that represents the JSON string
     */
    public static final String createOracleMobileErrorJson(int errorCode, String title, String detail, String errorPath){
                                                                                      
       String message = "{\"type\": \"http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.1\"," +
                         "\"status\": \""+errorCode+"\",\"title\":\""+title+"\",\"detail\":\""+detail+"\"," +
                         "\"o:ecid\": \" \",\"o:errorCode\":\" \",\"o:errorPath\":\""+errorPath+"\"}";            
       return message;
    }
    
    
    /**
     * Creates a Java object representation of the Oracle Mobile error message. The JSON error message format used with
     * Oracle MCS mobile errors is shown here
     * <pre>
     * {
     * "type":"?",
     * "status": <error_code>,
     * "title": "<short description of the error>",
     * "detail": "<long description of the error>",
     * "o:ecid": "<?>",
     * "o:httpResponseCode": "MOBILE-<number here>",
     * "o:errorPath": "<URI of the request>"
     * }
     * </pre>
     * @param jsonErorMessage
     * @return OracleMobileError object representing the JSON string error message passed as the argument
     * throws JSONException if the string cannot be parsed as a JSON object
     */
    public static final OracleMobileError getMobileErrorObject (String jsonErorMessage) throws JSONException{
        OracleMobileError oracleMobileError = new OracleMobileError();
        JSONObject jsonObject = new JSONObject(jsonErorMessage);
        oracleMobileError.setType(jsonObject.optString("type", null));
        oracleMobileError.setStatus(jsonObject.optInt("status", 0));
        oracleMobileError.setTitle(jsonObject.optString("title", null));
        oracleMobileError.setDetail(jsonObject.optString("detail", null));
        oracleMobileError.setOracleEcid(jsonObject.optString("o:ecid", null));
        oracleMobileError.setOracleErrorCode(jsonObject.optString("o:errorCode", null));
        oracleMobileError.setOracleErrorPath(jsonObject.optString("o:errorPath", null));
        
        return oracleMobileError;
    }
}
