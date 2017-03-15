package com.oracle.maf.sample.mcs.shared.authorization.oauth;

import com.oracle.maf.sample.mcs.shared.authorization.auth.Authorization;
import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.log.MBELogger;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSResponse;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRestClient;
import com.oracle.maf.sample.mcs.shared.mbe.MBE;
import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;
import com.oracle.maf.sample.mcs.shared.utils.MapUtils;

import java.util.Base64;
import java.util.GregorianCalendar;
import java.util.HashMap;

import oracle.adfmf.framework.api.AdfmfJavaUtilities;

import oracle.adfmf.json.JSONObject;

import oracle.mobile.cloud.internal.rest.RestClient;

/**
 * MAF MCS Utility access to Oracle MCS can be implemented through Oracle MAF security framework or manually using this
 * authorization class. The OAUTH authentication supports resource owner and client credential (anonymous) authentication.
 *
 * With OAuth, when you create a mobile backend or register with an existing mobile backend, a set of OAuth consumer keys
 * (that is, client credentials) consisting of a client ID and client secret are generated for you. The values of these keys
 * are unique to the mobile backend. You authenticate yourself to the OAuth server by providing your client credentials and
 * receive an access token that is passed in each API call via a header. Only a user with a valid token can access the API.
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public final class OauthAuthorization implements Authorization {

    private String mobileBackendID = null;
    private MBELogger mLogger = null;

    private MBEConfiguration mbeConfig = null;
    private MBE mobileBackend = null;

    private boolean isAnonymousAuthentication = false;

    public OauthAuthorization(MBE mbe) {
        super();

        this.mobileBackend = mbe;
        this.mbeConfig = mobileBackend.getMbeConfiguration();
        this.mobileBackendID = mbeConfig.getMobileBackendIdentifier();

        mLogger = mbeConfig.getLogger();
        mLogger.logFine("OAUTH Authorization for MBE: " + mbe.getMbeConfiguration().getMobileBackendIdentifier(),
                        this.getClass().getSimpleName(), "Constructor");
    }


    @Override
    public String getUsername() {
        return this.mbeConfig.getAuthenticatedUsername();
    }

    /**
     * Returns the OAuth authorizationProvider token for when users authenticated through MAF MCS Utility. For users that
     * authenticate through MAF Features or users that are not authenticated, this method returns an empty map
     *
     * @return the OAuth authentication token as a base64 encoded string in a map or an empty map if authentication
     * isn't performed through MAF MCS Util or is performed on the MAF Feature level
     */
    @Override
    public HashMap<String, String> getHTTPAuthorizationHeader() {
        HashMap<String, String> headers = new HashMap<String, String>();

        //the MBE configuration has the token set upon successful authentication
        if (this.mbeConfig.isManualAuthentication()) {
            headers.put("Authorization", this.mbeConfig.getAuthorizationToken());
        }

        mLogger.logFine("Authorization Header returned: " + MapUtils.dumpStringProperties(headers),
                        this.getClass().getSimpleName(), "getHTTPAuthorizationHeader");
        return headers;
    }


    @Override

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
    public void authenticate(String username, String password) throws IllegalArgumentException, ServiceProxyException {
      try{
         ResourceOwnerGrant resourceOwnerGrant = new ResourceOwnerGrant(this.mbeConfig);
         resourceOwnerGrant.authenticate(username, password);
         
         //set the authentication type to not anonymous
         this.isAnonymousAuthentication = false;    
        }
        catch (ServiceProxyException spe){
            //rethrow the service proxy exception that could be cause by the OAUTH server not being accessible or the 
            //username - password authentication failing
            throw spe;
        }

    }

    @Override
    /**
        * Oracle MCS does not require sessions to be logged out because sessions are authorized through an authorization
        * token. By removing the token from the MBE configuration, subsequent calls will not be authorized until the user
        * logs in again
    */
    public void logout() throws ServiceProxyException {
        this.mbeConfig.setManualAuthentication(false);
        this.mbeConfig.setAuthorizationToken("");
        this.mbeConfig.setAuthenticatedUsername(null);
        this.mbeConfig.setAuthenticatedUserMCSUserId(null);
    }

    @Override
    public void authenticateAsAnonymous() throws ServiceProxyException {
        try{
           ClientCredentialGrant clientCredentialGrant = new ClientCredentialGrant(this.mbeConfig);
           clientCredentialGrant.authenticate();
           
           //set the authentication type to not anonymous
           this.isAnonymousAuthentication = true;    
          }
          catch (ServiceProxyException spe){
              //rethrow the service proxy exception that could be cause by the OAUTH server not being accessible or the 
              //username - password authentication failing
              throw spe;
          }
    }

    @Override
    /**
    * Performs client credential grant type authentication based on client secret and client Id. This is the equivalent 
    * to anonymous authentication in basic authentication
    *
    * @throws ServiceProxyException in case of MCS application error or exception thrown on the REST transport layer
    * @throws IllegalArgumentException if the client Id and client secret are not provided in the MBE configuration
     */
    public boolean isAnonymousAuthentication() {
        return isAnonymousAuthentication;
    }
}
