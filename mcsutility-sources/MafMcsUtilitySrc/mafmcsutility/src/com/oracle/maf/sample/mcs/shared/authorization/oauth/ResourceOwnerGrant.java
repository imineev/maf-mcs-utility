package com.oracle.maf.sample.mcs.shared.authorization.oauth;

import com.oracle.maf.sample.mcs.apis.userinfo.User;
import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.headers.HeaderConstants;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSResponse;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRestClient;
import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;

import java.util.Base64;
import java.util.GregorianCalendar;
import java.util.HashMap;

import oracle.adfmf.json.JSONObject;


/**
 * The resource owner grant type requires access to the authentication server for obtaining a credential
 * token based on a user username/password pair.
 *
 * Authentication is performed following these steps:
 * ==================================================
 *
 *
 * How to create a resource owner credential grant the REST way, which is what this class wraps:
 *
 * 1. Base64 encode the clientID:clientSecret string.
 * 2. Set the Authorization header to Basic client id:client secret-Base64-encoded-string.
 * 3. Set the X-User-Identity-Domain-Name header to your environment's domain name.
 * 4. Set the Content-Type to application/x-www-form-urlencoded; charset=utf-8.
 * 5. Set the request body to  grant_type=password&username=username&password=password.
 *
 * The user name and password must be URL encoded.
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
public class ResourceOwnerGrant {

    private static int STATUS_RESPONSE_OK = 200;

    private MBEConfiguration mbeConfig = null;
    
    
    //make calling an empty constructor a no-op
    private ResourceOwnerGrant() {
    };

    protected ResourceOwnerGrant(MBEConfiguration mbeConfig) {
        super();
        this.mbeConfig = mbeConfig;
    }


    /**
     * Performs recource credential owner type authentication based on a provided username/password pair plus the OAUT
     * client secret and client Id
     *
     * @param username the MCS mobile user name
     * @param password the MSCS mobile user name
     * @throws ServiceProxyException in case of MCS application error or exception thrown on the REST transport layer
     * @throws IllegalArgumentException if the username or password argument is null, or if the client Id and client
     *         secret are not provided
     */
    protected void authenticate(String username, String password) throws IllegalArgumentException,
                                                                         ServiceProxyException {

        //username and password are required
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Username and / or password cannot be null");
        }

        //indert configuration required. The MBE client ID and the client secret are required
        if (this.mbeConfig.getMobileBackendIdentifier() == null ||
            this.mbeConfig.getMobileBackendIdentifier().isEmpty() || this.mbeConfig.getAnonymousKey() == null ||
            this.mbeConfig.getAnonymousKey().isEmpty()) {
            throw new IllegalArgumentException("The OAUTH client ID and / or client secret are not set in the MBEConfiguration object");
        }


        //read configuration from application preferences

        String oauthClientId = this.mbeConfig.getMobileBackendIdentifier();

        //the secret key in the configuration is the OAUTH cliet ID of OAUTH is used for authentication and
        //the anonymous token when basic authentication is used.
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
        
        request.setPayload("grant_type=password&username=" + username + "&password=" + password);


        try {
            MCSResponse response = MCSRestClient.sendForStringResponse(request);

            if (response.getHttpStatusCode() == STATUS_RESPONSE_OK) {

                mbeConfig.getLogger().logFine("Rest Call succeeded with http-200. RAW Json response string is " +
                                              response.getMessage(), this.getClass().getSimpleName(), "authenticate");

                JSONObject jsonObject = new JSONObject((String) response.getMessage());

                String accessToken = jsonObject.getString("access_token");
                String tokenType = jsonObject.getString("token_type");

                //create a token of type "Bearer jkhjhucheiuwnnoibofoi..."
                this.mbeConfig.setAuthorizationToken(tokenType+ " " + accessToken);
                
                //set flag to indicate that authentication is through MAF MCS Utility and not MAF
                this.mbeConfig.setManualAuthentication(true);
                this.mbeConfig.setAuthenticatedUsername(username);

                long tokenExpriresInMilliseconds = new Long(jsonObject.getString("expires_in")).longValue() * 1000;
                long expiryTimeInMilliSeconds =
                    GregorianCalendar.getInstance().getTimeInMillis() + tokenExpriresInMilliseconds;

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

        //next, verify the OAUTH access token in a call to the user management API. Note that this call issues a second
        //call to MCS, which causes some additional delay. However, it will bring OAUTH en-par with the information that 
        //is set internally in the MAF MCS Utility MBE config file after basic authentication. 
        verifyOAuthSuccess(mbeConfig.getAuthorizationToken());               
    }
    
    
    /**
     * Method verifies OAUTH authentication sucess in that it calls the server side user management AP to obtain the user 
     * internal user ID, which then is set to the authenticatedUserMCSUserId variable in the MBE configuration object.
     * 
     * @param bearerToken The "Bearer <base 64 encoded bearere token> string obtained in resonse to OAUTH authentication
     * @throws IllegalArgumentException Thrown if token is null or empty
     * @throws ServiceProxyException Thrown in case of verification failure
     */
    private void verifyOAuthSuccess (String bearerToken) throws IllegalArgumentException,  ServiceProxyException {


        //URI of the user management API
        final String AUTH_LOGIN_URL = "/mobile/platform/users/~";
        //Http header to add the bearer token to
        final String AUTH_HEADER = "Authorization";

        if (bearerToken == null || bearerToken.length() == 0) {
            throw new IllegalArgumentException("OAUTH Bearer token cannot be null in verifyOAuthSuccess in ResourceOwnerGrant");
        }

        MCSRequest requestObject = new MCSRequest(this.mbeConfig);
        requestObject.setHttpMethod(MCSRequest.HttpMethod.GET);
        requestObject.setRequestURI(AUTH_LOGIN_URL);


        HashMap<String, String> httpHeaders = new HashMap<String, String>();
        httpHeaders.put(AUTH_HEADER, bearerToken);
        httpHeaders.put(HeaderConstants.ACCEPT_HEADER, "application/json");

        requestObject.setHttpHeaders(httpHeaders);


        try {
            this.mbeConfig.getLogger().logFine("Atttempt to verify OAUTH token", this.getClass().getSimpleName(), "verifyOAuthSuccess");
           
            MCSResponse mcsResponse = MCSRestClient.sendForStringResponse(requestObject);

            if (mcsResponse != null && mcsResponse.getHttpStatusCode() == STATUS_RESPONSE_OK) {
                 JSONObject jsonObject = new JSONObject((String) mcsResponse.getMessage());
                 mbeConfig.setAuthenticatedUserMCSUserId(jsonObject.getString(User.USER_ID));
               this.mbeConfig.getLogger().logFine("OAUTH access verified with MBE", this.getClass().getSimpleName(), "verifyOAuthSuccess");

            } else {
                throw new ServiceProxyException(mcsResponse.getHttpStatusCode(), (String) mcsResponse.getMessage(),
                                                mcsResponse.getHeaders());
            }

        } catch (Exception e) {
          this.mbeConfig.getLogger().logError("OAUTH verification failed with Exception " + e.getClass().toString() + " :: " + e.getMessage(),this.getClass().getSimpleName(), "verifyOAuthSuccess");
            new ServiceProxyException(e, ServiceProxyException.ERROR);
        }
    }
}
