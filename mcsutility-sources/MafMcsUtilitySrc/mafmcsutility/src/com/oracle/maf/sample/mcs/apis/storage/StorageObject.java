package com.oracle.maf.sample.mcs.apis.storage;

import com.oracle.maf.sample.mcs.shared.utils.DateUtil;

import java.io.UnsupportedEncodingException;

import java.net.URLDecoder;

import java.util.Date;
import java.util.HashMap;

import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;
import oracle.adfmf.json.JSONArray;
import oracle.adfmf.json.JSONException;
import oracle.adfmf.json.JSONObject;

/**
 * Represents an object in a MCS collection. The object does not contain the object itelf but the metadata that describes the
 * object in Oracle MCS. MAF MCS Utility does not store objects for offline use. Objects in a collection can be up to 2GB in
 * size (2147483647 bytes (approx 2GB)), which is considerer too expensive to load all at once with no offline-synchronization
 * and caching. MAF MCS Utility does not provide offline caching and therefore only returns the information about stored objects
 * in MCS. The StorageCollection class exposes methods to fetch the real data object from MCS
 * <p>
 * The storage object metadata available for an object in Oracle MCS is described by a JSON object similar to the one below
 *
 * {
 *     "id": "6a66ea18-6f9c-4c8d-a23d-a345d48f740c",
 *     "name": "png.png",
 *     "user": "e6bda38e-635a-42d7-a509-a5da8a4cf50a",
 *     "contentLength": 10549,
 *     "contentType": "image/png",
 *     "eTag": "\"1\"",
 *     "createdBy": "uimcs",
 *     "createdOn": "2015-06-17T08:03:24Z",
 *     "modifiedBy": "uimcs",
 *     "modifiedOn": "2015-06-17T08:03:24Z",
 *     "links": [
 *       {
 *         "rel": "canonical",
 *         "href": "/mobile/platform/storage/collections/PrivateContent/objects/6a66ea18-6f9c-4c8d-a23d-a345d48f740c?user=e6bda38e-635a-42d7-a509-a5da8a4cf50a"
 *       },
 *       {
 *           "rel": "self",
 *            "href": "/mobile/platform/storage/collections/PrivateContent/objects/6a66ea18-6f9c-4c8d-a23d-a345d48f740c"
 *        }
 *     ]
 *   }
 *
  * @author Frank Nimphius, jiawshi
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class StorageObject { 
    
    private String objectID = null;

    //Properties
    private String displayName = null;
    private String user = "";
    private String contentType = null;
    private String createdBy = null;
    private Date createdOn = null;
    private String modifiedBy = null;
    private Date modifiedOn = null;
    private String canonicalLink = null;
    private String eTag = null;
    private long contentLength = 0;
    
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public StorageObject(String id,  String contentType) {
        this.objectID = id;
        this.contentType = contentType;
    }
    
    public StorageObject(String id) {
        this.objectID = id;
    }


    public void setID(String objectID) {
        String oldObjectID = this.objectID;
        this.objectID = objectID;
        propertyChangeSupport.firePropertyChange("objectID", oldObjectID, objectID);
    }

    public String getID() {
        return objectID;
    }

    public void setDisplayName(String displayName) {
        String oldDisplayName = this.displayName;
        this.displayName = displayName;
        propertyChangeSupport.firePropertyChange("displayName", oldDisplayName, displayName);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setContentType(String contentType) {
        String oldContentType = this.contentType;
        this.contentType = contentType;
        propertyChangeSupport.firePropertyChange("contentType", oldContentType, contentType);
    }

    public String getContentType() {
        return contentType;
    }

    public void setCreatedBy(String createdBy) {
        String oldCreatedBy = this.createdBy;
        this.createdBy = createdBy;
        propertyChangeSupport.firePropertyChange("createdBy", oldCreatedBy, createdBy);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedOn(String createdOn) {
        Date oldCreatedOn = this.createdOn;
        this.createdOn = DateUtil.parseToDate(createdOn);
        propertyChangeSupport.firePropertyChange("createdOn", oldCreatedOn, DateUtil.parseToDate(createdOn));
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setModifiedBy(String modifiedBy) {
        String oldModifiedBy = this.modifiedBy;
        this.modifiedBy = modifiedBy;
        propertyChangeSupport.firePropertyChange("modifiedBy", oldModifiedBy, modifiedBy);
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedOn(String modifiedOn) {
        Date oldModifiedOn = this.modifiedOn;
        this.modifiedOn = DateUtil.parseToDate(modifiedOn);
        propertyChangeSupport.firePropertyChange("modifiedOn", oldModifiedOn, DateUtil.parseToDate(modifiedOn));
    }

    public Date getModifiedOn() {
        return modifiedOn;
    }

    public void setCanonicalLink(String canonicalLink) {
        String oldCanonicalLink = this.canonicalLink;
        this.canonicalLink = canonicalLink;
        propertyChangeSupport.firePropertyChange("canonicalLink", oldCanonicalLink, canonicalLink);
    }

    public String getCanonicalLink() {
        return canonicalLink;
    }

    public void setETag(String eTag) {
        String oldETag = this.eTag;
        this.eTag = eTag;
        propertyChangeSupport.firePropertyChange("eTag", oldETag, eTag);
    }

    public String getETag() {
        return eTag;
    }


    public void setUser(String user) {
        String oldUser = this.user;
        this.user = user;
        propertyChangeSupport.firePropertyChange("user", oldUser, user);
    }

    public String getUser() {
        return user;
    }


    public void setContentLength(long contentLength) {
        long oldContentLength = this.contentLength;
        this.contentLength = contentLength;
        propertyChangeSupport.firePropertyChange("contentLength", oldContentLength, contentLength);
    }

    public long getContentLength() {
        return contentLength;
    }

    /**
     * Convenience method to populate StorageObject with content found in a JSONObject. The method attempts to read 
     * JSON Object properties. If the JSON Object is empty or if an attribute is not found, a default empty value is
     * set. For numeric values, the empty value is 0 (zero)
     * @param jsonObject
     * @throws Exception parsing exception
     */
    public void updateProperties(JSONObject jsonObject) throws Exception{
        
        // Set the properties of the StorageObject
        String id = jsonObject.optString(StorageConstants.OBJECT_PROPERTY_TAG_ID, "");
        this.setID(decodeName(id));
        
        String contentType = jsonObject.optString(StorageConstants.OBJECT_PROPERTY_CONTENT_TYPE,"");
        this.setContentType(contentType);
        
        String user = jsonObject.optString(StorageConstants.OBJECT_PROPERTY_USER, "");
        this.setUser(user);
        

        String displayName = jsonObject.optString(StorageConstants.OBJECT_PROPERTY_TAG_DISPLAYNAME,"");
        this.setDisplayName(displayName);

        String eTag = jsonObject.optString(StorageConstants.OBJECT_PROPERTY_TAG_ETAG,"");
        this.setETag(eTag);

        String createdBy = jsonObject.optString(StorageConstants.OBJECT_PROPERTY_TAG_CREATEDBY,"");
        this.setCreatedBy(createdBy);

        String createdOn = jsonObject.optString(StorageConstants.OBJECT_PROPERTY_TAG_CREATEDON,"");
        this.setCreatedOn(createdOn);
        
        long contentLength = jsonObject.optLong(StorageConstants.OBJECT_PROPERTY_CONTENT_LENGTH,0);
        this.setContentLength(contentLength); 

        //Take care of the case when the object is first created, neither "modifiedBy" or "modifiedOn" info is available
        if (jsonObject.has(StorageConstants.OBJECT_PROPERTY_TAG_MODIFIEDBY))
            this.setModifiedBy(jsonObject.getString(StorageConstants.OBJECT_PROPERTY_TAG_MODIFIEDBY));
        else
            this.setModifiedBy(null);

        if (jsonObject.has(StorageConstants.OBJECT_PROPERTY_TAG_MODIFIEDON))
            this.setModifiedOn(jsonObject.getString(StorageConstants.OBJECT_PROPERTY_TAG_MODIFIEDON));
        else
            this.setModifiedOn(null);

        //Set the canonicalLink
        JSONArray links = jsonObject.getJSONArray(StorageConstants.OBJECT_PROPERTY_TAG_LINKS);
        this.setCanonicalLink(this.pickCanonicalLink(links));
    }


    /**
     * Convenience method to populate StorageObject with content found in a JSON payload String
     * @param body JSON message describing a single StorageObject
     * @throws Exception
     */
    public void updateProperties(String body) throws Exception{        
        JSONObject json = new JSONObject(body);
        updateProperties(json);
    }

    
    /**
     * Convenience method to populate StorageObject with key-value pairs found in a HashMap
     * @param headers HashMap with key value pairs containing "name":"value","contenttype":"value","createdBy":"value",
     *        "createdOn":value,"modifiedBy":"value","modifiedOn":"value","canonical":"value","contentLength":"value",
     *        "Etag":"value"
     */
    public void updateProperties(HashMap<String, String> headers){
       
        String displayName = headers.get(StorageConstants.OBJECT_PROPERTY_TAG_DISPLAYNAME);
        this.setDisplayName(displayName);

        String contentType = headers.get(StorageConstants.OBJECT_PROPERTY_CONTENT_TYPE);
        this.setContentType(contentType);

        String user = headers.get(StorageConstants.OBJECT_PROPERTY_USER);
        if(user != null) this.setUser(user);

        String createdBy = headers.get(StorageConstants.OBJECT_PROPERTY_TAG_CREATEDBY);
        this.setCreatedBy(createdBy);

        String createdOn = headers.get(StorageConstants.OBJECT_PROPERTY_TAG_CREATEDON);
        this.setCreatedOn(createdOn);

        String modifiedBy = headers.get(StorageConstants.OBJECT_PROPERTY_TAG_MODIFIEDBY);
        this.setModifiedBy(modifiedBy);

        String modifiedOn = headers.get(StorageConstants.OBJECT_PROPERTY_TAG_MODIFIEDON);
        this.setModifiedOn(modifiedOn);
        
        String canonicalLink = headers.get(StorageConstants.OBJECT_PROPERTY_LINK_TAG_CANONICAL);
        this.setCanonicalLink(canonicalLink);

        long contentLength = new Long(headers.get(StorageConstants.OBJECT_PROPERTY_CONTENT_LENGTH)).longValue();
        this.setContentLength(contentLength);
        
        String eTag = headers.get(StorageConstants.HEADER_PROPERTY_TAG_ETAG);
        this.setETag(eTag);
    }


    /**
     * 
     * @param links
     * @return a link string or null if the link could not be found
     * @throws JSONException if parsing of the JSONArray holding the link fails
     */
    private String pickCanonicalLink(JSONArray links) throws JSONException{
        for(int i=0; i<links.length(); i++){
            JSONObject link = links.getJSONObject(i);
            if(link.getString(StorageConstants.OBJECT_PROPERTY_LINK_TAG_REL).equals(StorageConstants.OBJECT_PROPERTY_LINK_TAG_CANONICAL)){
                String canonicalLink = link.getString(StorageConstants.OBJECT_PROPERTY_LINK_TAG_HREF);
                return canonicalLink;
            }
        }
        return null;
    }

    /**
     * Method decodes the URL encoded id strings
     * @param objectId the name of 
     * @return decoded string
     */
    private String decodeName(String objectId){
        String decodedName = "";
        try {
            decodedName = URLDecoder.decode(objectId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return decodedName;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
    
    /**
     * Composes a JSON String representation of this object. The returned JSON string contains the object id, name, 
     * user, contentLength, contentType, eTag, createdby, createdOn, modifiedBy, modifiedOn and canonicalLink
     * 
     * @return
     */
    public String toJSONString(){
        StringBuffer sb = new StringBuffer("{");
        sb.append("\"id\":\""+this.getID()+"\"");
        if(this.getDisplayName()!=null) sb.append(",\"name\":\""+this.getDisplayName()+"\"");
        if(this.getUser()!=null)        sb.append(",\"user\":\""+this.getUser()+"\"");
        sb.append(",\"contentLength\":\""+this.getContentLength()+"\"");
        if(this.getContentType()!=null) sb.append(",\"contentType\":\""+this.getContentType()+"\"");
        if(this.getETag() !=null) {
            
            //remove possible quotes at the beginning and the end
            int indexStart = this.getETag().indexOf("", 0);
            int indexEnd = -1;
            
            if (indexStart > -1){
                indexEnd = this.getETag().indexOf("", indexStart+1);
                String etag =  this.getETag().substring(indexStart+1);
                if (indexEnd >-1){
                    etag = etag.substring(0, indexEnd);
                    sb.append(",\"eTag\":\""+etag+"\"");
                }
            }
            else{
                sb.append(",\"eTag\":\""+this.getETag()+"\"");
            }                                                
        }
        if(this.getContentType()!=null) sb.append(",\"createdBy\":\""+this.getCreatedBy()+"\"");
        if(this.getContentType()!=null) sb.append(",\"createdOn\":\""+this.getCreatedOn()+"\"");
        if(this.getContentType()!=null) sb.append(",\"modifiedBy\":\""+this.getModifiedBy()+"\"");
        if(this.getContentType()!=null) sb.append(",\"modifiedOn\":\""+this.getModifiedOn()+"\"");
        sb.append(",\"canonicalLink\":\""+this.getCanonicalLink()+"\"");
        sb.append("}");
      
      return sb.toString();  
    }
}

