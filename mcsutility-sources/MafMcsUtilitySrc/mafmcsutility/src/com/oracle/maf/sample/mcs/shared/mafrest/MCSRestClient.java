package com.oracle.maf.sample.mcs.shared.mafrest;

import com.oracle.maf.sample.mcs.shared.headers.HeaderConstants;
import com.oracle.maf.sample.mcs.shared.log.LibraryLogger;
import com.oracle.maf.sample.mcs.shared.log.UtilLogger;
import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;
import com.oracle.maf.sample.mcs.shared.mbe.constants.MBEConstants;
import com.oracle.maf.sample.mcs.shared.utils.MapUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.microedition.io.HttpConnection;

import oracle.adfmf.dc.ws.rest.RestServiceAdapter;
import oracle.adfmf.framework.api.Model;
import oracle.adfmf.util.Utility;

import oracle.mobile.cloud.SyncHttpConnection;


/**
 *  *** INTERNAL USE ONLY  ***
 *
 * Invokes the REST service using the MAF REST Service Adapter
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.erved.
 */
public final class MCSRestClient {
    
    
    private MCSRestClient() {
    }

    /**
     *
     * Sends the REST request to the server for String and byte[] payloads. The response type is expected to be
     * String. This method therefore attempts to convert the REST response into String
     *
     * @param request MCSRequest object with the REST call configuration
     * @return MCSResponse with header information and message body
     * @throws Exception Exceptions thrown upon invoking the REST call (could be anything)
     */
    public static MCSResponse sendForStringResponse(MCSRequest request) throws Exception {
        MCSResponse mcsResponse = sendForByteResponse(request);
        if(mcsResponse != null && mcsResponse.getMessage() != null){
            if(mcsResponse.getMessage() instanceof byte[]){
                mcsResponse.setMessage(Utility.bytesToString((byte[]) mcsResponse.getMessage()));   
            }
        }
        return mcsResponse;
    }

    /**
     * Sends the REST request to the server for String and byte array payloads. The response type is expected to be
     * byte[]
     *
     * @param request MCSRequest object with the REST call configuration
     * @return MCSResponse with header information and message body
     * @throws Exception Exceptions thrown upon invoking the REST call (could be anything)
     */
    public static MCSResponse sendForByteResponse(MCSRequest request) throws Exception {

        RestServiceAdapter restServiceAdapter = prepareRestServiceAdapter(request);
        
        //log all about this request except payload as it might be too large for logging
        request.getMbeConfig().getLogger().logFine("Header key/value pairs in MCSRestClient call: "+MapUtils.dumpObjectProperties(restServiceAdapter.getRequestProperties()), "RestClient.java", "sendForByteResponse");     
        request.getMbeConfig().getLogger().logFine("Request Method in MCSRestClient call: "+restServiceAdapter.getRequestType(), "RestClient.java", "sendForByteResponse");
        request.getMbeConfig().getLogger().logFine("Request URL in MCSRestClient call: "+restServiceAdapter.getConnectionEndPoint(restServiceAdapter.getConnectionName()) +restServiceAdapter.getRequestURI(), "RestClient.java", "sendForByteResponse");
        
        MCSResponse response = new MCSResponse(request.getMbeConfig());

        //response can be either String or byte[]
        if(request.getPayload() == null || request.getPayload() instanceof String){
            
            request.getMbeConfig().getLogger().logFine("Request-payload instance of String or NULL", "RestClient.java", "sendForByteResponse");     
            
            byte[] responseRaw = restServiceAdapter.sendReceive(request.getPayload() == null? "" : (String) request.getPayload());
                        
            response.setOriginalRequestUrl(restServiceAdapter.getConnectionEndPoint(request.getConnectionName()) +
                                          restServiceAdapter.getRequestURI());
            
            response.setMessage(responseRaw);            
            response.setMimeType(restServiceAdapter.getResponseContentType());
            response.setHttpStatusCode(restServiceAdapter.getResponseStatus());
            response.setHeaders(restServiceAdapter.getResponseHeaders());
        }
        
        //handle binary payload
        else if(request.getPayload() != null && request.getPayload() instanceof byte[]){
            request.getMbeConfig().getLogger().logFine("Request-payload instance of byte[]", "RestClient.java", "sendForByteResponse");                                
            response = handleBinaryArgumentRequest(restServiceAdapter,request);
            
            //add the full request URL to the response object for logging purpose            
            response.setOriginalRequestUrl(restServiceAdapter.getConnectionEndPoint(request.getConnectionName()) +
                                          restServiceAdapter.getRequestURI());
        }
        else{
            request.getMbeConfig().getLogger().logFine("Request-payload was neither byte[] nor String type. No REST service request was sent", "RestClient.java", "sendForByteResponse");
        }
        return response;        
    }
    
    /**
     * Method that handles the upload of binary message. The RestServiceAdapter by design handles String payloads but doesn't do byte arrays. This helper method provides
     * this functionality, still using the RestServiceAdapter in MAF to handle the request configuration. However, the request itself is issued directly through the HTTP
     * connection. Note that the payload in the request object is expected to be byte[]
     *
     * @param restServiceAdapter The prepared RestServiceAdapter (means containing all request properties. The payload will be overwritten with and empty String)"
     * @param request MCSRequest object
     * @param responseContext The response object to return to the client
     * @return MCSResponse object containing the payload and theresponse header information
     * @throws Exception
     */
    private static final MCSResponse handleBinaryArgumentRequest(RestServiceAdapter restServiceAdapter, MCSRequest request) throws Exception{
                
        String url = restServiceAdapter.getConnectionEndPoint(request.getConnectionName()) + request.getRequestURI();
        //prepare the response context object to return the outcome of the REST reqest
        MCSResponse response = new MCSResponse(request.getMbeConfig());
        
        HashMap headerProperties = restServiceAdapter.getRequestProperties();
       
        request.getMbeConfig().getLogger().logFine("Getting http connection", "MCSRestClient", "handleBinaryArgumentRequest");
        HttpConnection httpConnection = restServiceAdapter.getHttpConnection(request.getHttpMethod().toString(), url, headerProperties);                       
        
        //determine connection to be HttpSyncConnection or javax.microedition.io.HttpConnection connection. In the first case any gzipp'ed content is already
        //unzipped. In the latter case this uinzipping needs to be done manually in this class
        boolean isSnychHttpConnection = httpConnection instanceof SyncHttpConnection ? true:false;
        
        OutputStream outputStream = httpConnection.openDataOutputStream();  
        
        try{      
            if(outputStream != null){
                request.getMbeConfig().getLogger().logFine("Output stream OK", "MCSRestClient", "handleBinaryArgumentRequest");
                //serialize Object to binary[]
                copyStream(new ByteArrayInputStream((byte[])request.getPayload()) , outputStream);                
                 
                //this line actually invokes the content upload. Don't move this line as otherwise content may not
                //upload properly
                Integer statusCode = httpConnection.getResponseCode();
                request.getMbeConfig().getLogger().logFine("Content uploaded. Response code is: "+statusCode, "MCSRestClient", "handleBinaryArgumentRequest");
                request.getMbeConfig().getLogger().logFine("Response message: "+httpConnection.getResponseMessage(), "MCSRestClient", "handleBinaryArgumentRequest");
                
                request.getMbeConfig().getLogger().logFine("Checking whether response is GZIP encoded", "MCSRestClient", "handleBinaryArgumentRequest");
                String contentEncoding = httpConnection.getHeaderField("Content-Encoding");
                String responseMessage = "";
       
                if(contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")){
                   request.getMbeConfig().getLogger().logFine("Response is GZIP encoded", "MCSRestClient", "handleBinaryArgumentRequest");
                   
                  
                   
                   responseMessage = getResponse(httpConnection.openInputStream(), true, isSnychHttpConnection);
                }
                else{
                   request.getMbeConfig().getLogger().logFine("Response is not GZIP encoded", "MCSRestClient", "handleBinaryArgumentRequest");
                   responseMessage = getResponse(httpConnection.openInputStream(), false,isSnychHttpConnection);
                }
                                
                //set the response status, headers and the returned payload to the context object for
                //delivery to the requesting client
                
                response.setHttpStatusCode(statusCode);                
                response.setHeaders(restServiceAdapter.getResponseHeaders());
                
                request.getMbeConfig().getLogger().logFine("Response form MCS is: "+responseMessage, "MCSRestClient", "handleBinaryArgumentRequest");
                response.setMessage(responseMessage);
                response.setOriginalRequestUrl(url);
            }
            else{
                request.getMbeConfig().getLogger().logError("Request could not be send to server. No error received.", "RestClient.java", "handleBinaryRequest");
            
            }        
        }
        catch(Exception e){
            //rethrow as in this try/catch block we are only 
            //intersted in closing the output stream gracefully
            throw e;
        }
        finally{
            outputStream.close();
            httpConnection.close();
        }
        return response;
    }



    /**
     * Helper method to copy the payload into the outgoing stream. The method is marked as static as it is called from
     * a static method
     * @param input
     * @param output
     * @throws IOException
     */
     private static void copyStream(InputStream input, OutputStream output) throws IOException {      
        byte[] buffer = new byte[1024]; // Adjust if you want
           
           int bytesRead;           
           while ((bytesRead = input.read(buffer)) != -1) {
               output.write(buffer, 0, bytesRead);
           }
       }

       
     /**
     * Read the response message to report the result to MAF application
     * @param is
     * @return String with the response message (success or error)
     */
       private static String getResponse(InputStream is, boolean gzipEncoded, boolean isSynchHttpConnection) {
           Reader reader = null;
           StringWriter writer = null;
           String charset = "UTF-8"; //  
           InputStream response = null;
        
         try {
              
              /*
               * If SnychHttpConnection is used then we don't need to handle GZIP as the unzipping 
               * is handled for us already. If however HttpConnection is used then it needs to be
               * handled
               */
              if(gzipEncoded == true && !isSynchHttpConnection){ 
                  response = new GZIPInputStream(is);                                      
               }
               else{
                   response = is; 
               }
               
              reader = new InputStreamReader(response, charset);
              writer = new StringWriter();

              char[] buffer = new char[10240];
              for (int length = 0; (length = reader.read(buffer)) > 0;) {
                     writer.write(buffer, 0, length);
               }           
               } catch (IOException e) {
                 
               }               
               //housekeeping
               finally {
                 try {
                    writer.close();
                    reader.close();
                } catch (IOException e) {
                    //nothing we can do here. So we leave the dirt and go. 
                }            
            }
            
            return writer.toString();
       }

    /**
     * Method that returns the HttpConnection object for direct use in cases where the RestServiceAdapter may not
     * provide enough functionality. Use this API by exception. The HttpConnection itself is obtained from the
     * RestServiceAdapter and thus contains all of the RestServiceAdapter configuration and headerString
     *
     * @param request MCSRequest with information required by the RestServiceAdapter
     * @return HttpConnection
     */
    public static HttpConnection getHttpConnection(MCSRequest request) throws Exception {
        RestServiceAdapter restServiceAdapter = prepareRestServiceAdapter(request);
        String url = restServiceAdapter.getConnectionEndPoint(request.getConnectionName()) + request.getRequestURI();
        HashMap headerProperties = request.getHttpHeaders();
        HttpConnection connection = restServiceAdapter.getHttpConnection(request.getHttpMethod().toString(), url, headerProperties);
        return connection;
    }

    /**
     * Creates and configures an instance of RestServiceAdapter with information from the request context
     * @param request
     * @return RestServiceAdapter
     */
    private static RestServiceAdapter prepareRestServiceAdapter(MCSRequest request) {
        RestServiceAdapter restServiceAdapter = Model.createRestServiceAdapter();
        restServiceAdapter.clearRequestProperties();

        restServiceAdapter.setConnectionName(request.getConnectionName());
        restServiceAdapter.setRequestType(request.getHttpMethod().toString());
        restServiceAdapter.setRequestURI(request.getRequestURI());
        restServiceAdapter.setRetryLimit(request.getRetryLimit());

        //default accept header and content-type header to application/jason. The values
        //are overwritten with the information in the header map of the request context
        //object. If not then they are set at least
        restServiceAdapter.addRequestProperty("Accept", "application/json");
        restServiceAdapter.addRequestProperty("Content-Type", "application/json");
        
        //for basic authentication, requests in Oracle MCS allways happen in the context of a valid Oracle Mobile Backend object. For this 
        //reason it is set here. If the information is contained in the custom headers then this information here gets overwritten with the 
        //information in the headers. If the authentication type is OAUTH then the mobile backend Id is not set
        if(request.getMbeConfig() != null && !request.getMbeConfig().getAuthtype().equalsIgnoreCase(MBEConstants.OAUTH_AUTH)
           && request.getMbeConfig().getMobileBackendIdentifier() != null && !request.getMbeConfig().getMobileBackendIdentifier().isEmpty()){           
            restServiceAdapter.addRequestProperty(HeaderConstants.ORACLE_MOBILE_BACKEND_ID, request.getMbeConfig().getMobileBackendIdentifier());            
        }
        else{
            //the mobile backend id can also be passed as part of the header map, in which case it would not be 
            //required in the RequestContext object. While this is not an error, at least we will log it here
            
            getLogger(request).logFine(" Mobile backend ID could not be added to header for request to: "+request.getRequestURI() , "RestClient", "prepareRestServiceAdapter");
            
        }               
        
        HashMap<String,String> allRequestPropertyMap = new HashMap<String,String>();
                      
        //check for authorization headers in case of manual authentication. I authentication is performed by 
        //MAF on the feature level, then the authorization headers are automatically added
         HashMap<String,String> authMap = addAuthorizationHeaderIfNeeded(request.getMbeConfig());
        //if authorization headers need to be added to the request, add them to the request headers
        if (authMap != null && !authMap.isEmpty()){
            allRequestPropertyMap.putAll(authMap);
        }
        
        Map<String,String> requestHeaderMap = request.getHttpHeaders();
        if (requestHeaderMap != null && !requestHeaderMap.isEmpty()){
            allRequestPropertyMap.putAll(requestHeaderMap);
        }
        
        if (allRequestPropertyMap != null && !allRequestPropertyMap.isEmpty()) {
            
            Set keySet = allRequestPropertyMap.keySet();
            //read the headers passed in the request context
            for (Object headerParam : keySet) {
                String paramName = (String) headerParam;                
                String paramValue = allRequestPropertyMap.get(paramName).toString();
                restServiceAdapter.addRequestProperty(paramName, paramValue);                 
            }
        }
        return restServiceAdapter;
    }
    
    
    /**
     * Developers have the option to authenticate MBE session using the MAF authentication configured on the MAF Feature
     * level, or by passing username / password to the MBE configuration when creating the session. For the latter case,
     * the authorizationProvider header needs to be set manually
     */
    private static  HashMap<String,String> addAuthorizationHeaderIfNeeded(MBEConfiguration mbeConfiguration) {
        
         HashMap<String,String> headers = new  HashMap<String,String>();
        
        if (mbeConfiguration.isManualAuthentication()) {
            if (mbeConfiguration!= null && mbeConfiguration.getAuthtype().equalsIgnoreCase(MBEConstants.BASIC_AUTH)) {
                headers.put("Authorization", mbeConfiguration.getAuthorizationToken());
            }
            if (mbeConfiguration != null && mbeConfiguration.getAuthtype().equalsIgnoreCase(MBEConstants.OAUTH_AUTH)){
                headers.put("Authorization", mbeConfiguration.getAuthorizationToken());
            }
        }
        return headers;
    }
    
    /**
     * Accesses the MBE logger instance for this mobile backend
     * @param request
     * @return UtilLogger instance
     */
    private static UtilLogger getLogger(MCSRequest request){
        
        //try to get MBE logger
        if(request.getMbeConfig() != null && request.getMbeConfig().getLogger() != null){
            return request.getMbeConfig().getLogger();
        }
        //get LibraryLogger instead
        else{
            return new LibraryLogger();
        }
    }    
}
