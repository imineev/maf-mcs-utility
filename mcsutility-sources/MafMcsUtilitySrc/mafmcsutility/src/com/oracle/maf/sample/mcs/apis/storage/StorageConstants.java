package com.oracle.maf.sample.mcs.apis.storage;


/**
 *
 *
 * @author   Frank Nimphius, jiawshi
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class StorageConstants {
    /**
     * Relative url for Storage API
     */
    public static final String STORAGE_RELATIVE_URL = "/mobile/platform/storage/collections";
    
    public static final String USER_INFO_RELATIVE_URL = "/mobile/platform/users";

    public static final String USER_ID_KEY = "?user=";

    /**
     * Backend Token Headers
     */
    public static final String BACKEND_TOKEN_HEADER = "X-Backend-Token";

    /**
     * Http Headers
     */

    public static final String CONTENT_LENGTH_HEADER = "Content-Length";


    /**
     * TAGS for StorageCollectionsList
     */

    public static final String STORAGE_INFORMATION_LIMIT    = "limit";
    
    public static final String STORAGE_INFORMATION_HAS_MORE = "hasMore";
    
    public static final String STORAGE_INFORMATION_OFFSET   = "offset";
    
    public static final String STORAGE_INFORMATION_COUNT    = "count";
    
    public static final String STORAGE_INFORMATION_TOTAL_RESULTS = "totalResults";
    
    public static final String STORAGE_INFORMATION_ITEMS    = "items";
    
    public static final String STORAGE_INFORMATION_CONTENT_LENGTH    = "contentLength";
    
    /**
     * TAGS for collections in the Storage Information
     */

     public static final String STORAGE_COLLECTION_ID               =  "id";
    
     public static final String STORAGE_COLLECTION_DESCRIPTION      =  "description";
     
     public static final String STORAGE_COLLECTION_USER_ISOLATED    = "userIsolated";
     
     public static final String STORAGE_COLLECTION_CONTENT_LENGTH   =   "contentLength";
     
     public static final String STORAGE_COLLECTION_ETAG             =  "eTag";

    /**
     * TAGS for Object properties
     */
    public static final String OBJECT_PROPERTY_TAG_CREATEDON = "createdOn";

    public static final String OBJECT_PROPERTY_TAG_CREATEDBY = "createdBy";

    public static final String OBJECT_PROPERTY_TAG_MODIFIEDON = "modifiedOn";

    public static final String OBJECT_PROPERTY_TAG_MODIFIEDBY = "modifiedBy";

    public static final String OBJECT_PROPERTY_TAG_ID = "id";

    public static final String OBJECT_PROPERTY_TAG_DISPLAYNAME = "name";

    public static final String OBJECT_PROPERTY_TAG_ETAG = "eTag";

    public static final String OBJECT_PROPERTY_TAG_LINKS = "links";

    public static final String OBJECT_PROPERTY_LINK_TAG_REL = "rel";

    public static final String OBJECT_PROPERTY_LINK_TAG_CANONICAL = "canonical";

    public static final String OBJECT_PROPERTY_LINK_TAG_HREF = "href";
    
    public static final String OBJECT_PROPERTY_CONTENT_TYPE = "contentType";
    
    public static final String OBJECT_PROPERTY_USER = "user";
    
    public static final String OBJECT_PROPERTY_CONTENT_LENGTH = "contentLength";

    public static final String COLLECTION_PROPERTY_TAG_DESCRIPTION = "description";

    public static final String COLLECTION_PROPERTY_TAG_ETAG = "eTag";

    public static final String COLLECTION_PROPERTY_TAG_USER_ISOLATED = "userIsolated";
    
    public static final String COLLECTION_PROPERTY_TAG_CONTENT_LENGTH    = "contentLength";
    



    /**
     * TAGS for Header properties
     */
    public static final String HEADER_PROPERTY_TAG_CREATEDON = "Oracle-Mobile-CREATED-ON";

    public static final String HEADER_PROPRTTY_TAG_CREATEDBY = "Oracle-Mobile-CREATED-BY";

    public static final String HEADER_PROPERTY_TAG_MODIFIEDON = "Oracle-Mobile-MODIFIED-ON";

    public static final String HEADER_PROPERTY_TAG_MODIFIEDBY = "Oracle-Mobile-MODIFIED-BY";

    public static final String HEADER_PROPERTY_TAG_CANONICAL_LINK = "Oracle-Mobile-CANONICAL-LINK";

    public static final String HEADER_PROPERTY_TAG_DISPLAYNAME = "Oracle-Mobile-NAME";

    public static final String HEADER_PROPERTY_TAG_ETAG = "ETag";


    /**
     * HTTP 200 OK    The list of collections was successfully retrieved.
     */
    
    public static final int HTTP_200 = 200;
    
    
    /**
     * 201 Created The object has been successfully created in the specified collection.
     */
    
    public static final int HTTP_201 = 201;
    
    
    /**
     * 204 Request successful but server response does not contain content
     */
    public static final int HTTP_204 = 204;
    
    
    
    /**
     * This response contains the range(s) requested in the "Range" HTTP request header.
     */
    public static final int HTTP_206 = 206;
    
    /**
     * An item with specified identifier exists, but the operation failed because of the timestamp value specified in 
     * the request's "If-Modified-Since" HTTP header.
     */
    public static final int HTTP_304 = 304;
    
    /**
    * HTTP 400 Bad Request Returned if you attempt to:
    * Make a call without specifying the "Oracle-Mobile-BACKEND-ID" HTTP request header:
    *    - Specify the "user" query parameter for an endpoint that isn't an object level operation on a user isolated collection.  
    *    - Specify an incorrect value for a query parameter.
    *    - Store an object with an identifier longer than the maximum allowed size.
    *    - Store an object where the actual size of the object is different from what was specified in the "Content-Length" HTTP request header.
   */    
   public static final int HTTP_400 = 400;

   /**
   * HTTP 401 Unauthorized - The user is not authenticated. The request must be made with the "Authorization" HTTP request header.
   */
   public static final int HTTP_401 = 401;
   
   /**
   * HTTP 403 Forbidden -  Returned if you attempt to:

        - Make a request with a user from a realm that isn't associated with the mobile backend.
        - Retrieve an object without being assigned a role that has READ or READ_WRITE access for the collection.
        - Retrieve an object from your isolated space without being assigned a role that has READ, READ_WRITE, READ_ALL, or READ_WRITE_ALL access for the collection.
        - Retrieve an object from another user's isolated space without being assigned a role that has READ_ALL or READ_WRITE_ALL access for the collection.
        - Store an object without being assigned a role that has been granted READ_WRITE access for the collection.
        - Store an object to your isolated space without being assigned a role that has READ_WRITE or READ_WRITE_ALL access for the collection.
        - Store an object to another user's isolated space without being assigned a role that has READ_WRITE_ALL access for the collection.
    */   
   public static final int HTTP_403 = 403;
   
   /**
    * HTTP 404 Not Found - Object with specified identifier doesn't exist
    */
   public static final int HTTP_404 = 404;
   
   /**   
    * HTTP 406 Not Acceptable - The media type of the resource is not compatible with the values of the "Accept" header. 
    * For example if you tried to request a resource with media type "application/json" with an "Accept" header with value 
    * "application/xml".
    */   
   public static final int HTTP_406 = 406;
   
   /**
    * 
    */
   public static final int HTTP_412 = 412;
}
