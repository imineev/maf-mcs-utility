package com.oracle.maf.sample.mcs.apis.storage;

import com.oracle.maf.sample.mcs.shared.exceptions.ServiceProxyException;
import com.oracle.maf.sample.mcs.shared.headers.HeaderConstants;
import com.oracle.maf.sample.mcs.shared.log.MBELogger;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSResponse;
import com.oracle.maf.sample.mcs.shared.mafrest.MCSRestClient;
import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;
import com.oracle.maf.sample.mcs.shared.mbe.error.OracleMobileError;
import com.oracle.maf.sample.mcs.shared.mbe.error.OracleMobileErrorHelper;
import com.oracle.maf.sample.mcs.shared.utils.MAFUtil;
import com.oracle.maf.sample.mcs.shared.utils.MapUtils;

import java.io.UnsupportedEncodingException;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oracle.adfmf.framework.exception.NullPointerException;
import oracle.adfmf.json.JSONArray;
import oracle.adfmf.json.JSONException;
import oracle.adfmf.json.JSONObject;


/**
 * Mobile Object Storage (MOS) Service in MCS allows an application to upload files, images, and other message to a <i>location</i>
 * within a mobile backend. The <i>location</i> is a storage collection that not only holds the reference to the object message but
 * also allows administrators to restrict access to its message. This class retrieves the information about a collection identified
 * by a REST URI for the authenticated user.
 *
 * Note that READ/WRITE/DELETE operations don't use the ETag headers If-Match and If-Non-Match, or If-Modified-Since or If-Unmodified-Since
 * by default. IF you need to implement optimistic locking using ETag or date information you can use the optional eTagHashMap HashMap arguments
 * added to the create/update and remove object APIs the Etag header property. As MAF MCS Utility doesn't provide offline caching support for
 * Storage content, the read operations will always return content unless they fail due to an error
 *
 * <p>
 * About this class
 * ================
 * <p>
 * This class can be looked at as a proxy class that wraps all REST API calls to Oracle MCS for creating, deleting and updating
 * collection objects, as well as uploading and downloading object content. The "setObjectOwnerUserID" method is called from the
 * Storage object when applicationsinvoke the "Storage::querySingleCollectionForUserId" method. The user Id that is passed to the
 * StorageCollection instance is then used for isolated Collections to create , delete, update objects and to upload and download
 * content for the user id specified. For this to work, the authenticated user needs to have READ_WRITE_ALL permission for the
 * isolated collection. The userId cannot be set to "*" or set to an empty string.
 *
 * <p>Be aware that on changing the objectOwnerUserID for a StorageCollection instance at runtime will have an impact on all subsequent
 * create, read, update and delete operations. For example: setting the objectOwnerUserID to non-null will execute collection objects in
 * isolated collections for only this user using StorageCollection::queryStorageObjectsByRange (if the includeOtherUsersObjects is set to
 * false) and StorageCollection::querySingleStorageObjectById.
 * <p>
 * Note: Setting the objectOwnerUserID does not impersonate that user as the authenticated user still works with the privileges of the
 * authenticated usre and not those of the user specified in the objectOwnerUserID property.
 * <p>
 *
  * @author Frank Nimphius, jiawshi
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class StorageCollection {

    private MBEConfiguration mbeConfiguration = null;

    //Key
    private String collectionID = null;
    private String description = null;
    private String eTag = null;
    private int contentLength = 0;

    private Storage storage = null;

    //used to set the scope of the query for isolated queries. For example,to query all user objects you set the id to '*' to
    //qyery a specific user information you set this to the ID o fthe user (not the username). If no value is set and if the 
    //collection is isolated the MCS appends the ID of the authenticated user at the server side. Note that anonymous users don't
    //have access to storage objects.
    private String objectOwnerUserID = null;
    //storage can be user isolated or shared.
    private boolean isUserIsolated = false;
    private MBELogger mLogger = null;

    /**
     * Constructor
     *
     * @param collectionID
     * @param storage
     */
    StorageCollection(String collectionID, Storage storage) {

        this.collectionID = collectionID;
        this.storage = storage;
        this.mbeConfiguration = storage.getMbe().getMbeConfiguration();
        mLogger = this.mbeConfiguration.getLogger();

        mLogger.logFine("Constructor called with arguments: collectionId = " + collectionID + ", Storage != null? " + (storage != null ? "true" : "false"), this.getClass().getSimpleName(),"Constructor");
    }

    public String getCollectionID() {
        return collectionID;
    }

    public String getDescription() {
        return description;
    }


    public String getETag() {
        return eTag;
    }


    public Storage getStorage() {
        return storage;
    }

    /**
     * Used to set the scope of the query for isolated queries. For example,to query all user objects you set the id to '*' to
     * qyery a specific user information you set this to the ID o fthe user (not the username). If no value is set and if the 
     * collection is isolated the MCS appends the ID of the authenticated user at the server side. Note that anonymous users don't
     * have access to storage objects.
     */
    public String getObjectOwnerUserID() {
        return objectOwnerUserID;
    }


    public int getContentLength() {
        return contentLength;
    }

    /*
     * Setters not accessible for application developer
     */

    public boolean isUserIsolated() {
        return isUserIsolated;
    }

    /**
     * INTERNAL API - Applications should not use this method
     * @param description
     */
    void setDescription(String description) {
        this.description = description;
    }

    /**
     * INTERNAL API - Applications should not use this method
     * @param eTag
     */
    void setETag(String eTag) {
        this.eTag = eTag;
    }

    /**
     * Used to set the scope of the query for isolated queries. For example,to query all user objects you set the id to '*' to
     * qyery a specific user information you set this to the ID o fthe user (not the username). If no value is set and if the 
     * collection is isolated the MCS appends the ID of the authenticated user at the server side. Note that anonymous users don't
     * have access to storage objects.
     * @param objectOwnerUserId the ID of the user that originally created an object. Users that want to access other user's isolated objects need READ_ALL or READ_WRITE_ALL permission
     */
    public void setObjectOwnerUserID(String objectOwnerUserId) {
        this.objectOwnerUserID = objectOwnerUserId;
    }

    /**
     * INTERNAL API - Applications should not use this method
     * @param userIsolated
     */
    void setIsUserIsolated(boolean userIsolated) {
        this.isUserIsolated = userIsolated;
    }

    /**
     * INTERNAL API - Applications should not use this method
     * @param contentLength
     */
    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }


    /**
     * Returns true if a StorageObject with the key exists, false otherwise. This method performs a HEAD request to MCS and does not
     * query any content other than the response header telling whether or not the resource exists
     *
     * @param storageObjectId The id of the stored object as defined in the collection
     * @return true / false in case of MCS request success
     * @throws ServiceProxyException in case of transort layer exceptions. If exceptions are thrown by MAF because HTTP headers other than HTTP 200 are returned
     * then an attempt is made to analyze the error message to return "false". Throwing the exception is a last resort. The ServiceProxyException message is JSON
     * formatted and can be parsed into a Java object (OracleMobileError) using the OracleMobileErorrHelper class
     * @throws IllegalArgumentException if the storageObjectId is null or empty
     */
    public boolean contains(String storageObjectId) throws ServiceProxyException, IllegalArgumentException {

        mLogger.logFine( "contains method called with argument: storage object ID = " + storageObjectId, this.getClass().getSimpleName(), "contains");

        if (storageObjectId == null || storageObjectId.length() == 0) {
              throw new IllegalArgumentException("storageObjectId argument cannot be null or empty in call to contains() in StorageCollection.java");
        }

        String uri = generateUriForObject(this.collectionID, storageObjectId);
        mLogger.logFine( "URI generated for the object = " + uri, this.getClass().getSimpleName(), "contains");
        
        try {
            MCSRequest requestObject = new MCSRequest(mbeConfiguration);
            requestObject.setConnectionName(mbeConfiguration.getMafRestConnectionName());
            
            requestObject.setRequestURI(uri);
            requestObject.setHttpMethod(com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest.HttpMethod.HEAD);

             HashMap<String,String> httpHeaders = new  HashMap<String,String>();
            //Authorization will be added automatically either by MAF or this utility. So lets do only the 
            //Mobile Backend Id. The request could have IF-MATCH and IF-NON-MATCH headers set, but in this 
            //context we are only interested in whether or not the object exists
            //httpHeaders.put(HeaderConstants.ORACLE_MOBILE_BACKEND_ID, this.mbeConfiguration.getMobileBackendIdentifier());
            requestObject.setHttpHeaders(httpHeaders);

            MCSResponse mcsResponse = MCSRestClient.sendForStringResponse(requestObject);

            if (mcsResponse.getHttpStatusCode() == StorageConstants.HTTP_200){
                mLogger.logFine( "Object with Id "+storageObjectId+" exists in MCS collection", this.getClass().getSimpleName(), "contains");                                                                     
                return true;
            }
            else{
                mLogger.logFine( "Object with Id "+storageObjectId+" does NOT exist in MCS collection", this.getClass().getSimpleName(), "contains");                                                                     
                return false;
            }

        } catch (Exception e) {
            
            String message = e.getMessage();
            //ensure we have a message
            message = message != null? message : "";
            
            //chances are that the exception is thrown though the response is OK (HTTP 204). So lets check to ensure
            //no false positive
            if(message.contains(""+StorageConstants.HTTP_400) ||
               message.contains(""+StorageConstants.HTTP_401) ||
               message.contains(""+StorageConstants.HTTP_403) ||
               message.contains(""+StorageConstants.HTTP_406)){
                mLogger.logFine( "Object with Id "+storageObjectId+" does NOT exist in MCS collection", this.getClass().getSimpleName(), "contains");                                                                     
               return false;            
            }
            //if the HTTP codes are not found, throw an exception as chances are that the 
            //REST call failed
            throw new ServiceProxyException(e.getLocalizedMessage(), ServiceProxyException.ERROR);
        }
    }


    /**
     * Returns a list of StorageObject for a collection. Each StorageObject holds metadata information about the object in
     * the MCS collection. It does not hold the data object itself as it is conidered to expensive to load all the content
     * without offline-synchronization and caching. MAF application developers would use the information in the StorageObject
     * to access the object link for querying in a separate call.
     *
     * @see getObject (String objectLink)
     *
     * Decision cases:
     *
     * 1. If the collection is shared collection, then it return all the objects
     * 2. If the collection is user-isolated collection, and allObjects is false, then it returns the objects which belong to the current user
     * 3. If the collection is user-isolated collection, and allObjects is true, then it returns all the objects in the collection. The objects might belong to other users and the current user MUST have READ_ALL or READ_WRITE_ALL permission.
     *
     * @param offset Specify the index where you want to start browsing the list of items. (0 indicates the first item
     * in the list). The response from MCS will contain the offset used, and also a link to get the previous
     * set of items. This value must be 0 or above. It cannot be negative.
     * @param fetchSize The maximum number of items to be returned for each MCS access. If the requested limit is too large, 
     * MCS will apply its own limit (100 by default).
     * @param includeOtherUsersObjects setting for user isolated collections. If true and user has READ_ALL or READ_WRITE_ALL privilege then
     * all user objects - also those *not* owned by the authenticated user - are returned. The common use case for this parameter is to set it 
     * to false
     * @param [optional] queryFilter a case insensitive string to filter the result of a collection object query by a partial match of the filter value in the id, name, createdBy or modifiedBy attribute. Value can be passed as null.
     * @return List&lt;StorageObject&gt;
     * @throws IllegalArgumentException in case of i) offset < 0 , ii) fetchSize <0
     */
    public List<StorageObject> queryStorageObjectsByRange(int offset, int fetchSize, boolean includeOtherUsersObjects, String queryFilter) throws ServiceProxyException, IllegalArgumentException {
        
        mLogger.logFine("Arguments: offset = "+offset+", limit = "+fetchSize, this.getClass().getSimpleName(), "queryStorageObjectsByRange");
        
        if (offset < 0 || fetchSize < 0) throw new IllegalArgumentException("The \"offset\" and \"limit\" arguments cannot be <0 and the \"mcsCallback\" argument cannot be null");

        //instance variable to hold the StorageObjects
        List<StorageObject> result = new ArrayList<StorageObject>();

        //URI to access the MCS collection identified by the collection ID. If all objects in the collection should be
        //queried and the storage is user isolated then an "*" is added for the user query parameter. Otherwise, if only
        //a specific user object should be queried, the userId is appended. The queryFilter string filter is used to 
        String uri = generateUriForObjectArray(this.collectionID, includeOtherUsersObjects, queryFilter);

        boolean hasMore = true;
        int count = 0;

        while (hasMore) {
            //get information about the storage collection and whether it has content
            Map<String, Object> singlePageInfo = getSinglePageInfo(uri);
                        
            //return empty collection if the request did not succeed
            if (singlePageInfo == null || singlePageInfo.isEmpty()) {
                mLogger.logFine( "singlePageInfo did return empty", this.getClass().getSimpleName(), "queryStorageObjectsByRange");                                                                                     
                break;
            }
            
            mLogger.logFine( "singlePageInfo returned Map with content: "+MapUtils.dumpObjectProperties(singlePageInfo), this.getClass().getSimpleName(), "queryStorageObjectsByRange");                                                                     
            

            //a valid collection was found and has content. The singlePageInfo returns a MAP with information 
            //about the object. One information is a list of URI pointing to the objects in this list. 
            hasMore = ((Boolean) singlePageInfo.get("hasMore")).booleanValue();

            uri = (String) singlePageInfo.get("nextUri");
            int currentPageSize = ((Integer) singlePageInfo.get("count")).intValue();
                        
            JSONArray itemsArray = (JSONArray) singlePageInfo.get("items");
            
            /*
             * The items response returned from MCS has the following JSON array structure
             * 
             *  "items": [
             *  {
             *  "id": "6a66ea18-6f9c-4c8d-a23d-a345d48f740c",
             *  "name": "png.png",
             *  "user": "e6bda38e-635a-42d7-a509-a5da8a4cf50a",
             *  "contentLength": 10549,
             *  "contentType": "image/png",
             *  "eTag": "\"1\"",
             *  "createdBy": "uimcs",
             *  "createdOn": "2015-06-17T08:03:24Z",
             *  "modifiedBy": "uimcs",
             *  "modifiedOn": "2015-06-17T08:03:24Z",
             *  "links": [
             *  {
             *  "rel": "canonical",
             *  "href": "/mobile/platform/storage/collections/PrivateContent/objects/6a66ea18-6f9c-4c8d-a23d-a345d48f740c?user=e6bda38e-635a-42d7-a509-a5da8a4cf50a"
             *  },
             *  {
             *  "rel": "self",
             *  "href": "/mobile/platform/storage/collections/PrivateContent/objects/6a66ea18-6f9c-4c8d-a23d-a345d48f740c"
             *  }
             *  ]
             *  }, 
             *  { ... next item ...
             *  }
             *  ]
             * 
             */            
            mLogger.logFine( "Populating StorageObjects from items in Collection. There were "+itemsArray.length()+" items found.", this.getClass().getSimpleName(), "queryStorageObjectsByRange");  
            for (int i = 0; i < itemsArray.length(); i++) {

                try {
                    JSONObject itemObject = itemsArray.getJSONObject(i);
                    String itemId = itemObject.getString(StorageConstants.OBJECT_PROPERTY_TAG_ID);                    
                    StorageObject storageObject = new StorageObject(itemId);
                    mLogger.logFine( "Creating StorageObject for id="+itemId+". Content: "+storageObject.toString(), this.getClass().getSimpleName(), "queryStorageObjectsByRange");  
                    storageObject.updateProperties(itemObject);     
                    result.add(storageObject);
                    
                } catch (Exception e) {
                    //handle exception gracefully
                    this.handleExceptions(e, uri);                    
                }

            }            
            count += currentPageSize;
        }
        return result;
    }
    
    
    /**
     * Reads a single object from storage
     *
     * @param objectId
     * @return instance of StorageObject
     * @throws IllegalArgumentException if the objectId is null
     */
    public StorageObject querySingleStorageObjectById(String objectId) throws IllegalArgumentException, ServiceProxyException {
        if(objectId == null || objectId.length() == 0) {
            throw new IllegalArgumentException("Argument \"id\" in call to getSingleCollectionObject cannot be null or empty");
        }
        
        mLogger.logFine( "Querying object with Id: "+objectId, this.getClass().getSimpleName(), "querySingleStorageObjectById");          
        StorageObject object = new StorageObject(objectId);
       
        String url = generateUriForObject(this.collectionID, objectId);
       
        mLogger.logFine( "Storage Object URI: "+url, this.getClass().getSimpleName(), "querySingleStorageObjectById");  
        fetchSingleObject(object, url);
        return object;
    }



    /**
     * Create a new object in the server side MCS collection. The object Id will be auto generated by the MCS collection.
     * The MimeType in the StorageObject is used in the content-type HTTP parameter sent with the upload request. Oracle 
     * MCS server side has been tested for POST with the following Mime Types
     * <p>
     *  image/jpeg
     *  image/png
     *  text/plain
     *  text/xml
     *  text/html
     *  application/msword
     *  application/pdf
     *  application.zip
     *  image/gif
     *  image/x-ms-bmp
     *  image/gif
     *  video/mp4
     *  multipart/formdata
     * </p>
     * @param object metadata information about this object that will be sent as part of the request header
     * to MCS. Missing information will be auto-generated. Its recommended though to set the Oracle name, which
     * the display name of the object
     * @param byteContent the object to save in the server side collection as an array of bytes
     * @return The StorageObject object that contains the updated information based on the uploaded content. Changes are
     * to be extected e.g. for the modification date, the content length, the mime type
     * @throws IllegalArgumentException if storage object or byteContent in argument is null
     */
    public StorageObject createObject(StorageObject object, byte[] byteContent) throws IllegalArgumentException,
                                                                                       ServiceProxyException {
        
        //we cannot perform this action if there is no object and no information to it
        if(object == null || byteContent == null)
            throw new IllegalArgumentException("The storage object and byteContent cannot be null");

        //obtains the URI for the update
        String url = generateUriForObjectArray(this.collectionID);
        mLogger.logFine( "Uri for Collection object is: "+url, this.getClass().getSimpleName(), "createObject");
        return writeObject(url, "POST", object, byteContent, null);
    }
    
    
    /**
     * Create a new object in the server side MCS collection. The object Id will be auto generated by the MCS collection.
     * The MimeType in the StorageObject is used in the content-type HTTP parameter sent with the upload request. Oracle 
     * MCS server side has been tested for POST with the following Mime Types
     * <p>
     *  image/jpeg
     *  image/png
     *  text/plain
     *  text/xml
     *  text/html
     *  application/msword
     *  application/pdf
     *  application.zip
     *  image/gif
     *  image/x-ms-bmp
     *  image/gif
     *  video/mp4
     *  multipart/formdata
     * </p>
     * @param displayName the name of the object as it should be displayed on an application screen
     * @param contentType a valid MIME type describing the content like image/jpeg
     * @param byteContent the byte[] array of the content that should be loaded to MCS    
     * @return StorageObject with the metadata of the updated object in MCS
     * @throws IllegalArgumentException if any argument is empty or null
     * @throws ServiceProxyException Application or REST transport layer errors
     */
    public StorageObject createObject(String displayName, String contentType, byte[] byteContent) throws IllegalArgumentException, ServiceProxyException {
        
        if(displayName == null || displayName.isEmpty()  || contentType == null)
            throw new IllegalArgumentException("objectId, displayName and contentType arguments cannot be null or empty");
        
        //create a storage object with a dummy ID. The id will be overridden in 
        //MCS and replaced with a generic, uniwue, id
        StorageObject sObject = new StorageObject("_dummy_");
        sObject.setDisplayName(displayName);
        sObject.setContentType(contentType);
               
        //obtains the URI for the update
        String url = generateUriForObjectArray(this.collectionID);
        mLogger.logFine( "Uri for Collection object is: "+url, this.getClass().getSimpleName(), "createObject");
        return writeObject(url, "POST", sObject, byteContent, null);
    }
    
    
    /**
    * Updates an existing object identified by the canonical URI. If the collection is isolated and the object is not
    * owned by the authenticated user, READ_WRITE_ALL permissions must be granted to the user. 
    * <p>
    * The MimeType in the StorageObject is used in the content-type HTTP parameter sent with the upload request. Oracle 
    * MCS server side has been tested for POST with the following Mime Types
    * <p>
    *  image/jpeg
    *  image/png
    *  text/plain
    *  text/xml
    *  text/html
    *  application/msword
    *  application/pdf
    *  application.zip
    *  image/gif
    *  image/x-ms-bmp
    *  image/gif
    *  video/mp4
    *  multipart/formdata
    * </p>
    * <p>
    * @param canonicalURI the URI pointng to an object in a collection. This URI may contain the ?userId=.... query parameter for an object in an isolated collection
    * @param displayName the name of the object as it should be dispalyed
    * @param contentType a valid MIME type describing the content like image/jpeg
    * @param byteContent the byte[] array of the content that should be loaded to MCS
    * @param optimisticLockingInfo a HashMap containing key/value pairs for e.g. If-Modified-Since, If-Unmodified-Since, If-Match, If-None-Match. This value can be set to null if no ETag based locking should be enforced
    * @return StorageObject with the metadata of the updated object in MCS
    * @throws IllegalArgumentException if any argument is empty or null (except optimisticLockingInfo, which can be null)
    * @throws ServiceProxyException Application or REST transport layer errors
    */
    public StorageObject updateCollectionObjectWithURI(String canonicalURI, String displayName, String contentType, byte[] byteContent, HashMap<String,String>optimisticLockingInfo) throws IllegalArgumentException,ServiceProxyException{
                
        if(canonicalURI == null || byteContent == null)
            throw new IllegalArgumentException("The storage object and/or byteContent cannot be null in a call to updateCollectionObjectWithURI");

        mLogger.logFine( "canonicalURI argument is: "+canonicalURI, this.getClass().getSimpleName(), "updateCollectionObjectWithURI");
        
        //get objectId from URI        
        String objectId = canonicalURI.toString();
        int indexOfQueryParamStart = objectId.indexOf("?");
        
        if(indexOfQueryParamStart > -1){
            objectId = objectId.substring(0,indexOfQueryParamStart);
        }
        
        int indexOfObjectsPathKey = objectId.indexOf("/objects/");
        
        if (indexOfObjectsPathKey > -1){
             objectId = objectId.substring(indexOfObjectsPathKey+10);
        }
        
        StorageObject sObject = new StorageObject(objectId);
        sObject.setDisplayName(displayName);
        sObject.setContentType(contentType);
        
        return writeObject(canonicalURI, "PUT", sObject, byteContent, optimisticLockingInfo);                
    }

    /**
     * Create a new object or updates an existing object in the server side MCS collection. The object Id must be provided 
     * in the StorageObject object
     * <p>
     * The MimeType in the StorageObject is used in the content-type HTTP parameter sent with the upload request. Oracle 
     * MCS server side has been tested for POST with the following Mime Types
     * <p>
     *  image/jpeg
     *  image/png
     *  text/plain
     *  text/xml
     *  text/html
     *  application/msword
     *  application/pdf
     *  application.zip
     *  image/gif
     *  image/x-ms-bmp
     *  image/gif
     *  video/mp4
     *  multipart/formdata
     * </p>
     * <p>
     *
     * @param object metadata information about this object that will be sent as part of the request header
     * to MCS. Missing information will be auto-generated (except for the Id, which must be provided).
     *
     * @param byteContent the object to save in the server side collection as an array of bytes
     * @param [optional] optimisticLockingInfo HashMap to add ETag header parameters to e.g. only create objects if they 
     *        don't exist or only if the object in MCS is of an older version. Please check the Storage API documentation 
     *        for supported ETag and modification headers that allow implementing optimistic locks.
     * @return The StorageObject object that contains the updated information based on the uploaded content.
     * Changes are to be extected e.g. for the modification date, the content length, the mime type
     * @throws IllegalArgumentException if storage object or byteContent in argument is null
     */
    public StorageObject createOrUpdateObject(StorageObject object, byte[] byteContent, HashMap<String,String>optimisticLockingInfo) throws IllegalArgumentException,ServiceProxyException {
        if(object == null || byteContent == null)
            throw new IllegalArgumentException("The storage object and byteContent cannot be null");

        if(object.getID() == null || object.getID().length() == 0)
            throw new IllegalArgumentException("Id for the storage object to be put cannot be null or empty");

        String url = generateUriForObject(this.collectionID, object.getID());
        mLogger.logFine( "Uri for Collection object is: "+url, this.getClass().getSimpleName(), "createOrUpdateObject");
        return writeObject(url, "PUT", object, byteContent, optimisticLockingInfo);
    }
    
    
    
    
    /** 
     * Create a new object or updates an existing object in the server side MCS collection. The object Id must be provided 
     * in the StorageObject object
     * <p>
     * The MimeType in the StorageObject is used in the content-type HTTP parameter sent with the upload request. Oracle 
     * MCS server side has been tested for POST with the following Mime Types
     * <p>
     *  image/jpeg
     *  image/png
     *  text/plain
     *  text/xml
     *  text/html
     *  application/msword
     *  application/pdf
     *  application.zip
     *  image/gif
     *  image/x-ms-bmp
     *  image/gif
     *  video/mp4
     *  multipart/formdata
     * </p>
     * <p>
     * @param objectId the unique Id of the object to create or update
     * @param displayName the name of the object as it should be dispalyed
     * @param contentType a valid MIME type describing the content like image/jpeg
     * @param byteContent the byte[] array of the content that should be loaded to MCS
     * @param optimisticLockingInfo a HashMap containing key/value pairs for e.g. If-Modified-Since, If-Unmodified-Since, If-Match, If-None-Match. This value can be set to null if no ETag based locking should be enforced
     * @return StorageObject with the metadata of the updated object in MCS
     * @throws IllegalArgumentException if any argument is empty or null (except optimisticLockingInfo, which can be null)
     * @throws ServiceProxyException Application or REST transport layer errors
     */
    public StorageObject createOrUpdateObject(String objectId, String displayName, String contentType, byte[] byteContent, HashMap<String,String>optimisticLockingInfo) throws IllegalArgumentException,ServiceProxyException {
        
        if(objectId == null || objectId.isEmpty() || displayName == null || displayName.isEmpty()  || contentType == null)
            throw new IllegalArgumentException("objectId, displayName and contentType arguments cannot be null or empty");
        
        StorageObject sObject = new StorageObject(objectId);
        sObject.setDisplayName(displayName);
        sObject.setContentType(contentType);
       
       //delegate request
       return createOrUpdateObject(sObject, byteContent, optimisticLockingInfo);
    }

     /**
     * Uploads a single object to a collection in MCS
     * @param uri The object URI in a collection
     * @param httpMethod PUT (for Create or Update), POST (for Create)
     * @param storageObject The metadata object that descibes the object.
     * @param payload The binary[] content to store in the collection
     * @param [optional } etagMap HashMap to add the headers parameters and the eTag parameter property and value to e.g. only create objects if they don't exist or if the object is of an older version
     * @return StorageObject, the updated object after the create or udate post of the binary content
     * @throws ServiceProxyException in case of MCS application errors or service exceptions on the transport layer
     * @throws IllegalArgumentException If storageObject or payload argument is null. Or if the httpMethod is PUT and the storageObject doesn't have an id specified
     */
     private StorageObject writeObject(String uri, String httpMethod, StorageObject storageObject, byte[] payload, HashMap<String,String> etagHashMap) throws ServiceProxyException, IllegalArgumentException {
         
          mLogger.logFine("Trying to create or update Storage object for collection: "+this.getCollectionID(), this.getClass().getSimpleName(), "writeObject");        
         
         if(storageObject == null || payload == null){
             throw new IllegalArgumentException("storageObject and / or payload argument cannot be NULL in call to MCS Storage");
         }
          
        if(httpMethod.equalsIgnoreCase("PUT") && storageObject.getID()==null ){
            throw new IllegalArgumentException("The storageObject object ID property cannot be null for PUT method calls to MCS Storage");
        }
                
        MCSRequest requestObject = null; 
                
        
        try {            
            //ceck if network is available
            if(!MAFUtil.isNetworkAccess()){
                mLogger.logFine( "No online connection detected for "+httpMethod+" call to  "+uri, this.getClass().getSimpleName(), "writeObject");
                throw new ServiceProxyException("The device is not online. An online connection is required", ServiceProxyException.ERROR);
            }

            requestObject = new MCSRequest(this.mbeConfiguration);
            mLogger.logFine( "Setting URI to "+uri, this.getClass().getSimpleName(), "writeObject");
            requestObject.setRequestURI(uri);
            
                       
            if(httpMethod.equalsIgnoreCase("PUT")){
                requestObject.setHttpMethod(com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest.HttpMethod.PUT);
                mLogger.logFine( "PUT configured for "+uri, this.getClass().getSimpleName(), "writeObject");
            }
            else{
                requestObject.setHttpMethod(MCSRequest.HttpMethod.POST);
                mLogger.logFine( "POST configured for "+uri, this.getClass().getSimpleName(), "writeObject");
            }
                      
             HashMap<String,String> headers = new  HashMap<String,String>();            
            
            //add Etag map passed by the MAF developer
            if(etagHashMap != null && !etagHashMap.isEmpty()){
                mLogger.logFine( "ETag HashMap content is: "+MapUtils.dumpStringProperties(etagHashMap), this.getClass().getSimpleName(), "writeObject");
                headers.putAll(etagHashMap);
            }
            
            //Set the "Oracle-Mobile-NAME" header
            if(storageObject.getDisplayName() != null){
                headers.put(StorageConstants.HEADER_PROPERTY_TAG_DISPLAYNAME, storageObject.getDisplayName());
            }
            
            if(storageObject.getContentType() != null && !storageObject.getContentType().isEmpty()){
               headers.put(HeaderConstants.CONTENT_TYPE_HEADER, storageObject.getContentType());   
            }
        
            if (payload != null){
                headers.put(HeaderConstants.CONTENT_LENGTH, (new Integer(payload.length)).toString());
            }
                                   
            
            // headers.put(StorageConstants.CONTENT_LENGTH_HEADER, new Integer(payload.length).toString());                        
            //PUT can be used to update an object. In this case the objectId is specified 
            //by the sender
            if(httpMethod.equalsIgnoreCase("PUT")){
                mLogger.logFine( "PUT method requested. Adding object ID from Storage object. ID is: "+storageObject.getID(), this.getClass().getSimpleName(), "writeObject");
                headers.put(StorageConstants.OBJECT_PROPERTY_TAG_ID, storageObject.getID());
            }
            
            //headers.put(HeaderConstants.ORACLE_MOBILE_BACKEND_ID, this.mbeConfiguration.getMobileBackendIdentifier());
            requestObject.setHttpHeaders(headers);
            
            mLogger.logFine( "Header value key/value pairs in Storage Collection: "+MapUtils.dumpStringProperties(headers) , this.getClass().getSimpleName(), "writeObject");
            
            requestObject.setConnectionName(this.mbeConfiguration.getMafRestConnectionName());
            
            
            //send payload array[]?to Object                  
            requestObject.setPayload(payload);            
                        
            MCSResponse mcsResponse = MCSRestClient.sendForStringResponse(requestObject);
            
            if(mcsResponse != null && (mcsResponse.getHttpStatusCode() == StorageConstants.HTTP_200 || 
                                          mcsResponse.getHttpStatusCode() == StorageConstants.HTTP_201)){
              
              mLogger.logFine( "Object " + (httpMethod.equalsIgnoreCase("POST")==true?"created": "created or updated") +" response message is:" +mcsResponse.getMessage(),this.getClass().getSimpleName(), "writeObject");
              
              String jsonString = (String) mcsResponse.getMessage();
              
              if(jsonString != null && !jsonString.isEmpty()){
                 JSONObject jsonObject = new JSONObject(jsonString);
                 storageObject.updateProperties(jsonObject);     
              }
              else{
                  mLogger.logFine( "REST response does contain a null or empty string message.",this.getClass().getSimpleName(), "writeObject");
              }
            }
            else if(mcsResponse != null){
                mLogger.logFine( "MCS application error found. Error Code = "+mcsResponse.getHttpStatusCode()+", Message: "+mcsResponse.getMessage(), this.getClass().getSimpleName(), "writeObject");
                throw new ServiceProxyException(mcsResponse.getHttpStatusCode(), (String)mcsResponse.getMessage(), mcsResponse.getHeaders());
            }
            
        } catch (Exception e) {
            
            //check if exception contains Oracle MCSS application error response. If not, throw ServiceProxyException
            mLogger.logFine( "Exception Handling: calling handleExceptions(...) method to find Oracle MCS response codes in exception message", this.getClass().getSimpleName(), "writeObject");
            this.handleExceptions(e, requestObject.getRequestURI());
        }

        return storageObject;
    }
     

    /**
     * Delete the server side storage objects
     *
     * @param objectId the unique ID associated with the server side collection object to removeCollectionObject
     * @param [optional} etagHashMap HashMap to add the header parameters and the eTag parameter property and value to e.g. only remove object if it doesn't match the eTag sent with the request
     * @return true / false if the object could be deleted
     * @throws IllegalArgumentException if objectId is null or empty
     * @throws ServiceProxyException for application and system error
     */
    public boolean removeCollectionObject(String objectId, HashMap<String,String> etagHashMap) throws IllegalArgumentException, ServiceProxyException {
        
        if(objectId == null || objectId.length() == 0)
            throw new IllegalArgumentException("Id for the storage object to be removed cannot be null or empty");
        
        String caconicalLink = generateUriForObject(this.collectionID, objectId);
        return removeCollectionObjectWithURI(caconicalLink,etagHashMap);
    }

    /**
        * 
        * @param caconicalLink
        * @param etagHashMap
        * @return
        * @throws IllegalArgumentException
        * @throws ServiceProxyException
        */
       public boolean removeCollectionObjectWithURI(String caconicalLink, HashMap<String,String> etagHashMap) throws IllegalArgumentException, ServiceProxyException {
           
           if(caconicalLink == null || caconicalLink.length() == 0)
               throw new IllegalArgumentException("caconicalLink for the storage object to be removed cannot be null or empty");
           
           
           MCSRequest requestObject = null;
           
           mLogger.logFine("URI of collection object to be removed = "+caconicalLink, this.getClass().getSimpleName(), "removeCollectionObject");

           try {
               requestObject = new MCSRequest(this.mbeConfiguration);
               requestObject.setRequestURI(caconicalLink);
               requestObject.setHttpMethod(com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest.HttpMethod.DELETE);
               
                HashMap<String,String> headers = new  HashMap<String,String>();
               
               //if the MAF developer provides a MAP wih the ETag and the IF-MATCH or IF-NONE-MATCH header properies then add it here
               if(etagHashMap != null && !etagHashMap.isEmpty()){
                   headers.putAll(etagHashMap);
               }
               
               //headers.put(HeaderConstants.ORACLE_MOBILE_BACKEND_ID, this.mbeConfiguration.getMobileBackendIdentifier());            
               //authorization and accept headers are automatically added by MAF or MAF MCS Utility
               requestObject.setHttpHeaders(headers);            
               requestObject.setConnectionName(this.mbeConfiguration.getMafRestConnectionName());
               //no payload to set as the object Id is part of the request URI
               requestObject.setPayload("");
               MCSResponse mcsResponse = MCSRestClient.sendForStringResponse(requestObject);    
               
               if (mcsResponse != null && mcsResponse.getHttpStatusCode() == StorageConstants.HTTP_204) {                
                   return true;
               }
               else if(mcsResponse != null){
                   mLogger.logFine( "MCS application error found. Error Code = "+mcsResponse.getHttpStatusCode()+", Message: "+mcsResponse.getMessage(), this.getClass().getSimpleName(), "removeCollectionObject");
                   throw new ServiceProxyException(mcsResponse.getHttpStatusCode(), (String)mcsResponse.getMessage(), mcsResponse.getHeaders());
               }

           } catch (Exception e) {
               //check if exception contains Oracle MCSS application error response. If not, throw ServiceProxyException
               mLogger.logFine( "Exception Handling ::: calling determineOracleMobileErrorFromException(...) to find Oracle MCS response codes in exception message", this.getClass().getSimpleName(), "removeCollectionObject");
               this.handleExceptions(e, requestObject.getRequestURI());
           } 
           
           return false;
       }


    /**
     * Composes the URI for accessing an object in a collection contained in a remote MCS Storage instance
     * @param collectionId the name of the collection
     * @param objectId the object identified
     * @return a REST URI
     */
    private String generateUriForObject(String collectionId, String objectId) {
        String uri = StorageConstants.STORAGE_RELATIVE_URL + "/" + encodeName(collectionId) + "/objects/" + encodeName(objectId);
        
        if (this.isUserIsolated() && this.objectOwnerUserID != null && !this.objectOwnerUserID.isEmpty()) {           
            uri += StorageConstants.USER_ID_KEY + this.objectOwnerUserID;
        }
        mLogger.logFine( "Collection is isolated and userId setting is found: "+uri, this.getClass().getSimpleName(), "generateUriForObject");
        return uri;
    }

    /**
     * Composes the URI for accessing a collection in a remote MCS Storage instance
     * @param collectionId the name of the collection
     * @return a REST URI
     */
    private String generateUriForObjectArray(String collectionId) {
        String uri = StorageConstants.STORAGE_RELATIVE_URL + "/" + encodeName(collectionId) + "/objects";
        if (this.isUserIsolated() && this.objectOwnerUserID != null && !this.objectOwnerUserID.isEmpty())
            uri += StorageConstants.USER_ID_KEY + this.objectOwnerUserID;
        mLogger.logFine( "Collection is isolated and userId is found: "+uri, this.getClass().getSimpleName(), "generateUriForObjectArray");
        return uri;
    }

    /**
     * Composes a URI to return objects from a MCS Storage collection. If the allObjects parameter is set to true, then
     * this will return all objects. Otherwise only objects that are owned by the specified user (thus.userId) are returned
     *
     * @param collectionId The name of the collection
     * @param allObjects true/false dependent
     * @param [optional] qFilter a case insensitive string to filter the result of a collection object query by a partial match of the filter value in the id, name, createdBy or modifiedBy attribute
     * @return URI
     */
    private String generateUriForObjectArray(String collectionId, boolean allObjects, String qFilter) {

        String uri = StorageConstants.STORAGE_RELATIVE_URL + "/" + encodeName(collectionId) + "/objects";

        //userId key is mandatory for isolated collections. If data for no specific users is queried, then the userId is 
        //provided as '*'
        if (this.isUserIsolated() && allObjects){
            uri += StorageConstants.USER_ID_KEY + "*";
            mLogger.logFine( "Collection is isolated and all User objects should be queried: "+uri, this.getClass().getSimpleName(), "generateUriForObjectArray");
        }
        else if (this.isUserIsolated() && this.objectOwnerUserID != null){
            uri += StorageConstants.USER_ID_KEY + this.objectOwnerUserID;
            mLogger.logFine( "Collection is isolated and User id is set: "+uri, this.getClass().getSimpleName(), "generateUriForObjectArray");
        }
        
        //add the request filter taking the value of the qFilter parameter and adding it with a "?" 
        //or "+" to the uri, ended with a "%" for partial searches. 
        if (qFilter != null && !qFilter.isEmpty()){
           if(uri.contains(StorageConstants.USER_ID_KEY)){
                 uri=uri+"&q="+qFilter+"%";
               mLogger.logFine( "Query filter is set after user id: "+uri, this.getClass().getSimpleName(), "generateUriForObjectArray");
           }
           else{
              uri=uri+"?q="+qFilter+"%";
              mLogger.logFine( "Query filter is set: "+uri, this.getClass().getSimpleName(), "generateUriForObjectArray");
           }            
        }        
        
        return uri;
    }

    /**
     * UTRL encoding of name strings. Translates blanks " " into "%20" and then URL encodes Query Strings
     * @param name
     * @return decoded String
     * */
    private String encodeName(String name) {
        String encodedName = "";
        try {                 
            String _blankEncodedString = name.replace(" ","%20");
            int indxOfQueryParameter = _blankEncodedString.indexOf("?");
            
            if(indxOfQueryParameter >0){
                encodedName = URLEncoder.encode(_blankEncodedString.substring(indxOfQueryParameter) , "UTF-8");
                encodedName = _blankEncodedString+encodedName;
            }
            else{
                encodedName = _blankEncodedString;
            }
            
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodedName;
    }


    /**
     * Parses the result of the REST call to MCS for the URI of items in the collection and possible NEXT links
     *
     * @param uri
     * @return Map of links and item references plus information about th enumber of items and if there are items in the collection
     * @throws ServiceProxyException in case of application errors (non HTTP 2XX errors), system errors or transport layerexceptions. The ServiceProxyException
     * message is JSON formatted and can be parsed into a Java object (OracleMobileError) using the OracleMobileErorrHelper class
     */
    private Map<String, Object> getSinglePageInfo(String uri) throws ServiceProxyException {

        mLogger.logFine("URI in call to getSinglePageInfo is: "+uri, this.getClass().getSimpleName(), "getSinglePageInfo");  
        
        MCSRequest requestObject = null;
        
        Map<String, Object> info = new  HashMap<String,Object>();

        try {

            requestObject = new MCSRequest(this.mbeConfiguration);
            requestObject.setConnectionName(this.mbeConfiguration.getMafRestConnectionName());
            requestObject.setHttpMethod(com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest.HttpMethod.GET);
            requestObject.setRequestURI(uri);
            requestObject.setPayload("");
             HashMap<String,String> httpHeaders = new  HashMap<String,String>();
            httpHeaders.put("Accept", "application/json");
            requestObject.setHttpHeaders(httpHeaders);

            //send request for application/json payload (defaulted in RestClient) to obtain list of
            //storage object descriptions. The call will receive a JSON string payload if there is
            //a valid collection addressed
            MCSResponse mcsResponse = MCSRestClient.sendForStringResponse(requestObject);

            if (mcsResponse!= null && mcsResponse.getHttpStatusCode() == StorageConstants.HTTP_200 &&
              mcsResponse.getMessage() != null) {                 
                
                mLogger.logFine("MCS request returns successful with payload: "+mcsResponse.getMessage(), this.getClass().getSimpleName(), "getSinglePageInfo");  
                
                JSONObject json = new JSONObject((String) mcsResponse.getMessage());

                //HasMore
                boolean hasMore = false;
                //Handle the circumstances when 'hasMore' field is missing
                if (!json.isNull("hasMore"))
                    hasMore = Boolean.getBoolean(json.getString("hasMore"));
                info.put("hasMore", hasMore);

                //count
                int count = json.getInt("count");
                info.put("count", count);

                //uris to items within the collection
                List<String> uris = new ArrayList<String>();
                
                JSONArray items = json.getJSONArray("items");

                info.put("items", items);
                 
                 //nextUri
                 JSONArray links = json.getJSONArray("links");
                 for (int i = 0; i < links.length(); i++) {
                    JSONObject singleLink = links.getJSONObject(i);
                    if (singleLink.getString("rel").equals("next")) {
                        info.put("nextUri", singleLink.getString("href"));
                        break;
                    }
                }
            }

            else if (mcsResponse != null){
                //no collection was found
                OracleMobileError errorCtx = mcsResponse.getOracleErrorMessage();
                String errorMessage =
                     "The request to " + uri + " failed with http error code " +
                    mcsResponse.getHttpStatusCode();

                if (errorCtx != null) {
                    errorMessage =
                        errorMessage + "\n Error Title: " + errorCtx.getTitle() + "\n Error Detail: " +
                        errorCtx.getDetail() + "\n Error Path: " + errorCtx.getOracleErrorPath();
                }

                this.mbeConfiguration.getLogger().logFine(errorMessage, this.getClass().getSimpleName(),
                                                          "getSinglePageInfo");
                //return empty map
                return info;
            }
        } catch (Exception e) {            
            handleExceptions(e, requestObject.getRequestURI());            
        }
        return info;
    }

    /**
     * URL decoding of name strings
     * @param name
     * @return decoded String
     */
    private String decodeName(String name) {
        String decodedName = "";
        try {
            decodedName = URLDecoder.decode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return decodedName;
    }


    private boolean isInThisPage(int count, int index, int offset, int limit) {
        return (count + index >= offset) && (count + index < offset + limit);
    }


    /**
     * Fetches information about a single item (storage object) in a MCS collection
     *
     * @param storageObject StorageObjetInfo object instance to be populated with result content
     * @param storageObjectURI The URI that identifies a single object in a collection
     * @throws ServiceProxyException in case of application errors (non HTTP 2XX errors), system errors or 
     * transport layer exceptions. The ServiceProxyException message is JSON formatted and can be parsed 
     * into a Java object (OracleMobileError) using the OracleMobileErorrHelper class
     * @throws IllegalArgumentException if one of the two arguments is null
     */
    private void fetchSingleObject(StorageObject storageObject, String storageObjectURI) throws ServiceProxyException, IllegalArgumentException {
       
        MCSRequest requestObject = null;
        try {
            
            if(storageObject == null ||  storageObjectURI == null || storageObjectURI.isEmpty()){
                throw new IllegalArgumentException("Neither the StorageObject object argument nor the storageObjectURI can be null or empty");
            }
            
            mLogger.logFine( "Fetching object for storage URI: "+storageObjectURI, this.getClass().getSimpleName(), "fetchSingleObject");  

            requestObject = new MCSRequest(this.mbeConfiguration);
            
            requestObject.setConnectionName(mbeConfiguration.getMafRestConnectionName());
            
            //get request returns headers only
            requestObject.setHttpMethod(com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest.HttpMethod.HEAD);
            
            requestObject.setRequestURI(storageObjectURI);

             HashMap<String,String> httpHeaders = new  HashMap<String,String>();
            
            //accept header will be defaulted by MAF MCS Utility to application/json. The Authorization
            //is also automatically added either by MAF or this utility. So all that needs to be done is
            //to set the moble backend Id. As we want to read the storageObject info, we also don't add IF-MATCH 
            //or other eTAG headers
            //httpHeaders.put(HeaderConstants.ORACLE_MOBILE_BACKEND_ID, this.mbeConfiguration.getMobileBackendIdentifier());            
            requestObject.setHttpHeaders(httpHeaders);

            //REST call for binary content (byte array)
            MCSResponse mcsResponse = MCSRestClient.sendForByteResponse(requestObject);


            if (mcsResponse != null && mcsResponse.getHttpStatusCode() == StorageConstants.HTTP_200) {
                //Fetch the headers
                
                mLogger.logFine( "Request returns folowing information for object URI: "+storageObjectURI, this.getClass().getSimpleName(), "fetchSingleObject");  
                mLogger.logFine( MapUtils.dumpStringProperties(mcsResponse.getHeaders()), this.getClass().getSimpleName(), "fetchSingleObject");  
                
                storageObject.updateProperties(mcsResponse.getHeaders());
                
            }
            else if(mcsResponse != null){
                
                OracleMobileError errorCtx = mcsResponse.getOracleErrorMessage();
                String errorMessage =  "The Head request to " + storageObjectURI + " failed with http error code " +mcsResponse.getHttpStatusCode();

                if (errorCtx != null) {
                    errorMessage =  errorMessage + "\n Error Title: " + errorCtx.getTitle() + "\n Error Detail: " +
                                    errorCtx.getDetail() + "\n Error Path: " + errorCtx.getOracleErrorPath();
                }

                this.mbeConfiguration.getLogger().logFine(errorMessage, this.getClass().getSimpleName(),"fetchSingleObject");
                
                throw new ServiceProxyException(mcsResponse.getHttpStatusCode(), (String) mcsResponse.getMessage(), mcsResponse.getHeaders());
            }
            else{
                //no response returned, which should lead to an exception to be thrown anyway. 
                NullPointerException npe = new NullPointerException();
                npe.setMessage("Response object for REST call: "+ requestObject.getRequestURI()+"returned as null");
                throw new ServiceProxyException(npe,ServiceProxyException.ERROR);
            }

        } catch (Exception e) {
            
            handleExceptions(e, requestObject.getRequestURI());
        } 
    }


     /**
     *  This method analyzes the exception for instances of AdfInvocationRuntimeException, AdfInvocation Exception and, 
     *  more broadly, AdfExceptions. If none of the two are found, it will look into the exception message for status 
     *  codes known returned by the API called by the utility. If still the exception isn't identified as Oracle MCS, 
     *  it will be rethrown with a status code of -1. 
     * 
     * @param e    the exception thrown by Oracle ADF. 
     * @param uri  the origin uri. This uri is used if the error message is composed based on error code findings in the exception
     *             message
     */
    private void handleExceptions(Exception e,String uri) throws ServiceProxyException {
        
         //Step 1: Is error AdfInvocationRuntimeException, AdfInvocationException or AdfException?        
        String exceptionPrimaryMessage      = e.getLocalizedMessage();
        String exceptionSecondaryMessage    = e.getCause() != null? e.getCause().getLocalizedMessage() : null;
        String combinedExceptionMessage =  "primary message:"+exceptionPrimaryMessage+(exceptionSecondaryMessage!=null?("; secondary message: "+exceptionSecondaryMessage):(""));
        
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
        if(exceptionSecondaryMessage!=null){
              mLogger.logFine( "Secondary exception message found: "+exceptionSecondaryMessage, this.getClass().getSimpleName(), "writeObject");
            try {
                JSONObject jsonErrorObject = new JSONObject(exceptionSecondaryMessage);
                //if we get here, then its a Oracle MCS error JSON Object. Get the 
                //status code or set it to 0 (means none is found)
                int statusCode = jsonErrorObject.optInt("status", 0);
                throw new ServiceProxyException(statusCode, exceptionSecondaryMessage);
                
            } catch (JSONException jse) {
                //if parsing fails, the this is proof enough that the error message is not 
                //an Oracle MCS message and we need to continue our analysis
                
                mLogger.logFine("Exception message is not a Oracle MCS error JSONObject", this.getClass().getSimpleName(), "handleExcpetions");
             }            
          }
        
          //continue message analysis and check for known error codes for the references MCS API
        
            mLogger.logFine("Rest invocation failed with following message"+exceptionPrimaryMessage, this.getClass().getSimpleName(), "handleExcpetions");
        
            int httpErrorCode = -1; 
            String restoredOracleMcsErrorMessage = null;
        
        /*             
        HTTP 400 Bad Request : Returned if "user" query parameter is specified for an endpoint that isn't an storageObject 
                               level operation on a user isolated collection.
        */   
            if(combinedExceptionMessage.contains("400")){
                httpErrorCode = StorageConstants.HTTP_400;
                restoredOracleMcsErrorMessage = OracleMobileErrorHelper.createOracleMobileErrorJson(StorageConstants.HTTP_400, "Bad request", "query parameter not allowed in this context",uri);
            }
            
        /*
        HTTP 401 Unauthorized : The user is not authenticated. The request must be made with the "Authorization" HTTP request header.
                                This would be the case if the authorization headers are not added by MAF or this utility or if the user
                                authenticated as anaonymous            
        */  
            else if(combinedExceptionMessage.contains("401")){
                httpErrorCode = StorageConstants.HTTP_401; 
                restoredOracleMcsErrorMessage = OracleMobileErrorHelper.createOracleMobileErrorJson(StorageConstants.HTTP_401, "Unauthorized", "User not authenticated or MBE Id missing", uri);
            }
            
        /*
        HTTP 403 Forbidden      Retrieve an storageObject without being assigned a role that has READ or READ_WRITE access for the collection. Or,
                                retrieve an storageObject from your isolated space without being assigned a role that has READ, READ_WRITE, READ_ALL, 
                                or READ_WRITE_ALL access for the collection.
            
        */
            else if(combinedExceptionMessage.contains("403")){
                httpErrorCode = StorageConstants.HTTP_403; 
                restoredOracleMcsErrorMessage =                 OracleMobileErrorHelper.createOracleMobileErrorJson(StorageConstants.HTTP_403, "Forbidden", "Retrieve an object without being assigned a role that has READ or READ_WRITE access for the collection. Or,\n" + 
            "                                    retrieve an object from your isolated space without being assigned a role that has READ, READ_WRITE, READ_ALL, \n" + 
            "                                    or READ_WRITE_ALL access for the collection.", uri);
            }
        /*
        HTTP 404 Not Found      An storageObjectwith the given identifier does not exist.
        */
            else if(combinedExceptionMessage.contains("404")){
                httpErrorCode = StorageConstants.HTTP_404; 
                restoredOracleMcsErrorMessage = OracleMobileErrorHelper.createOracleMobileErrorJson(StorageConstants.HTTP_404, "Not Found", "Object with the specified Id does not exist", uri);
            }
        /*
        HTTP 406 Not Acceptable The media type of the resource is not compatible with the values of the "Accept" header.         
         */
           else if(combinedExceptionMessage.contains("406")){
              httpErrorCode = StorageConstants.HTTP_406; 
              restoredOracleMcsErrorMessage = OracleMobileErrorHelper.createOracleMobileErrorJson(StorageConstants.HTTP_406,"Request Not Accepted","Accept header parameter did not match the server side content for the response", uri);
          }
        /*
        HTTP 409 Conflict - A change could not be applied because of a concurrent change by another requestr
         */
          else if(combinedExceptionMessage.contains("409")){
            httpErrorCode = 409; 
            restoredOracleMcsErrorMessage = OracleMobileErrorHelper.createOracleMobileErrorJson(409,"Concurrent request conflict","This operation conflicted with another change made concurrently to the object.", uri);
        }
            
        /*
        HTTP 411 Missing Content-Length
         */
          else if(combinedExceptionMessage.contains("411")){
            httpErrorCode = 411; 
            restoredOracleMcsErrorMessage = OracleMobileErrorHelper.createOracleMobileErrorJson(411,"Length Required","The HTTP request (PUT or POST) to store an object into a collection was missing either the Content-Length or Transfer-Encoding header.", uri);
        }
            
        /*
        HTTP 412 Precondition Failed
         */
          else if(combinedExceptionMessage.contains("412")){
            httpErrorCode = 411; 
            restoredOracleMcsErrorMessage = OracleMobileErrorHelper.createOracleMobileErrorJson(412,"Precondition Failed","An object with the given identifier in the specified collection exists, but the operation failed because of one or more of the following conditions: If-Match, If-Modified-Since, If-None-Match, or If-Unmodified-Since.", uri);
        }
            
        /*
        HTTP 413 Request Entity Too Large
         */
          else if(combinedExceptionMessage.contains("413")){
            httpErrorCode = 413; 
            restoredOracleMcsErrorMessage = OracleMobileErrorHelper.createOracleMobileErrorJson(413,"Request Entity Too Large","Returned if you attempt to store an object that is bigger than 2147483647 bytes (approx 2GB)", uri);
        }
            
            else{
                mLogger.logFine("Request failed with Exception: "+e.getClass().getSimpleName()+"; message: "+e.getLocalizedMessage(), this.getClass().getSimpleName(), "handleExcpetions");
                throw new ServiceProxyException(e.getLocalizedMessage(), ServiceProxyException.ERROR);
            }
            //if we get here then again its an Oracle MCS error, though one we found by inspecting the exception message
            mLogger.logFine("Request succeeded successful but failed with MCS application error. HTTP response: "+httpErrorCode+", Error message: "+restoredOracleMcsErrorMessage, this.getClass().getSimpleName(), "handleExcpetions");
           throw new ServiceProxyException(httpErrorCode, restoredOracleMcsErrorMessage);
    }

    /**
     * returns the decoded object Id
     * @param uri
     * @return 
     */
    private String getStorageObjectID(String uri) {
        int startIndex = uri.lastIndexOf('/');
        String tmp = uri.substring(startIndex + 1);

        if (tmp.indexOf(StorageConstants.USER_ID_KEY) != -1) {
            int endIndex = tmp.indexOf(StorageConstants.USER_ID_KEY);
            return decodeName(tmp.substring(0, endIndex));
        } else
            return decodeName(tmp);
    }

    
    /**
     * Adds the URI starting with "/collections" to the storage base URI
     * @param uri
     * @return
     */
    private String generateFullUri(String uri) {
        int index = uri.indexOf("/collection");
        return StorageConstants.STORAGE_RELATIVE_URL + uri.substring(index);
    }
    
    
    /**
     * Downloads the MCS collection identified by the collectionObjectUri
     *
     * @param collectionObjectURI the complete request URI for a object. E.g. /mobile/platform/storage/collections/collection_name/objects/cf29dea3-da84-47b7-b262-a47f69c58bf1.
     * You get the URL from the list returned by a call to queryStorageObjectsByRange() or a call to querySingleStorageObjectById()
     * @param acceptedMimeType the accept type for the expected content e.g. application/json, image/png, ... see: http://www.iana.org/assignments/media-types/media-types.xhtml
     * @return byte[] Array with the content read from MCS
     * @throws IllegalArgumentException if the collectionObjectURI is null or empty
     * @throws ServiceProxyException for MCS application failure an dtransport level exceptions
     */
    public byte[] downloadByteContentForObjectUri(String collectionObjectURI, String acceptedMimeType) throws IllegalArgumentException, ServiceProxyException {
        
        if(collectionObjectURI == null || collectionObjectURI.isEmpty()){
            throw new IllegalArgumentException("The collectionObjectURI in the call to getCollectionObject cannot be null");
        }
        
        MCSRequest requestObject = null;
        
        try {
            
           //Ensure URI is valid for collection if its isolated
            if(this.isUserIsolated && !collectionObjectURI.contains(StorageConstants.USER_ID_KEY)){
                //userid parameter is missing. 
                new ServiceProxyException("The userId parameter is missing in the request URI to an isolated collection", ServiceProxyException.ERROR);
            }
                        
           requestObject = new MCSRequest(this.mbeConfiguration);
           
           requestObject.setHttpMethod(com.oracle.maf.sample.mcs.shared.mafrest.MCSRequest.HttpMethod.GET);
           requestObject.setRequestURI(collectionObjectURI);
            
           requestObject.setConnectionName(this.mbeConfiguration.getMafRestConnectionName());
           
            HashMap<String,String> httpHeaders = new  HashMap<String,String>();
           //httpHeaders.put(HeaderConstants.ORACLE_MOBILE_BACKEND_ID, this.mbeConfiguration.getMobileBackendIdentifier());            
           httpHeaders.put(HeaderConstants.ACCEPT_HEADER, acceptedMimeType);
           
            mLogger.logFine( "key/value pairs in header Map: "+MapUtils.dumpStringProperties(httpHeaders), this.getClass().getSimpleName(), "getByteContentByObjectUri");
           
           //Authorization is added by MAF or MCS Utility
           requestObject.setHttpHeaders(httpHeaders);            
           requestObject.setPayload("");

           MCSResponse mcsResponse = MCSRestClient.sendForByteResponse(requestObject);
            
           if(mcsResponse != null && mcsResponse.getHttpStatusCode() == 200){ 
                mLogger.logFine( "Request succeeded", this.getClass().getSimpleName(), "getByteContentByObjectUri");
                return (byte[]) mcsResponse.getMessage();
            }
           else if (mcsResponse != null){
               mLogger.logFine( "Request succeeded with MCS application error. Response code: "+mcsResponse.getHttpStatusCode()+", Response Message: "+mcsResponse.getMessage(), this.getClass().getSimpleName(), "getByteContentByObjectUri");
               throw new ServiceProxyException(mcsResponse.getHttpStatusCode(),(String)mcsResponse.getMessage(), mcsResponse.getHeaders());
               
           }           
           } catch (Exception e) {
               //check if exception contains Oracle MCSS application error response. If not, throw ServiceProxyException
               mLogger.logFine( "Exception Handling ::: calling determineOracleMobileErrorFromException(...) to find Oracle MCS response codes in exception message", this.getClass().getSimpleName(), "getByteContentByObjectUri");
               this.handleExceptions(e, requestObject.getRequestURI());
           }        
        //we should get here
        return null;
    }
    
    /**
     * Downloads the MCS collection identified by the collectionObjectUri
     * @param objectId  the ID of an object in the collection 
     * @return byte[] Array with the content read from MCS
     * @throws IllegalArgumentException if the collectionObjectURI is null or empty
     * @throws ServiceProxyException for MCS application failure an dtransport level exceptions
     */
    public byte[] downloadByteContentForObjectId(String objectId, String acceptedMimeType) throws IllegalArgumentException, ServiceProxyException {
        mLogger.logFine("Downloading object from MCS using the object Id = "+objectId+" as a reference. Accept type = "+acceptedMimeType, this.getClass().getSimpleName(), "getByteContentByObjectId");
        String uri = generateUriForObject(this.collectionID, objectId);
        mLogger.logFine("URI is: "+uri, this.getClass().getSimpleName(), "getByteContentByObjectId");
        
        return downloadByteContentForObjectUri(uri, acceptedMimeType);
    }
    
    /**
     * print values of this object
     * @return String with property/value information 
     */
    public String dump(){
        return "collectionId:"+collectionID+", description: "+description+", contentLength: "+contentLength+", eTag: "+eTag+", isUserIsolated: "+isUserIsolated;
    }
    
    /**
     * The object owner Id property of this class will be set to the authenticated user id. This ID is set for users that 
     * perform basic authentication through MAF MCS Utility. 
     * 
     * If authentication is through MAF, you need to first call the UserInfo service proxy, get the userId for the current user 
     * and then call MBEConfiguration::setAuthenticatedUserMCSUserId
     */
    public void resetObjectOwnerUserIdToAuthenticatedUserId(){
        mLogger.logFine("Restting objectOwnerUserId to "+this.getStorage().getMbe().getMbeConfiguration().getAuthenticatedUserMCSUserId(), this.getClass().getSimpleName(), "resetObjectOwnerUserIdToAuthenticatedUserId");
        this.setObjectOwnerUserID(this.getStorage().getMbe().getMbeConfiguration().getAuthenticatedUserMCSUserId());
    }
}
