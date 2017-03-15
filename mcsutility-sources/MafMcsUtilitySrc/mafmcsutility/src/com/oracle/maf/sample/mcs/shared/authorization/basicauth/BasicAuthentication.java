package com.oracle.maf.sample.mcs.shared.authorization.basicauth;

import com.oracle.maf.sample.mcs.apis.userinfo.User;
import com.oracle.maf.sample.mcs.shared.authorization.auth.Authorization;
import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.headers.HeaderConstants;
import com.oracle.maf.sample.mcs.shared.log.MBELogger;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSResponse;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRestClient;
import com.oracle.maf.sample.mcs.shared.mbe.MBE;
import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;
import com.oracle.maf.sample.mcs.shared.mbe.constants.MBEConstants;
import com.oracle.maf.sample.mcs.shared.mbe.error.OracleMobileErrorHelper;
import com.oracle.maf.sample.mcs.shared.utils.MapUtils;
import com.oracle.maf.sample.mcs.shared.utils.StringEncodeUtil;

import java.util.HashMap;

import oracle.adfmf.json.JSONException;
import oracle.adfmf.json.JSONObject;

/**
 * Using mobile backends gives you a unified security context for your APIs. By default, APIs require user authentication
 * before they can be accessed. Authentication can be user based or anonymous. For anonymous authentication, the anonymous
 * key for an MBE must be provided in the MBEConfiguration object. Otherwise username and password need to be provided. Any
 * API that requires authorizationProvider - going beyond authentication - requires named user authentication as security roles are
 * applied to users and not anonymous access.
 *
 * The MCS client platform APIs that can be accessed from anonymous users include Analytics and custom APIs (if the author of
 * a custom API did select anonymous access)
 *
 * Possible HTTP Status Codes returned by the call
 *
 * HTTP 200 Login successful.
 * HTTP 401 User is unauthorized to perform this call.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public final class BasicAuthentication implements Authorization {


    private static final String AUTH_HEADER = "Authorization";
    public static final String AUTH_LOGIN_URL = "/mobile/platform/users/";

    private static int STATUS_RESPONSE_OK = 200;

    private String mobileBackendID = null;

    private MBEConfiguration mbeConfig = null;
    private MBE mobileBackend = null;

    private MBELogger mLogger = null;

    private String anonymousKey = null;

    private boolean isAnonymousAuthentication = false;

    public BasicAuthentication(MBE mbe) {
        super();
        this.mobileBackend = mbe;
        this.mbeConfig = mobileBackend.getMbeConfiguration();


        this.mobileBackendID = mbeConfig.getMobileBackendIdentifier();
        this.anonymousKey = mbeConfig.getAnonymousKey();
        mLogger = mbe.getMbeConfiguration().getLogger();

        mLogger.logFine("Basic Authorization for MBE: " + mbe.getMbeConfiguration().getMobileBackendIdentifier(),
                        this.getClass().getSimpleName(), "Constructor");
    }


    /**
     * performs basic authentication against Oracle MBE. The authentication does not do anything else than returning
     * a HTTP 200 for a successful request, which indicates the username and password pair in the Authorization header
     *
     * @param username the MCS mobile user name
     * @param password the MSCS mobile user name
     * @throws ServiceProxyException in case of MCS application error or exception thrown on the REST transport layer
     * @throws IllegalArgumentException if the username or password argument is null
     */
    public void authenticate(String username, String password) throws IllegalArgumentException, ServiceProxyException {
        mLogger.logFine("Authenticate: " + username + " PW == null?: " + (password == null ? true : false),
                        this.getClass().getSimpleName(), "authenticate");
        //username and password must be provided
        if ((username == null || username.length() == 0) || (password == null || password.length() == 0)) {
            throw new IllegalArgumentException("Username and password cannot be null in Basic Authentication");
        }

        //BasicAuthentication uses Base64 encoding
        String userCredentials = username + ":" + password;
        String base64EncodedCredentials = "Basic " + StringEncodeUtil.base64Encode(userCredentials);

        mLogger.logFine("Authenticating user " + username + " (backendId: " + mbeConfig.getMobileBackendIdentifier() +
                        ")", this.getClass().getSimpleName(), "authenticate");
        mLogger.logFine("Authentication URI: " + AUTH_LOGIN_URL + " (full MBE URL: " +
                        mbeConfig.getMobileBackendBaseURL() + AUTH_LOGIN_URL + ")", this.getClass().getSimpleName(),
                        "authenticate");

        handleBasicAuthentication(username, base64EncodedCredentials);
        //if authentication fails, a service proxy exception is thrown that would prevent the code to get here
        isAnonymousAuthentication = false;
    }

    /**
     * Oracle MCS allows users to authenticate with a secret client key that is generated when an MBE is created. The
     * key is changed when an MBE is moved along in the deployment cylce (development, testing, production) and allows
     * clients to access non-protected APIs (aka. public APIs) that don't require user privileges.
     * @throws ServiceProxyException in case of MCS application error or exception thrown on the REST transport layer
     * @throws IllegalArgumentException if callback argument is null
     */
    public void authenticateAsAnonymous() throws ServiceProxyException {

        mLogger.logFine("AnonymousAuthentication for MBE: " + this.mobileBackendID, this.getClass().getSimpleName(),
                        "anonymousAuthentication");

        String base64EncodedCredentials = "Basic " + this.mbeConfig.getAnonymousKey();

        mLogger.logFine("Authenticating anonymous user (backendId: " + mbeConfig.getMobileBackendIdentifier() + ")",
                        this.getClass().getSimpleName(), "authenticate");
        mLogger.logFine("Authentication URI: " + AUTH_LOGIN_URL + " (full MBE URL: " +
                        mbeConfig.getMobileBackendBaseURL() + AUTH_LOGIN_URL + ")", this.getClass().getSimpleName(),
                        "authenticateAnonymous");


        this.handleBasicAuthentication("___anonymous___internal", base64EncodedCredentials);
        //if authentication fails, a service proxy exception is thrown that would prevent the code to get here
        isAnonymousAuthentication = true;
    }


    /**
     * Method that calls the MCS REST API to verify user login credentials. This method is used with named and
     * anonymous authentication. In the latter case the base64 encoded string is the anonymous key configured in
     * the MBE config.
     *
     * @param username  username or anonymous
     * @param base64EncodedCredentials username:password as base 64 encoded string. For anonymous login its the anonmyous token for the MBE
     * @throws IllegalArgumentException if callback, username and  base64EncodedCredentials arguments are null
     * @throws ServiceProxyException in case of MCS application failure or exception thrown on the REST transport layer
     *
     */
    private void handleBasicAuthentication(String username,
                                           String base64EncodedCredentials) throws IllegalArgumentException,
                                                                                   ServiceProxyException {


        if ((username == null || username.length() == 0) ||
            (base64EncodedCredentials == null || base64EncodedCredentials.length() == 0)) {
            throw new IllegalArgumentException("Username, credentials cannot be null in Basic Authentication");
        }

        MCSRequest requestObject = new MCSRequest(this.mbeConfig);
        requestObject.setHttpMethod(MCSRequest.HttpMethod.GET);

        if (username.equalsIgnoreCase("___anonymous___internal")) {
            //anonymous credentials cannot be verified by obtaining user information as there is no user information
            //to access (and error is thrown instead). So we verify against an internal API
            requestObject.setRequestURI(AUTH_LOGIN_URL + "login");
        } else {
            //allows to obtain user information long with the login
            requestObject.setRequestURI(AUTH_LOGIN_URL + "~");
        }


        HashMap<String, String> httpHeaders = new HashMap<String, String>();
        httpHeaders.put(AUTH_HEADER, base64EncodedCredentials);
        httpHeaders.put(HeaderConstants.ORACLE_MOBILE_BACKEND_ID, mbeConfig.getMobileBackendIdentifier());
        httpHeaders.put(HeaderConstants.ACCEPT_HEADER, "application/json");

        requestObject.setHttpHeaders(httpHeaders);


        try {
            mLogger.logFine("Atttempt to authenticate user: " +
                            (username.equalsIgnoreCase("___anonymous___internal") ? "anonymous" : username),
                            this.getClass().getSimpleName(), "handleBasicAuthentication");
            MCSResponse mcsResponse = MCSRestClient.sendForStringResponse(requestObject);

            //throw exception if the authnetication call succeeds but the authorization not
            if (mcsResponse != null && mcsResponse.getHttpStatusCode() == STATUS_RESPONSE_OK) {

                //authorization succeeded
                //set the basic authorization credentials to the configuration class
                mbeConfig.setAuthorizationToken(base64EncodedCredentials);
                //set the flag to the mobile backend configuration that authentication has been overridden
                mbeConfig.setManualAuthentication(true);
                //set authenticated user name to anonymous or the authenticated user account
                mbeConfig.setAuthenticatedUsername(username.equalsIgnoreCase("___anonymous___internal") ? "anonymous" :
                                                   username);

                //for authenticated users, set the Oracle MCS user id to the MCS user ID obtained from the login
                if (username.equalsIgnoreCase("___anonymous___internal") == false) {
                    //Set the authenticated userId as known in Oracle MCS. This information is required for accessing
                    //user isolated collections
                    JSONObject jsonObject = new JSONObject((String) mcsResponse.getMessage());
                    mbeConfig.setAuthenticatedUserMCSUserId(jsonObject.getString(User.USER_ID));
                } else {
                    mbeConfig.setAuthenticatedUserMCSUserId(null);
                }

                mLogger.logFine("Login success", this.getClass().getSimpleName(), "handleBasicAuthentication");

            } else {
                throw new ServiceProxyException(mcsResponse.getHttpStatusCode(), (String) mcsResponse.getMessage(),
                                                mcsResponse.getHeaders());
            }

        } catch (Exception e) {

            //just in case HTTP 401 caused an exception. Try to detect error code for proper message

            mLogger.logError("Login failed with Exception " + e.getClass().toString() + " :: " + e.getMessage(),
                             this.getClass().getSimpleName(), "handleBasicAuthentication");
            this.handleExceptions(e, AUTH_LOGIN_URL);
        }
    }

    /**
     * If authorizaton is handled through the utility, the username of the authenticated user is set
     * @return username
     */
    public String getUsername() {
        return this.mbeConfig.getAuthenticatedUsername();
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

    /**
     * Returns the Basic authorizationProvider token for when users authenticated through MAF MCS Utility. For users that
     * authenticate through MAF Features or users that are not authenticated, this method returns an empty map
     *
     * @return the basic authentication token as a base64 encoded string in a map or an empty map if authentication
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

    /**
     * This method analyzes the exception for instances of AdfInvocationRuntimeException, AdfInvocation-Exception and, more
     * broadly, AdfExceptions. If none of the two are found, it will look into the exception message for status codes known
     * returned by the API called by the utility. If still the exception isn't identified as Oracle MCS, it will be rethrown
     * with a status code of -1.
     *
     * @param e    the exception thrown by Oracle ADF.
     * @param uri  the origin uri. This uri is used if the error message is composed based on error code findings in the exception
     *             message
     */
    private void handleExceptions(Exception e, String uri) throws ServiceProxyException {

        //Step 1: Is error AdfInvocationRuntimeException, AdfInvocationException or AdfException?
        String exceptionPrimaryMessage = e.getLocalizedMessage();
        String exceptionSecondaryMessage = e.getCause() != null ? e.getCause().getLocalizedMessage() : null;

        String combinedExceptionMessage =
            "primary message:" + exceptionPrimaryMessage +
            (exceptionSecondaryMessage != null ? ("; secondary message: " + exceptionSecondaryMessage) : (""));
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
        if (exceptionSecondaryMessage != null) {
            try {
                JSONObject jsonErrorObject = new JSONObject(exceptionSecondaryMessage);
                //if we get here, then its a Oracle MCS error JSON Object. Get the
                //status code or set it to 0 (means none is found)
                int statusCode = jsonErrorObject.optInt("status", 0);
                throw new ServiceProxyException(statusCode, exceptionSecondaryMessage);

            } catch (JSONException jse) {
                //if parsing fails, the this is proof enough that the error message is not
                //an Oracle MCS message and we need to continue our analysis
                this.mLogger.logFine("Exception message is not a Oracle MCS error JSONObject",
                                     this.getClass().getSimpleName(), "handleExceptions");
            }
        }

        //continue message analysis and check for known error codes for the references MCS API
        if (combinedExceptionMessage.contains("401")) {
            int httpErrorCode = 401;
            String reconstructedOracleMCSErrorMessage =
                OracleMobileErrorHelper.createOracleMobileErrorJson(401,
                                                                    "Authorization failed. Verify user credentials",
                                                                    "The user is not authorized to perform this operation.",
                                                                    uri);
            throw new ServiceProxyException(httpErrorCode, reconstructedOracleMCSErrorMessage);
        } else if (combinedExceptionMessage.contains("404")) {
            int httpErrorCode = 404;
            String reconstructedOracleMCSErrorMessage =
                OracleMobileErrorHelper.createOracleMobileErrorJson(404, "Mobile Backend not found",
                                                                    "We cannot find the active mobile backend for the given Id " +
                                                                    this.mbeConfig.getMobileBackendIdentifier() +
                                                                    " and BASIC schema. Specify a valid clientId and try again.",
                                                                    uri);
            throw new ServiceProxyException(httpErrorCode, reconstructedOracleMCSErrorMessage);
        } else {
            throw new ServiceProxyException(e.getLocalizedMessage(), ServiceProxyException.ERROR);
        }

    }

    @Override
    /**
     * Return information about whether authentication is performed with anonmyous credentials
     * @return true if authentication was anonymous, false otherwise
     */
    public boolean isAnonymousAuthentication() {
        return isAnonymousAuthentication;
    }
}
