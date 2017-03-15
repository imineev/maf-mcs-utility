package com.oracle.maf.sample.mcs.shared.authorization.oauth;

import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSResponse;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRestClient;
import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;

import java.util.Base64;
import java.util.GregorianCalendar;
import java.util.HashMap;

import oracle.adfmf.json.JSONObject;


/**
 * The Client Credentials grant type is used when the client is requesting access to protected resources anonymously,
 * which is when no 3rd party username/password is provided.
 *
 *
 * How to create a client credential grant the REST way, which is what this class wraps:
 *
 * 1. Base64 encode the clientID:clientSecret string.
 * 2. Set the Authorization header to Basic client id:client secret-Base64-encoded-string.
 * 3. Set the X-User-Identity-Domain-Name header to your environment's domain name.
 * 4. Set the Content-Type to application/x-www-form-urlencoded; charset=utf-8.
 * 5. Set the request body to  grant_type=client_credentials
 *
 * The response will include an access_token property, as shown below (the value is truncated in this example).
 * {"oracle_client_assertion_type":"urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
 *   "expires_in":604800,
 *   "token_type":"Bearer",
 *   "oracle_tk_context":"client_assertion",
 *  "access_token":"eyJhbGciOiJ...FIqFiA"}
 *
 *  In your REST calls to MCS APIs, set the Authorization header to Bearer access_token.
 *
 * Upon receiving the OAUTH token, the Authorization header for the request needs to be changed from BASIC to BEARER
 * Authorization -> Bearer huihfiuzfbuibuinlnndriwngdobzg78zbdfhbudjiobf...
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class ClientCredentialGrant {
    
    private static int STATUS_RESPONSE_OK = 200;
    private MBEConfiguration mbeConfig = null;
    
    private ClientCredentialGrant() {
        super();
    }
    
    protected ClientCredentialGrant(MBEConfiguration mbeConfig) {
        super();
        this.mbeConfig = mbeConfig;
    }
    
    
    /**
     * Performs client credential grant authentication based on a provided client secret and client Id pair

     * @throws ServiceProxyException in case of MCS application error or exception thrown on the REST transport layer
     * @throws IllegalArgumentException if the client Id and client  secret are not provided
     */
    protected void authenticate() throws IllegalArgumentException, ServiceProxyException {

        if (this.mbeConfig.getMobileBackendIdentifier() == null || this.mbeConfig.getMobileBackendIdentifier().isEmpty() 
            || this.mbeConfig.getAnonymousKey() == null || this.mbeConfig.getAnonymousKey().isEmpty()) {
            throw new IllegalArgumentException("The OAUTH client ID and / or client secret are not set in the MBEConfiguration object");
        }


        //read configuration from application preferences

        String oauthClientId = this.mbeConfig.getMobileBackendIdentifier();

        //the secret key in the configuration is the OAUTH client ID of OAUTH is used for authentication and the anonymous token when basic authentication is used.
        String oauthClientSecret = this.mbeConfig.getAnonymousKey();

        String oauthTokenEndpointConnectionName = this.mbeConfig.getOauthEndpointConnectionName();

        mbeConfig.getLogger().logFine("Preferences: oauthClientId = " + oauthClientId + " oauthTokenEndpointConnectionName=" + oauthTokenEndpointConnectionName, this.getClass().getSimpleName(), "authenticate");

        //to authenticate the OAUTH user, the client ID and the client secret must be sent in a BASIC authorization header. The values are
        //base64 encoded and concatenated by a ":"
        String base64EncodedBasicCredentials =  "Basic " + Base64.getEncoder().encodeToString((oauthClientId + ":" + oauthClientSecret).getBytes());

        //prepare the REST call headers
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        headers.put("Authorization", base64EncodedBasicCredentials);
        
        //set the identity domain of the cloud instance. 
        headers.put("X-User-Identity-Domain-Name", mbeConfig.getOauthIdentityDomain());

        MCSRequest request = new MCSRequest(this.mbeConfig);

        request.setHttpHeaders(headers);

        request.setHttpMethod(MCSRequest.HttpMethod.POST);

        //the MCS token endpoint URL is provided by the Mobile Backend (MBE). The URL does not need any addition and thus
        //the request URI is set to an empty string
        request.setConnectionName(oauthTokenEndpointConnectionName);
        request.setRequestURI("");
        
        request.setPayload("grant_type=client_credentials");


        try {
            MCSResponse response = MCSRestClient.sendForStringResponse(request);

            if (response.getHttpStatusCode() == STATUS_RESPONSE_OK) {

                mbeConfig.getLogger().logFine("Rest Call succeeded with http-200. RAW Json response string is " + response.getMessage(), this.getClass().getSimpleName(), "authenticate");

                JSONObject jsonObject = new JSONObject((String) response.getMessage());

                String accessToken = jsonObject.getString("access_token");
                String tokenType = jsonObject.getString("token_type");

                //create a token of type "Bearer jkhjhucheiuwnnoibofoi..."
                this.mbeConfig.setAuthorizationToken(tokenType+ " " + accessToken);
                //seta flag indicating that the MBE is authenticated through MAF MCS Utility and not MAF 
                this.mbeConfig.setManualAuthentication(true);
                
                this.mbeConfig.setAuthenticatedUsername(null);

                long tokenExpriresInMilliseconds = new Long(jsonObject.getString("expires_in")).longValue() * 1000;
                long expiryTimeInMilliSeconds = GregorianCalendar.getInstance().getTimeInMillis() + tokenExpriresInMilliseconds;

                //save token expiry time for later use in MAF applications
                this.mbeConfig.setOauthTokenExpiryTimeInMilliSeconds(expiryTimeInMilliSeconds);

            } else {
                mbeConfig.getLogger().logFine("Rest Call failed with http-" + response.getHttpStatusCode() +". RAW Json response string is " + response.getMessage(),
                                              this.getClass().getSimpleName(), "authenticate");

            }

        } catch (Exception e) {

    //TODO Optimize exception messages
            mbeConfig.getLogger().logError("REST Call fails with exception: " + e.getMessage(),
                                           this.getClass().getSimpleName(), "authenticate");


            if ((e.getMessage()).toLowerCase().contains("connection refused")) {
                new ServiceProxyException("Service not available. Please try again later. Full exception message is: " +
                                          e.getLocalizedMessage(), ServiceProxyException.ERROR);
            } else {
                new ServiceProxyException("Login failed. Please verify username and password",
                                          ServiceProxyException.ERROR);
            }
        }             
    }
}
