package com.oracle.maf.sample.mcs.apis.storage;

import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.headers.HeaderConstants;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSResponse;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRestClient;
import com.oracle.maf.sample.mcs.shared.mbe.error.OracleMobileErrorHelper;
import com.oracle.maf.sample.mcs.shared.mbe.proxy.MBEServiceProxy;

import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.HashMap;

import oracle.adfmf.json.JSONArray;
import oracle.adfmf.json.JSONException;
import oracle.adfmf.json.JSONObject;

/**
 *
 * *** INTERNAL FRAMEWORK CLASS ***
 * ***  Access through MBE only ***
 *
 * The Oracle Mobile Cloud Service Storage feature provides an easy way to store, share and access opaque data objects  like
 * images or documents that belong to a specific mobile use case. Storage should not be confused with document management or
 * a database.
 *
 * The Oracle Mobile Cloud Service Storage feature provides an easy way to store, share and access opaque data objects like
 * images or documents that belong to a specific mobile use case.
 *
 * Proxy class to access the Mobile Cloud Service (MCS) mobile backend (MBE) Storage API. To use the storage API the
 * mobile backend must be associated with the collection referenced by its ID. In addition, users must be granted the
 * READ, READ_WRITE, READ_ALL or READ_WRITE_ALL role dependent on the collection's isolate state. This class returns an
 * instance of StorageCollection that represents the collection in an MBE. The StorageCollection object then exposes
 * APIs to access the Collection content on the MBE, which is when the server side round trip happens.
 *
 * <p>
 * Important: Storage in Oracle MCS requires an authenticated user. Anonymous users currently have no access to Oracle
 * MCS Storage objects.
 * <p>
 * About this class
 * ================
 * <p>
 * The Storage object class in MAF MCS Utility allows users to query all collections available for an MBE. This functonality
 * is provided by the "queryStorageInformation" method that returns instance of StorageInformation.
 *
 * With this information, application developer learn to know about the collections in an MBE so they can call the
 * "querySingleCollection" or "querySingleCollectionForUserId" methods to populate an instance of StorageCollection,
 * which is a proxy class that wraps REST calls to MCS for uploading and downloading content, as well as to create,
 * update and delete collection objects. To work on collection objects that are not owned by the authenticated user,
 * you need to call "querySingleCollectionForUserId" and pass the user Id (not the name) of the user for which the
 * StorageCollection object should update, create, delete or download objects. The StorageCollection class has a public
 * method that allows you to change that user context at runtime. Even when gettig a StorageCollection object that has
 * the user Id set, its still can be used to query all users objects for isolated collections if the authenticaed has
 * READ_ALL or READ_WRITE_ALL permission to the collection
 * <p>
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class Storage extends MBEServiceProxy {

    public Storage() {
        super();
    }


    /**
     *
     * Queries MCS MBE Storage for information about collections (based on "offset" and "limit" filter values) it contains.
     * Use this method e.g. to populate a list of values showing available collections as well as their shared or isolated
     * states or for users to choose where to upload content to
     *
     * @param offset the index where you want to start browsing the list of items offset cannot be lower than 0 (first item in a collection)
     * @param limit the maximum number of items to be returned. This value is optional and can be applied as null
     * @throws IllegalArgumentException if the offset is lower than 0 (zero) or provided as null
     * @throws ServiceProxyException if the REST request to Oracle MCS fails or an error occured on the REST transport
     * @return
     */
    public StorageInformation queryStorageInformation(Integer offset, Integer limit) throws ServiceProxyException {

        StorageInformation storageInformation = null;

        if (offset == null || offset.intValue() < 0) {
            throw new IllegalArgumentException("\"offset\" in argument to getListOfStorageCollection method call cannot be null or empty");
        }

        this.getMbe().getMbeConfiguration().getLogger().logFine("Querying collections from MCS. \"offset\" = " + offset,
                                                                this.getClass().getSimpleName(),
                                                                "getStorageInformation");

        //MCS Query Example URI: /mobile/platform/storage/collections?limit=100&totalResults=true&offset=0" --> we always want the total count and so this
        //query parameter is always set

        String uri = StorageConstants.STORAGE_RELATIVE_URL + "?totalResults=true&offset=" + offset +(limit != null ? ("&limit=" + limit.intValue()) : (""));

        this.getMbe().getMbeConfiguration().getLogger().logFine("Storage request URI=" + uri,
                                                                this.getClass().getSimpleName(),
                                                                "getStorageInformation");

        MCSRequest requestObject = new MCSRequest(this.getMbe().getMbeConfiguration());
        
        requestObject.setHttpMethod(MCSRequest.HttpMethod.GET);
        requestObject.setConnectionName(this.getMbe().getMbeConfiguration().getMafRestConnectionName());
        requestObject.setRequestURI(uri);

        this.getMbe().getMbeConfiguration().getLogger().logFine("RestURI added to request=" + uri,
                                                                this.getClass().getSimpleName(),
                                                                "getStorageInformation");

         HashMap<String,String> httpHeaders = new  HashMap<String,String>();
        httpHeaders.put(HeaderConstants.ACCEPT_HEADER, "application/json");
        requestObject.setHttpHeaders(httpHeaders);


        try {
            this.getMbe().getMbeConfiguration().getLogger().logFine("sending REST request",
                                                                    this.getClass().getSimpleName(),
                                                                    "getStorageInformation");
            MCSResponse mafRestResponse = MCSRestClient.sendForStringResponse(requestObject);
            int status = mafRestResponse.getHttpStatusCode();

            //check if request succeeded with returned payload or if application error occured. In the latter case,
            //throw service proxy exception
            if (status == StorageConstants.HTTP_200) {

                this.getMbe().getMbeConfiguration().getLogger().logFine("REST call returned successfully",
                                                                        this.getClass().getSimpleName(),
                                                                        "getStorageInformation");

                this.getMbe().getMbeConfiguration().getLogger().logFine("Creating JSONObject from response message",this.getClass().getSimpleName(),"getStorageInformation");
                JSONObject jsonObject = new JSONObject((String) mafRestResponse.getMessage());

                //create storage information object and populate with attributes
                storageInformation = new StorageInformation();


                /*
                *   "hasMore"   --> are there more collection information to fecth? If so call this methid with offset+limit
                *   "limit"     --> max. fetch size for collection information
                *   "offset"    --> start index (zero based) of where to start reading collection information
                *   "count"     --> number of collection information found (e.g 15 collections)
                *   "totalResults" --> how many collections are there in total within this storage
                *
                */
                if (jsonObject.has(StorageConstants.STORAGE_INFORMATION_COUNT)) {
                    this.getMbe().getMbeConfiguration().getLogger().logFine("Reading "+StorageConstants.STORAGE_INFORMATION_COUNT+" JSON attribute",this.getClass().getSimpleName(),"getStorageInformation");
                    storageInformation.setCount(jsonObject.getInt(StorageConstants.STORAGE_INFORMATION_COUNT));
                }

                if (jsonObject.has(StorageConstants.STORAGE_INFORMATION_HAS_MORE)) {                    
                    storageInformation.setHasMore(jsonObject.getBoolean(StorageConstants.STORAGE_INFORMATION_HAS_MORE));
                }

                if (jsonObject.has(StorageConstants.STORAGE_INFORMATION_LIMIT)) {                    
                    storageInformation.setLimit(jsonObject.getInt(StorageConstants.STORAGE_INFORMATION_LIMIT));
                }

                if (jsonObject.has(StorageConstants.STORAGE_INFORMATION_OFFSET)) {                    
                    storageInformation.setOffset(jsonObject.getInt(StorageConstants.STORAGE_INFORMATION_OFFSET));
                }

                if (jsonObject.has(StorageConstants.STORAGE_INFORMATION_TOTAL_RESULTS)) {                    
                    storageInformation.setTotalResults(jsonObject.getInt(StorageConstants.STORAGE_INFORMATION_TOTAL_RESULTS));
                }

                //get the information about the fetched collections
                if (storageInformation.getCount() > 0 && jsonObject.has(StorageConstants.STORAGE_INFORMATION_ITEMS)) {
                    this.getMbe().getMbeConfiguration().getLogger().logFine("Storage Collections  found",
                                                                            this.getClass().getSimpleName(),
                                                                            "getStorageInformation");

                    JSONArray collectionArray = jsonObject.getJSONArray((StorageConstants.STORAGE_INFORMATION_ITEMS));

                    //list of collections in a storage (for at least as many as can be fetched due to offset/limit ratio
                    ArrayList<StorageCollection> storageCollectionList = new ArrayList<StorageCollection>();

                    for (int i = 0; i < collectionArray.length(); i++) {
                        JSONObject collection = collectionArray.getJSONObject(i);

                        String collectionId = collection.getString(StorageConstants.STORAGE_COLLECTION_ID);

                        StorageCollection storageCollection = new StorageCollection(collectionId, this);
                        
                        /*
                         * Provide the descriptive attributes for the collection. The collection ID is enough 
                         * infromation for the StorageCollection to determine the request API to query objects
                         * in the collection. The StorageCollection object is a entity and proxy object you use
                         * to interact with the server side collection in MCS MBE
                         */
                        String description = collection.getString(StorageConstants.STORAGE_COLLECTION_DESCRIPTION);
                        storageCollection.setDescription(description);
                        
                        int contentLength = collection.getInt(StorageConstants.STORAGE_COLLECTION_CONTENT_LENGTH);
                        storageCollection.setContentLength(contentLength);
                        
                        String eTag = collection.getString(StorageConstants.STORAGE_COLLECTION_ETAG);
                        storageCollection.setETag(eTag);
                        
                        //if user isolated is set, use value. Else use isolated collection state as the default
                        boolean userIsolated = collection.optBoolean((StorageConstants.STORAGE_COLLECTION_USER_ISOLATED),true);                                                
                        storageCollection.setIsUserIsolated(userIsolated);                        

                        this.getMbe().getMbeConfiguration().getLogger().logFine("Collection object added to list: "+storageCollection.dump(),
                                                                                this.getClass().getSimpleName(),"getStorageInformation");

                        storageCollectionList.add(storageCollection);

                    }

                    //add list of fetched collection items to ist
                    storageInformation.setItems(storageCollectionList);
                    
                } else {
                    //the storage does not have any collections. This is allowed but should be logged
                    this.getMbe().getMbeConfiguration().getLogger().logFine("No Storage Collections found",this.getClass().getSimpleName(), "getStorageInformation");
                }


            } else {
                this.getMbe().getMbeConfiguration().getLogger().logError("REST Invocation Failed for Request URL: " +mafRestResponse.getOriginalRequestUrl(),this.getClass().getSimpleName(),"getStorageCollectionList");                
                this.getMbe().getMbeConfiguration().getLogger().logError("Error message: " +mafRestResponse.getMessage(),this.getClass().getSimpleName(),"getStorageCollectionList");               
                throw new ServiceProxyException(status, (String) mafRestResponse.getMessage(),mafRestResponse.getHeaders());
            }

        }
        //chances are that a failed request leads to an exception and not a proper RestServiceAdapter response. To handle this case
        //gracefully, the exception message is processed and analyzed for MCS MAF messages
        catch (Exception e) {
            this.handleExceptions(e, uri);
        }

        return storageInformation;
    }


    /**
     * Creates a StorageCollection object with information about the collection in the MBE. The JSON object that is
     * returned from the MCS MBE looks similar to
     *
     * <pre>
     * {
     * "id": "SharedImages",
     * "description": "Collection to store uploaded images",
     * "contentLength": 196016,
     * "eTag": "\"1.0\"",
     * "links": [
     * {
     * "rel": "canonical",
     * "href": "/mobile/platform/storage/collections/SharedImages"
     * },
     * {
     * "rel": "self",
     * "href": "/mobile/platform/storage/collections/SharedImages"
     * }
     * ]
     * }
     * </pre>
     *
     * This information is exposed by the StorageCollection object along with methods to query and upate objects in
     * the collection.
     *
     * This method queries shared collections as well as isolated collections for the current authenticated user. If an
     * isolated collection should be queried for a different user (assuming the authenticated user has READ_ALL or WRITE_
     * READ_ALL privilege for the collection) you need to call the querySingleCollectionForUserId(...) method, passing
     * the userId of the user you want to query the collection objects of. Note that the userId is not the username but
     * the ID maintained for each user in MCS
     *
     * @param collectionId ID that identifies a collection in the remote MBE
     * @return StorageCollection object that contains information about the referenced collection
     * @throws IllegalArgumentException if the collectionId argument is null or empty
     * @throws ServiceProxyException if the storageId cannot be UTF-8 application/x-www-form-urlencoded
     */
    public StorageCollection querySingleCollection(String collectionId) throws ServiceProxyException {

        if (collectionId == null || collectionId.length() == 0) {
            throw new IllegalArgumentException("collectionId in argument to getStorageCollection method call cannot be null or empty");
        }

        StorageCollection collection = new StorageCollection(collectionId, this);
        
        //convert storageId to the application/x-www-form-urlencoded MIME format
        String encodedCollectionId = null;

        try {
            encodedCollectionId = URLEncoder.encode(collectionId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            //"UTF-8 application/x-www-form-urlencoded for collectionId failed
            throw new ServiceProxyException(e.getCause(), ServiceProxyException.ERROR);
        }
        this.getMbe().getMbeConfiguration().getLogger().logFine("encoded StorageId=" + encodedCollectionId,
                                                                this.getClass().getSimpleName(),
                                                                "getStorageCollection");

        //reference a specific object in the Storage
        String uri = StorageConstants.STORAGE_RELATIVE_URL + "/" + encodedCollectionId;

        this.getMbe().getMbeConfiguration().getLogger().logFine("Storage URI=" + uri, this.getClass().getSimpleName(),
                                                                "getStorageCollection");
        this.getMbe().getMbeConfiguration().getLogger().logFine("Creating single StorageCollection object for=" +
                                                                collection.getCollectionID() + " and URI=" + uri,
                                                                this.getClass().getSimpleName(),
                                                                "getStorageCollection");

        fetchSingleCollectionInfo(collection, uri);                
        //set the object owner ID to the authenticated user MCS user ID, which is a requirement for 
        //accessing and creating isolated user objects in Oracle MCS. Note that this information is 
        //null  if the user is authenticated as anonymous. However, anonymous users have no access to 
        //storage collections, in which case it doesn't matter if this property is set null. You can 
        //change the object owner Id programmatically at runtime. 
        collection.setObjectOwnerUserID(this.getMbe().getMbeConfiguration().getAuthenticatedUserMCSUserId());
                
        return collection;
    }


    /**
     * Retrieves the collection identified by the collection Id and sets the provided userId as a filter for the
     * collection objects it queries. The userId is not the same as the username used for login but the internal
     * user Id of the user account in Oracle Public Cloud (OPC). CollectionObject operations like update, delete
     * and create require the user ID of the user who owns or who should own the object. So if the authenticated
     * user only queries and updates her own objects in an isolated collection, then no user Id is required and
     * the querySingleCollection(...) method can be used.
     *
     * If a user however has READ_ALL or READ_WRITE_ALL permissions for isolated collections, then to access objects
     * owned by other users require a userId to be set.
     *
     * <p>
     * For the authenticated user you can get the userId through the usermanagement API. However, security restriction in
     * OPC prevents you from being able to query other users userId by providing their username. Here a mobile application
     * logic needs to find a way to obtain the userId to query content for.
     * <p>
     * For user-isolated collections, specifying the user id in the query allows authenticated users with READ or READ_ALL
     * permission to access this user?s collection object. Note that READ permission only entitles for reading the user's
     * own isolated objects. To read other user's isolated objects, READ_ALL is required
     *
     * @param collectionId The ID of the collection to getObjectsInfo the metadata object for
     * @param userId of the user who's collection objects should be queried. The userId is *NOT* the same as the username and 
     * also nan *NOT* be set to '*'. Passing a value of "null"
     * @return StorageCollection identified by the collection Id in the context of the provided user
     * @throws IllegalArgumentException if userId or collectionId arguments are null or empty
     * @throws ServiceProxyException in case of application errors (non HTTP 2XX errors), system errors or transport
     * layer exceptions. The ServiceProxyException message is JSON formatted and can be parsed into OracleMobileError
     * Java object using the OracleMobileErorrHelper class
     */
    public StorageCollection querySingleCollectionForUserId(String collectionId, String userId) throws ServiceProxyException {

        if (collectionId == null || collectionId.isEmpty() || userId.isEmpty()) {

            throw new IllegalArgumentException("collectionId in call to queryCollectionForUserId cannot be null or empty and userId length must not me 0");

        }
        
        if(userId.equalsIgnoreCase("*")){
            throw new IllegalArgumentException("userId value in call to queryCollectionForUserId cannot be set to \"*\"");
        }

        this.getMbe().getMbeConfiguration().getLogger().logFine("Collection Id = " + collectionId + ", userId = " +userId, this.getClass().getSimpleName(),"getStorageCollectionForUserId");
        this.getMbe().getMbeConfiguration().getLogger().logFine("Calling getStorageCollection to create the collection ID",this.getClass().getSimpleName(),"getStorageCollectionForUserId");

        StorageCollection collection = querySingleCollection(collectionId);
        
        //this ensures that collections query and update objects in the scope of the owning user. This is important because
        //collection object ID's are only unique in the context of a user Id for isolated collections. 
        collection.setObjectOwnerUserID(userId);

        return collection;
    }
    
    

    /**
     * Returns information details (meta data information) about a collection in MCS
     *
     * @param collection the object that is update with the remote message
     * @param restURI The URI containing the relative path to the Storage plus the collection ID to lookup.
     *        E.g. /mobile/platform/users/<collectionId>
     *
     * @throws ServiceProxyException in case of application errors (non HTTP 2XX errors), system errors or transport layerexceptions. The ServiceProxyException
     * message is JSON formatted and can be parsed into a Java object (OracleMobileError) using the OracleMobileErorrHelper class
     */
    private void fetchSingleCollectionInfo(StorageCollection collection, String restURI) throws ServiceProxyException {

        this.getMbe().getMbeConfiguration().getLogger().logFine("Enter method", this.getClass().getSimpleName(),"fetchSingleCollectionInfo");

        MCSRequest requestObject = new MCSRequest(this.getMbe().getMbeConfiguration());
        requestObject.setHttpMethod(com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest.HttpMethod.GET);
        requestObject.setConnectionName(this.getMbe().getMbeConfiguration().getMafRestConnectionName());
        requestObject.setRequestURI(restURI);

        this.getMbe().getMbeConfiguration().getLogger().logFine("RestURI added to request=" + restURI,this.getClass().getSimpleName(),"fetchSingleCollectionInfo");

         HashMap<String,String> httpHeaders = new  HashMap<String,String>();
        httpHeaders.put(HeaderConstants.ACCEPT_HEADER, "application/json");
        requestObject.setHttpHeaders(httpHeaders);

        try {
            this.getMbe().getMbeConfiguration().getLogger().logFine("sending REST request",this.getClass().getSimpleName(), "fetchSingleCollectionInfo");
            MCSResponse mafRestResponse = MCSRestClient.sendForStringResponse(requestObject);
            int status = mafRestResponse.getHttpStatusCode();
            //check if request succeeded with returned payload or if application error occured. In the latter case,
            //throw service proxy exception
            if (status == StorageConstants.HTTP_200) {

                this.getMbe().getMbeConfiguration().getLogger().logFine("REST call returned successfully", this.getClass().getSimpleName(),"fetchSingleCollectionInfo");

                JSONObject json = new JSONObject((String) mafRestResponse.getMessage());

                //get collection objects and save them in storage collection
                String description =
                    json.isNull(StorageConstants.COLLECTION_PROPERTY_TAG_DESCRIPTION) ? "" :
                    json.getString(StorageConstants.COLLECTION_PROPERTY_TAG_DESCRIPTION);

                collection.setDescription(description);

                String eTag = json.getString(StorageConstants.COLLECTION_PROPERTY_TAG_ETAG);
                collection.setETag(eTag);

                int contentLength = json.getInt(StorageConstants.COLLECTION_PROPERTY_TAG_CONTENT_LENGTH);
                collection.setContentLength(contentLength);

                if (!json.has(StorageConstants.COLLECTION_PROPERTY_TAG_USER_ISOLATED)) {
                    collection.setIsUserIsolated(false);
                } else {
                    collection.setIsUserIsolated(json.getBoolean(StorageConstants.COLLECTION_PROPERTY_TAG_USER_ISOLATED));
                }
            } else {

                this.getMbe().getMbeConfiguration().getLogger().logError("REST Invocation Failed for Request URL: " +
                                                                         mafRestResponse.getOriginalRequestUrl(),
                                                                         this.getClass().getSimpleName(),
                                                                         "fetchSingleCollectionInfo");
                this.getMbe().getMbeConfiguration().getLogger().logError("Error message: " +
                                                                         mafRestResponse.getMessage(),
                                                                         this.getClass().getSimpleName(),
                                                                         "fetchSingleCollectionInfo");
                throw new ServiceProxyException(status, (String) mafRestResponse.getMessage(),
                                                mafRestResponse.getHeaders());
            }

        } catch (Exception e) {
            this.handleExceptions(e, restURI);
        }
    }

    /**
     * Queries the userId information for the authenticated user using the user management API
     * @return String - userId
     */
    public String getThisUserId() {

        String userName = this.getMbe().getMbeConfiguration().getAuthenticatedUsername();

        this.getMbe().getMbeConfiguration().getLogger().logFine("Start querying user Id for username: "+userName,this.getClass().getSimpleName(), "getThisUserId");


        //compose URI to user information in MCS
        String uri = StorageConstants.USER_INFO_RELATIVE_URL + "/" + userName;
        this.getMbe().getMbeConfiguration().getLogger().logFine("User Info relative URI is " + uri,
                                                                this.getClass().getSimpleName(), "getThisUserId");

        //prepare REST call
        MCSRequest requestCtx = new MCSRequest(this.getMbe().getMbeConfiguration());

        requestCtx.setConnectionName(this.getMbe().getMbeConfiguration().getMafRestConnectionName());
        requestCtx.setHttpMethod(com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest.HttpMethod.GET);
        requestCtx.setRequestURI(uri);

         HashMap<String,String> httpHeaders = new  HashMap<String,String>();

        requestCtx.setHttpHeaders(httpHeaders);

        //no payload needed for GET request
        requestCtx.setPayload("");
        try {
            MCSResponse responseCtx = MCSRestClient.sendForStringResponse(requestCtx);

            //handle request success
            if (responseCtx != null && responseCtx.getHttpStatusCode() == StorageConstants.HTTP_200) {
                JSONObject jsonResponse = new JSONObject((String) responseCtx.getMessage());
                String userId = jsonResponse.getString("id");
                this.getMbe().getMbeConfiguration().getLogger().logFine("User Found. User ID is " + userId,
                                                                        this.getClass().getSimpleName(), "getThisUserId");
                return userId;
            } else {
                this.getMbe().getMbeConfiguration().getLogger().logFine("Rest call succeeded but no user information returned",
                                                                        this.getClass().getSimpleName(), "getThisUserId");
                return null;
            }
        } catch (Exception e) {
            this.getMbe().getMbeConfiguration().getLogger().logError("User Id request fails with Exception " + uri,
                                                                     this.getClass().getSimpleName(), "getUserId");
            this.getMbe().getMbeConfiguration().getLogger().logError("Exceprion cause is: " + e.getCause().getMessage(),
                                                                     this.getClass().getSimpleName(), "getThisUserId");
        }
        return null;
    }
    

    /**
     * This method analyzes the exception for instances of AdfInvocationRuntimeException, AdfInvocation-Exception and, more broadly,
     * AdfExceptions. If none of the two are found, it will look into the exception message for status codes known returned by the API
     * called by the utility. If still the exception isn't identified as Oracle MCS, it will be rethrown with a status code of -1.
     *
     * @param e    the exception thrown by Oracle ADF.
     * @param uri  the origin uri. This uri is used if the error message is composed based on error code findings in the exception
     *             message
     */
    private void handleExceptions(Exception e, String uri) throws ServiceProxyException {


        //Step 1: Is error AdfInvocationRuntimeException, AdfInvocationException or AdfException?
        String exceptionPrimaryMessage = e.getLocalizedMessage();
        String exceptionSecondaryMessage = e.getCause() != null ? e.getCause().getLocalizedMessage() : null;
        String combinedExceptionMessage ="primary message:" + exceptionPrimaryMessage +(exceptionSecondaryMessage != null ? ("; secondary message: " + exceptionSecondaryMessage) : (""));

        this.getMbe().getMbeConfiguration().getLogger().logError("Exception handler received exception with message: "+combinedExceptionMessage,this.getClass().getSimpleName(),"handleExceptions");                
        this.getMbe().getMbeConfiguration().getLogger().logFine("Analyzing message for MCS MBE error messages",this.getClass().getSimpleName(),"handleExceptions");                

        //chances are this is the Oracle MCS error message. If so then ths message has a JSON format. A simple JSON parsing
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
                this.getMbe().getMbeConfiguration().getLogger().logFine("Error message contains MCS MBE JSON formatted error string",this.getClass().getSimpleName(),"handleExceptions");                
                throw new ServiceProxyException(statusCode, exceptionSecondaryMessage);

            } catch (JSONException jse) {
                //if parsing fails, the this is proof enough that the error message is not
                //an Oracle MCS message and we need to continue our analysis

                this.getMbe().getMbeConfiguration().getLogger().logFine("Exception message is not a Oracle MCS error JSONObject",
                                                                        this.getClass().getSimpleName(),
                                                                        "handleExcpetions");
            }
        }

        //continue message analysis and check for known error codes for the references MCS API

        int httpErrorCode = -1;
        String restoredOracleMcsErrorMessage = null;

        /*
        HTTP 400 Bad Request : Returned if "user" query parameter is specified for an endpoint that isn't an storageObject
                               level operation on a user isolated collection.
        */
        if (combinedExceptionMessage.contains("400")) {
            httpErrorCode = StorageConstants.HTTP_400;
            restoredOracleMcsErrorMessage = OracleMobileErrorHelper.createOracleMobileErrorJson(StorageConstants.HTTP_400, "Bad request","query parameter not allowed in this context", uri);
            this.getMbe().getMbeConfiguration().getLogger().logFine("Http 400 error message found. Constructing MCS error message format",
                                                                    this.getClass().getSimpleName(),
                                                                    "handleExcpetions");
            
        }

        /*
        HTTP 401 Unauthorized : The user is not authenticated. The request must be made with the "Authorization" HTTP request header.
                                This would be the case if the authorization headers are not added by MAF or this utility or if the user
                                authenticated as anaonymous
        */
        else if (combinedExceptionMessage.contains("401")) {
            httpErrorCode = StorageConstants.HTTP_401;
            restoredOracleMcsErrorMessage =
                OracleMobileErrorHelper.createOracleMobileErrorJson(StorageConstants.HTTP_401, "Unauthorized",
                                                                    "User not authenticated or MBE Id missing", uri);
            this.getMbe().getMbeConfiguration().getLogger().logFine("Http 401 error message found. Constructing MCS error message format",
                                                                    this.getClass().getSimpleName(),
                                                                    "handleExcpetions");
        }

        /*
        HTTP 403 Forbidden      Retrieve an storageObject without being assigned a role that has READ or READ_WRITE access for the collection. Or,
                                retrieve an storageObject from your isolated space without being assigned a role that has READ, READ_WRITE, READ_ALL,
                                or READ_WRITE_ALL access for the collection.

        */
        else if (combinedExceptionMessage.contains("403")) {
            httpErrorCode = StorageConstants.HTTP_403;
            restoredOracleMcsErrorMessage =
                OracleMobileErrorHelper.createOracleMobileErrorJson(StorageConstants.HTTP_403, "Forbidden",
                                                                    "Retrieve an object without being assigned a role that has READ or READ_WRITE access for the collection. Or,\n" +
                                                                    "                                    retrieve an object from your isolated space without being assigned a role that has READ, READ_WRITE, READ_ALL, \n" +
                                                                    "                                    or READ_WRITE_ALL access for the collection.",
                                                                    uri);
            this.getMbe().getMbeConfiguration().getLogger().logFine("Http 403 error message found. Constructing MCS error message format",
                                                                    this.getClass().getSimpleName(),
                                                                    "handleExcpetions");
        }
        /*
        HTTP 404 Not Found      An storageObjectwith the given identifier does not exist.
        */
        else if (combinedExceptionMessage.contains("404")) {
            httpErrorCode = StorageConstants.HTTP_404;
            restoredOracleMcsErrorMessage =
                OracleMobileErrorHelper.createOracleMobileErrorJson(StorageConstants.HTTP_404, "Not Found",
                                                                    "Object with the specified Id does not exist", uri);
            this.getMbe().getMbeConfiguration().getLogger().logFine("Http 404 error message found. Constructing MCS error message format",
                                                                    this.getClass().getSimpleName(),
                                                                    "handleExcpetions");
        }
        /*
        HTTP 406 Not Acceptable The media type of the resource is not compatible with the values of the "Accept" header.
         */
        else if (combinedExceptionMessage.contains("406")) {
            httpErrorCode = StorageConstants.HTTP_406;
            restoredOracleMcsErrorMessage =
                OracleMobileErrorHelper.createOracleMobileErrorJson(StorageConstants.HTTP_406, "Request Not Accepted",
                                                                    "Accept header parameter did not match the server side content for the response",
                                                                    uri);
            this.getMbe().getMbeConfiguration().getLogger().logFine("Http 406 error message found. Constructing MCS error message format",
                                                                    this.getClass().getSimpleName(),
                                                                    "handleExcpetions");
        }

        else {
            this.getMbe().getMbeConfiguration().getLogger().logFine("MCS Error code not found in message: Request failed with Exception: " +e.getClass().getSimpleName() + "; message: " +e.getLocalizedMessage(),this.getClass().getSimpleName(),"handleExcpetions");
            throw new ServiceProxyException(e.getLocalizedMessage(), ServiceProxyException.ERROR);
        }
        //if we get here then again its an Oracle MCS error, though one we found by inspecting the exception message
        this.getMbe().getMbeConfiguration().getLogger().logFine("Request succeeded successful but failed with MCS application error. HTTP response: " +
                                                                httpErrorCode + ", Error message: " +restoredOracleMcsErrorMessage,this.getClass().getSimpleName(), "handleExcpetions");
        throw new ServiceProxyException(httpErrorCode, restoredOracleMcsErrorMessage);
    }
}
