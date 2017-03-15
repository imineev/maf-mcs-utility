
package com.oracle.maf.sample.mcs.apis.custom;

import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSResponse;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRestClient;
import com.oracle.maf.sample.mcs.shared.mbe.error.OracleMobileErrorHelper;
import com.oracle.maf.sample.mcs.shared.mbe.proxy.MBEServiceProxy;

import java.util.HashMap;

/**
 * *** INTERNAL FRAMEWORK CLASS ***
 * ***  Access through MBE only ***
 *
 * Proxy class to access Mobile Cloud Service (MCS) custom APIs. It applies the mobile backend Id as well as the
 * authorizationProvider header if authentication is performed through the MAF MCS Utility. A custom API in Oracle MCS is
 * a REST interface that is exposed on an MBE. Custom APIs can be used to invoke other cloud services or on-premise
 * service for integration in MCS MBE. MCS APIs like to send notifications, database or connectors can be accessed
 * by moble client applications through Custom APIs.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class CustomAPI extends MBEServiceProxy {

    public CustomAPI() {
        super();
    }

    /**
     * Method to send a <b>synchronous</b> custom REST request to MCS server instance for requests that return a String payload.
     * <p>
     * Note that this method indicates all response codes that are not within a HTTP 2XX range as application errors in which case 
     * a ServicProxyException is thrown. The service exception contains information about the exception type (whether it is an MCS
     * application error or a exception thrown in the framework)
     * <p>
     * @param request com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest configured with all information required for a successful MCS custom API request
     * @return MCSResponse an object that contains information about the server response including status code, payload and headers
     * @throws ServiceProxyException thrown when a request to MCS fails either because of an MCS application error or an exception in the REST request transport
     * @throws IllegalArgumentException if request object argument is null. The RequestObject object exposes methods to properly configure a request
     */
    public MCSResponse sendForStringResponse(MCSRequest request) throws ServiceProxyException {
        
        if(request == null){
            throw new IllegalArgumentException("RequestObject cannot be null in call to sendReceiveString method in CustomAPI");
        }
        
        //indicate string response to be expected
        return handleSendReceiveCalls(request, false);
    }


    /**
     * Method to send <b>synchronous</b> custom REST request to MCS server instance for requests that return a byte[] response
     * <p>
     * Note that this method indicates all response codes that are not within a HTTP 2XX range as application errors in which case 
     * a ServicProxyException is thrown. The service exception contains information about the exception type (whether it is an MCS
     * application error or a exception thrown in the framework)
     *
     * @param request com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest configured with all information required for a successful MCS custom API request
     * @return MCSResponse an object that contains information about the server response including status code, payload and headers
     * @throws ServiceProxyException thrown when a request to MCS fails either because of an MCS application error or an exception in the REST request transport
     * @throws IllegalArgumentException if request object argument is null. The RequestObject object exposes methods to properly configure a request
     */
    public MCSResponse sendReceiveBytes(MCSRequest request) throws ServiceProxyException{

        if(request == null){
            throw new IllegalArgumentException("RequestObject arguments cannot be null in call to sendReceiveByte method in CustomAPI");
        }

        //indicate byte response to be returned 
        return handleSendReceiveCalls(request,true);
    }

    /**
     * Method that sends a request to receicve a byte response from Oracle MCS
     * @param request   instance of com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest that holds the request configuration
     * @param isByteResponse true indicates whether a byte resonse is expected or a String response. If string response is chosen then this framework converts the response to string
     * @return MCSResponse that contains information about the MCS response including the response paylaod, http status and the HTTP header iformation
     * @throws ServiceProxyException thrown when a request to MCS fails either because of an MCS application error or an exception in the REST request transport
     */
    private MCSResponse handleSendReceiveCalls(MCSRequest request, boolean isByteResponse) throws ServiceProxyException{
        try {

            MCSResponse mcsResponse = null;

            //call for String or byte response
            if (isByteResponse) {
                mcsResponse = MCSRestClient.sendForByteResponse(request);
            } else {
                mcsResponse = MCSRestClient.sendForStringResponse(request);
            }


            if (mcsResponse != null && mcsResponse.getHttpStatusCode() > 199 && mcsResponse.getHttpStatusCode() < 210) {
                this.getMbe().getMbeConfiguration().getLogger().logFine("REST call to "+mcsResponse.getOriginalRequestUrl()+" succeeded with status :" +mcsResponse.getHttpStatusCode(), this.getClass().getSimpleName(), "handleSendReceiveCalls");
                return mcsResponse;

            }
            else {
                this.getMbe().getMbeConfiguration().getLogger().logFine("REST call to "+mcsResponse.getOriginalRequestUrl()+" succeeded with status :" +mcsResponse.getHttpStatusCode()+". However, the returned status code indicates an application error", this.getClass().getSimpleName(), "handleSendReceiveCalls");               
                throw new ServiceProxyException(mcsResponse.getHttpStatusCode(), (String) mcsResponse.getMessage(), mcsResponse.getHeaders());
            }

        } catch (Exception e) {
            
            //Exceptions might be false positives. Check if request succeeded 
            
            String exceptionPrimaryMessage      = e.getLocalizedMessage();
            String exceptionSecondaryMessage    = e.getCause() != null? e.getCause().getLocalizedMessage() : null;
             String combinedExceptionMessage    = "primary message:"+exceptionPrimaryMessage+(exceptionSecondaryMessage!=null?("; secondary message: "+exceptionSecondaryMessage):(""));
            
            this.getMbe().getMbeConfiguration().getLogger().logError("REST call to "+request.getRequestURI() +" resulted in an exception :" +combinedExceptionMessage, this.getClass().getSimpleName(), "handleSendReceiveCalls");
            this.getMbe().getMbeConfiguration().getLogger().logError("Checking exception message for HTTP code", this.getClass().getSimpleName(), "handleSendReceiveCalls");
                       
            int statusCode = 0;            
            boolean requestSuccess = false;
                      
           /* *** NOTE ***
            * the code below can be simplified in MAF version 2.1.3+ using a new MAF API introduced in that release. This 
            * change will be implemented in a later version of MAF MCS Utility and then restrict support to MAF versions 
            * 2.1.3+ 
            */
            
           //treat status code in the 200 range as success            
            //201 Created
            if(exceptionPrimaryMessage.contains("201")){
                statusCode = 201;
                requestSuccess = true;
            }
            //202 Accepted
            else if(exceptionPrimaryMessage.contains("202")){
                statusCode = 202;
                requestSuccess = true;
            }
            //less commonly used 
            else if(exceptionPrimaryMessage.contains("203")){
                statusCode = 203;
                requestSuccess = true;
            }
            //204 No Content
            else if(exceptionPrimaryMessage.contains("204")){
                statusCode = 204;
                requestSuccess = true;
            }
            else if(exceptionPrimaryMessage.contains("205")){
                statusCode = 205;
                requestSuccess = true;
            }
            //do the same for common error codes
            else if(exceptionPrimaryMessage.contains("400")){
                statusCode = 400;
                requestSuccess = false;
            }
            else if(exceptionPrimaryMessage.contains("401")){
                statusCode = 401;
                requestSuccess = false;
            }
            else if(exceptionPrimaryMessage.contains("403")){
                statusCode = 403;
                requestSuccess = false;
            }
            else if(exceptionPrimaryMessage.contains("404")){
                statusCode = 404;
                requestSuccess = false;
            }
            //405 Method Not Allowed
            else if(exceptionPrimaryMessage.contains("405")){
                statusCode = 405;
                requestSuccess = false;
            }
            //406 Not Acceptable
            else if(exceptionPrimaryMessage.contains("406")){
                statusCode = 406;
                requestSuccess = false;
            }
            else if(exceptionPrimaryMessage.contains("409")){
                statusCode = 409;
                requestSuccess = false;
            }
            //411 Length Required
            else if(exceptionPrimaryMessage.contains("411")){
                statusCode = 411;
                requestSuccess = false;
            }
            //412 Precondition Failed
            else if(exceptionPrimaryMessage.contains("412")){
               statusCode = 412;
               requestSuccess = false;
            }
            // 413 Request Entity Too Large
            else if(exceptionPrimaryMessage.contains("413")){
               statusCode = 413;
               requestSuccess = false;
            }
           // 414 Request-URI Too Long
           else if(exceptionPrimaryMessage.contains("414")){
              statusCode = 414;
              requestSuccess = false;
           }
            //415 Unsupported Media Type
            else if(exceptionPrimaryMessage.contains("415")){
              statusCode = 415;
              requestSuccess = false;
           }
           //416 Requested Range Not Satisfiable
           else if(exceptionPrimaryMessage.contains("416")){
             statusCode = 416;
             requestSuccess = false;
           } 
            
            //handle the  successful request. Compose the REST response
            if(requestSuccess){                
                MCSResponse response = new MCSResponse(request.getMbeConfig());
                response.setHttpStatusCode(statusCode);
                response.setOriginalRequestUrl(request.getRequestURI());
                response.setHeaders(new HashMap<String,String>());
                
                //Construct the response message if the secondary message indicates a successful MCS REST request. This 
                //response does not contain headers
                if(exceptionSecondaryMessage!=null) {                
                    //attempt to construct a reasonable success response                    
                    response.setMessage(exceptionSecondaryMessage);
                    return response;                    
                }
                //there was no message from the server. Create empty response message
                else{
                    //successful request but no secondary error message with the detailed response. Return empty response
                    response.setMessage("");
                    
                    this.getMbe().getMbeConfiguration().getLogger().logFine("Custom API call succeeded but transport layer throwed Exception: "+combinedExceptionMessage, this.getClass().getSimpleName(), "handleSendReceiveCalls");
                    this.getMbe().getMbeConfiguration().getLogger().logFine("No response message returned.", this.getClass().getSimpleName(), "handleSendReceiveCalls");
                    return response; 
                    
                }
            }
            //application error or transport layer exception
            else{
                //determine MCS application error
                if(exceptionSecondaryMessage!=null){                    
                   this.getMbe().getMbeConfiguration().getLogger().logFine("MCS Custom API call failed MCS with application error: "+combinedExceptionMessage, this.getClass().getSimpleName(), "handleSendReceiveCalls");
                   throw new ServiceProxyException(statusCode,OracleMobileErrorHelper.createOracleMobileErrorJson(statusCode,"Unexpected error", e.getClass().toString()+" :: "+exceptionPrimaryMessage, request.getRequestURI()));                    
                }
                else{
                    this.getMbe().getMbeConfiguration().getLogger().logFine("Excpetion thrown on transport layer: "+combinedExceptionMessage, this.getClass().getSimpleName(), "handleSendReceiveCalls");
                    this.getMbe().getMbeConfiguration().getLogger().logFine("Not able to recover from this. Throwing exception.", this.getClass().getSimpleName(), "handleSendReceiveCalls");
                    throw new ServiceProxyException(e.getLocalizedMessage(), ServiceProxyException.ERROR);
                }
              
             }
           //end catch exception 
       }
  }
}
