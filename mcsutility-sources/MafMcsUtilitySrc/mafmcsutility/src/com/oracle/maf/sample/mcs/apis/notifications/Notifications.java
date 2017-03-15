package com.oracle.maf.sample.mcs.apis.notifications;

import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.headers.HeaderConstants;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSResponse;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRestClient;
import com.oracle.maf.sample.mcs.shared.mbe.error.OracleMobileErrorHelper;
import com.oracle.maf.sample.mcs.shared.mbe.proxy.MBEServiceProxy;
import com.oracle.maf.sample.mcs.shared.utils.MAFUtil;

import java.util.HashMap;
import java.util.Iterator;

import oracle.adfmf.json.JSONException;
import oracle.adfmf.json.JSONObject;


/**
 * *** INTERNAL FRAMEWORK CLASS ***
 * ***  Access through MBE only ***
 *
 * Proxy class to work with Mobile Cloud Service (MCS) notification service.
 *
 * Push notifications are notifications sent from an external source, such as MCS, to an application on a mobile device.
 * The Notifications.java class does not handle receiving push notifications but entitles MCS to send push messages to this
 * client. To receove push notifications in a MAG application you need to configure push notifications for MAF
 *
 * For details on how to enable MAF appications to receive push notifications, read "Enabling and Using Notifications"
 * in the Oracle Mobile Application Framework Developing Mobile Applications with Oracle Mobile Application Framework
 * documentation.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class Notifications extends MBEServiceProxy {


    private static final String REGISTER_DEVICE_URL = "/mobile/platform/devices/register";
    private static final String DEREGISTER_DEVICE_URL = "/mobile/platform/devices/deregister";

    private boolean deviceRegisteredForPush = false;

    public Notifications() {
        super();
    }


    /**
     * Method that registers the device. This method composes the JSON payload for the registration . The payload structure
     * sent to the server looks similar to shown below
     * <p>
     * <pre>
     * {"notificationToken":"c7645c692e143855054b40c3621d4c262ce1f97f0fd62a844bef34eab991758b",
     *     "mobileClient":{
     *         "id":"com.company.mycert.FiFTechnician",
     *         "version":"1.0",
     *         "platform":"IOS"
     *      }
     *  }
     * </pre>
     * <p>
     * Note that the id is eirther the package and application name on Android or the bundled Id on Apple. The version 
     * identifier can be left to 1.0 as it doesn't seem to play a role in MCS. Provide the platform in all uppercase 
     * letter as otherwise MCS complains about the mobile client Id
     * <p>
     * Upon success, the response contains the JSON object with the device registration information including
     * the MCS internal ID for the registration.
     *
     * e.g.
     * <pre>
     * {
     *   "id": "8a8a1eff-83c3-41b4-bea8-33357962d9a7",
     *   "notificationToken": "c7645c692e143855054b40c3621d4c262ce1f97f0fd62a844bef34eab991758b",
     *   "mobileClient": {
     *   "id": "com.company.mycert.FiFTechnician",
     *   "version": "1.0",
     *   "platform": "IOS"
     *   },
     *     "modifiedOn": "2014-05-05T12:09:33.281Z"
     *  }
     *  </pre>
     * <p>
     *  Note the first "id" attribute in the response, which is an identifier internally kept on Oracle MCS. This Id can 
     *  be used to send notifictions to this specific device. However, sending notifications can only be done from the 
     *  server (as a client application, you could call a custom API that then calls the server side API for sending 
     *  notifications)
     * <p>
     * The MAF application receives the payload string and then parses it into a JSON object. So lets assume the response
     * payload with the JSON string above is saved in a string "responseString" on the MAF side. In this case parsing could be
     * performed like shown below
     *
     * <pre>
     * JSONObject messageJSON = new JSONObject(responseString);
     * String mcsId = messageJSON.optString("id");
     * JSONObject mobileClientJSON = messageJSON.optJSONObject("mobileClient");
     * String clientId = mobileClientJSON.optString("id");
     * ...
     * </pre>
     *
     * @return a JSON string object as the message of the MCSSuccess object.
     * @throws ServiceProxyException if an MCS application error occurs, a problem in MAF MCS or a REST transport layer exception
     */
    public String registerDeviceToMCS() throws ServiceProxyException {
        this.getMbe().getMbeConfiguration().getLogger().logFine("Attempting to register device to MCS for notification. MBE id = " +
                        this.getMbe().getMbeConfiguration().getMobileBackendIdentifier(), this.getClass().getSimpleName(),
                        "registerDeviceToMCSForPush");

        return handleDeviceMcsRegistrationDeRegistration(REGISTER_DEVICE_URL);
    }


    /**
     * In case of a successful deregistration, a text string is returned with no further information other than deregistration succeeded.
     *
     * @throws ServiceProxyException if an MCS application error occurs, a problem in MAF MCS or a REST transport layer exception
     * @return a text string as the message confirming the deregistration
     */
    public String deregisterDeviceFromMCS() throws ServiceProxyException {

        this.getMbe().getMbeConfiguration().getLogger().logFine("Attempting to register device to MCS for notification. MBE id = " +
                        this.getMbe().getMbeConfiguration().getMobileBackendIdentifier(), this.getClass().getSimpleName(),
                        "registerDeviceToMCSForPush");
        return handleDeviceMcsRegistrationDeRegistration(DEREGISTER_DEVICE_URL);
    }

    /**
     * Method that registers or de-registers the device. This method composes the JSON payload for the registration and de-registration.
     *
     * Upon "registration" success, the callback payload contains the JSON object with the device registration information (in case of registration) including
     * the MCS internal ID for the registration.
     *
     * e.g.
     * <pre>
     * {
     *   "id": "8a8a1eff-83c3-41b4-bea8-33357962d9a7",
     *   "notificationToken": "c7645c692e143855054b40c3621d4c262ce1f97f0fd62a844bef34eab991758b",
     *   "mobileClient": {
     *   "id": "com.company.mycert.FiFTechnician",
     *   "version": "1.0",
     *   "platform": "IOS"
     *   },
     *     "modifiedOn": "2014-05-05T12:09:33.281Z"
     *  }
     *  </pre>
     *
     * In case of a successful deregistration, a text string is returned with no furrher information other than deregistration succeeded. Its indicated by a HTTP
     * 200 code returned
     *
     * In the case of an error, the Oracle mobile error message and code is returned in the payload. In the case of an
     * exception, a best attempt is made to return a sensible error message. In this case however the HTTP code is set to 0 (zero)
     *
     * @param mcsURI request URI to register or de-register device
     * @throws ServiceProxyException if an MCS application error occurs, a problem in MAF MCS or a REST transport layer exception
     */
    private String handleDeviceMcsRegistrationDeRegistration(String mcsURI) throws ServiceProxyException {

        this.getMbe().getMbeConfiguration().getLogger().logFine("Device registration or deregistration for push with URI: " + mcsURI + " and MBE id = " +
                        this.getMbe().getMbeConfiguration().getMobileBackendIdentifier(), this.getClass().getSimpleName(),
                        "handleDeviceMcsRegistrationDeRegistration");

        //check for supported Push platform
        if (MAFUtil.getOsVendor().equals(MAFUtil.VENDOR_GOOGLE) || MAFUtil.getOsVendor().equals(MAFUtil.VENDOR_APPLE)) {
            this.getMbe().getMbeConfiguration().getLogger().logFine("Registration for OS = " + MAFUtil.getOsVendor() + ". MBE id = " +
                            this.getMbe().getMbeConfiguration().getMobileBackendIdentifier(), this.getClass().getSimpleName(),
                            "handleDeviceMcsRegistrationDeRegistration");
            String payloadString = "";

            try {

                //prepare to register client to MCS

                MCSRequest requestObject = new MCSRequest(this.getMbe().getMbeConfiguration());
                //register of de-register Device from MCS
                requestObject.setRequestURI(mcsURI);
                requestObject.setConnectionName(this.getMbe().getMbeConfiguration().getMafRestConnectionName());
                requestObject.setHttpMethod(com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest.HttpMethod.POST);

                JSONObject payloadJSONObject = new JSONObject();
                JSONObject nestedDetailJSONObject = new JSONObject();

                //ID attribute is only added to payload upon registration
                if (MAFUtil.getOsVendor().equalsIgnoreCase(MAFUtil.VENDOR_GOOGLE)) {

                    //Google wants is package name here. The MAF application ID becomes the Google bundle ID unless changed
                    //In the latter case, the application developer should have set this value in the MBE configuration to
                    //whatever the custom name is
                    nestedDetailJSONObject.put("id", "\"" + this.getMbe().getMbeConfiguration().getGooglePackageName() + "\"");

                } else if (MAFUtil.getOsVendor().equals(MAFUtil.VENDOR_APPLE)) {
                    nestedDetailJSONObject.put("id", "\"" + this.getMbe().getMbeConfiguration().getAppleBundleId() + "\"");
                }
                
                //not sure the version value is looked at. However, I see this hard coded in the Android SDK, so doing
                //the same here. Also note that the version only needs to be added when registering the client
                if (mcsURI.equalsIgnoreCase(REGISTER_DEVICE_URL)) {
                    nestedDetailJSONObject.put("version", "\"" + "1.0" + "\"");
                }
                nestedDetailJSONObject.put("platform", "\"" + MAFUtil.getDeviceOS().toUpperCase() + "\"");

                //add version and platform
                payloadJSONObject.put("mobileClient", nestedDetailJSONObject);

                payloadJSONObject.put("notificationToken", "\"" + this.getMbe().getMbeConfiguration().getDeviceToken() + "\"");

                payloadString = this.stringifyJSONObject(payloadJSONObject);

                if (mcsURI.equalsIgnoreCase(REGISTER_DEVICE_URL)) {
                    this.getMbe().getMbeConfiguration().getLogger().logFine("Payload for device registration: " + payloadString,
                                    this.getClass().getSimpleName(), "handleDeviceMcsRegistrationDeRegistration");
                } else {
                    this.getMbe().getMbeConfiguration().getLogger().logFine("Payload for device de-registration: " + payloadString,
                                    this.getClass().getSimpleName(), "handleDeviceMcsRegistrationDeRegistration");
                }

                 HashMap<String,String> headers = new  HashMap<String,String>();

                //Note: Authorization is automatically added to the requet header either by MAF or the RestClient

                headers.put(HeaderConstants.CONTENT_TYPE_HEADER, "application/json");
                //headers.put(HeaderConstants.ORACLE_MOBILE_BACKEND_ID, this.getMbe().getMbeConfiguration().getMobileBackendIdentifier());
                requestObject.setHttpHeaders(headers);

                requestObject.setPayload(payloadString);

                MCSResponse mcsResponse = MCSRestClient.sendForStringResponse(requestObject);

                //add loogin in case of application failure
                if (mcsResponse != null) {

                    //customize message for operation that happens
                    String operation = mcsURI.equalsIgnoreCase(REGISTER_DEVICE_URL) ? "registration" : "deregistration";

                    if (mcsResponse.getHttpStatusCode() == 201 || mcsResponse.getHttpStatusCode() == 200) {
                        this.getMbe().getMbeConfiguration().getLogger().logFine(MAFUtil.getOsVendor() + " device " + operation + " successful.",
                                        this.getClass().getSimpleName(), "handleDeviceMcsRegistrationDeRegistration");
                        return (String) mcsResponse.getMessage();
                    }

                    else {
                        this.getMbe().getMbeConfiguration().getLogger().logError("Push notification " + operation + " failed with HTTP ERROR " +
                                         mcsResponse.getHttpStatusCode(), this.getClass().getSimpleName(),
                                         "handleDeviceMcsRegistrationDeRegistration");
                        this.getMbe().getMbeConfiguration().getLogger().logError("Oracle MCS Error Message:" + mcsResponse.getMessage(),
                                         this.getClass().getSimpleName(), "handleDeviceMcsRegistrationDeRegistration");

                        throw new ServiceProxyException(mcsResponse.getHttpStatusCode(), (String) mcsResponse.getMessage(), mcsResponse.getHeaders());
                    }
                }
            } catch (Exception e) {
                String exceptionPrimaryMessage = e.getLocalizedMessage();
                String exceptionSecondaryMessage = e.getCause() != null ? e.getCause().getLocalizedMessage() : null;
                 String combinedExceptionMessage =  "primary message:"+exceptionPrimaryMessage+(exceptionSecondaryMessage!=null?("; secondary message: "+exceptionSecondaryMessage):(""));
                
                this.getMbe().getMbeConfiguration().getLogger().logFine("Exception thrown: "+combinedExceptionMessage,
                                this.getClass().getSimpleName(),"handleDeviceMcsRegistrationDeRegistration");

                //check if HTTP 201 was treated as error (exception)
                if (exceptionPrimaryMessage.contains("201")) {
                    //try to get the MCS respose from the exception
                    if (exceptionSecondaryMessage != null) {
                        try {
                            JSONObject jsonErrorObject = new JSONObject(exceptionSecondaryMessage);
                            //if we get here, then its a Oracle MCS error JSON Object. Get the
                            int statusCode = jsonErrorObject.optInt("status", 201);
                            throw new ServiceProxyException(statusCode, exceptionSecondaryMessage);

                        } catch (JSONException jse) {
                            //if parsing fails, the this is proof enough that the error message is not
                            //an Oracle MCS message and we need to continue our analysis
                            this.getMbe().getMbeConfiguration().getLogger().logFine("Exception message does not contain an Oracle MCS JSONObject",
                                            this.getClass().getSimpleName(),"handleDeviceMcsRegistrationDeRegistration");
                        }
                    }

                    //return an Oracle Mobile JSON message. Note that the detail message misses the MCS internal registration Id and
                    //only returns the valid registration JSON string. The internal registration id isn't required to deregister the
                    //device or to receive notifications.
                    return OracleMobileErrorHelper.createOracleMobileErrorJson(201, "Device registration successful.",
                                                                               payloadString, REGISTER_DEVICE_URL);
                }

                this.getMbe().getMbeConfiguration().getLogger().logError("Failed to send REST request to server: " + e.getClass().toString() + " :: " +
                                 e.getMessage(), this.getClass().getSimpleName(),
                                 "handleDeviceMcsRegistrationDeRegistration");

                //if the problem could not be solved, then chances are that the error is an Oracle MCS application error.
                //So check if the exception is an application error
                this.handleExceptions(e, mcsURI);
            }
        } else {
            this.getMbe().getMbeConfiguration().getLogger().logError("Vendor not supported for push. This version of MAF MCS UTIL only supports push integration for Google and Apple devices ",
                             this.getClass().getSimpleName(), "handleDeviceMcsRegistrationDeRegistration");

            throw new ServiceProxyException("MAF MCS Utility Version 1 supports push for Google and Apple devices. " + MAFUtil.getOsVendor() + " is not upported",ServiceProxyException.ERROR);
        }
        //we should not get here
        return null;
    }


    /**
     * Simple JSONObject-String parser that parses JSONObject that may or may not contain nested
     * instances of JSONObject
     *
     * @param jsonObjectToStringify
     * @return JSONObject string representation
     */
    private String stringifyJSONObject(JSONObject jsonObjectToStringify) {

        try {
            Iterator keys = jsonObjectToStringify.keys();
            //open bracket
            StringBuffer sb = new StringBuffer("{");
            //iterate keys to create JSON string structure
            while (keys.hasNext()) {
                //determine whether to add a comma as a delimiter between attributes
                if (sb.length() > 1) {
                    sb.append(',');
                }
                String key = (String) keys.next();
                //ensure proper encoding of JSON strings e.g using backslash
                //encodings for quote charaters
                sb.append(JSONObject.quote(key.toString()));
                sb.append(':');
                Object valueObject = jsonObjectToStringify.get(key);

                //check if value is nested JSON object
                if (valueObject instanceof JSONObject) {
                    //recursive call to this method
                    sb.append(this.stringifyJSONObject((JSONObject) valueObject));
                }
                //simple value
                else {
                    sb.append(valueObject);
                }
            }
            sb.append('}');
            return sb.toString();
        } catch (Exception e) {
            this.getMbe().getMbeConfiguration().getLogger().logError("Failed parsing JSONObject: " + jsonObjectToStringify + " to String",
                             this.getClass().getSimpleName(), "stringifyJSONObject");
            return null;
        }
    }


    /**
     * Returns information about whether the device is registered for receiving MCS notifications
     * @return true/false
     */
    public boolean isDeviceRegisteredForPush() {
        return deviceRegisteredForPush;
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
            "primary message: " + exceptionPrimaryMessage + exceptionSecondaryMessage != null ?
            "; detailed message: exceptionSecondaryMessage" : "";

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

                this.getMbe().getMbeConfiguration().getLogger().logFine("Exception message is not a Oracle MCS error JSONObject",
                                this.getClass().getSimpleName(), "handleExcpetions");
            }
        }

        //continue message analysis and check for known error codes for the references MCS API

        this.getMbe().getMbeConfiguration().getLogger().logFine("Rest invocation failed with following message" + exceptionPrimaryMessage,
                        this.getClass().getSimpleName(), "handleException");

        int httpErrorCode = -1;
        String restoredOracleMcsErrorMessage = null;

        /*
               *  Try to identify an MCS failure from the exception message.
               */
        if (combinedExceptionMessage.contains("400")) {
            httpErrorCode = 400;
            restoredOracleMcsErrorMessage =
                OracleMobileErrorHelper.createOracleMobileErrorJson(400, "The device information is not valid",
                                                                    "The device information is not valid. Chances are that either the device Id or the MBE Id reference is wrong",
                                                                    uri);
        } else if (combinedExceptionMessage.contains("401")) {
            httpErrorCode = 401;
            restoredOracleMcsErrorMessage =
                OracleMobileErrorHelper.createOracleMobileErrorJson(401, "Not authorized to invoke this call",
                                                                    "Not authorized to invoke this call", uri);
        }

        else {
            this.getMbe().getMbeConfiguration().getLogger().logFine("Request failed with Exception: " + e.getClass().getSimpleName() + "; message: " +
                            e.getLocalizedMessage(), this.getClass().getSimpleName(), "handleExcpetion");
            throw new ServiceProxyException(e.getLocalizedMessage(), ServiceProxyException.ERROR);
        }
        //if we get here then again its an Oracle MCS error, though one we found by inspecting the exception message
        this.getMbe().getMbeConfiguration().getLogger().logFine("Request succeeded successful but failed with MCS application error. HTTP response: " +
                        httpErrorCode + ", Error message: " + restoredOracleMcsErrorMessage,
                        this.getClass().getSimpleName(), "handleExcpetion");
        throw new ServiceProxyException(httpErrorCode, restoredOracleMcsErrorMessage);

    }
}
