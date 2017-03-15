package com.oracle.maf.sample.mobile.mbeans.storage;

import com.oracle.maf.sample.mobile.mbeans.utils.DataControlsUtil;
import com.oracle.maf.sample.mobile.utils.DeviceUtil;

import java.util.ArrayList;

import oracle.adfmf.amx.event.ActionEvent;
import oracle.adfmf.amx.event.ValueChangeEvent;
import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;
import oracle.adfmf.json.JSONException;
import oracle.adfmf.json.JSONObject;

/**
 * Managed bean that supports the Oracle MCS MBE Storage upload/read use cases. This managed bean accesses the MAF MCS Utilty
 * directly (not going trough the data control) to demonstrate the singleton character of the MAF MCS Utility MBE objects. In
 * production application it is recommended to follow a consistent strategy for accessing MAF MCS Utility objects. Usually the
 * best option is to use the MAF AMX data control. To simplify development when working through data contols, it is recomended
 * you create a utility class similar to DataControlsUtil.java used in this sample.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class ObjectUpdateBacking extends SharedObjectHandling{


    private String tabMenuSelection =  ((Boolean) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.more.showintroduction}"))==true? "instructions":"content";

    // MCS Collection Object Attributes
    private String objectId          = null;
    private String displayName       = null;
    private String contentType       = null;
    private String createdOn         = null;
    private String createdBy         = null; 
    private String modifiedOn        = null;
    private String modifiedBy        = null;
    private String eTag              = null;
    private String contentLength     = null;
    private String canonicalLink     = null;
       
    private String documentName = "png.png";


    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public ObjectUpdateBacking() {
        super();
    }
   

    /**
     * Upload local file and file infromation to Oracle MCS. 
     * @param actionEvent
     */
    public void uploadFileToMCS(ActionEvent actionEvent){    
        
        //reset error messages
        setDisplaysErrorMessage("");
        
        if (DeviceUtil.hasFileAccess()) {
            byte[] fileContent = getDocumentContent(this.documentName);
            String mimeTye = this.getSelectedDocumentMimeType(this.documentName);
                        
            //prepare arguments
            ArrayList<String> parameterNames = new ArrayList<String>();
            parameterNames.add("canonicalURI");
            parameterNames.add("displayName");
            parameterNames.add("contentType");
            parameterNames.add("byteContent");            
            
            ArrayList<Object> parameterValues = new ArrayList<Object>();
            parameterValues.add(this.getCanonicalLink());
            parameterValues.add(this.getDocumentName());
            parameterValues.add(mimeTye);
            parameterValues.add(fileContent);
            
            ArrayList<Class> parameterTypes = new ArrayList<Class>();
            parameterTypes.add(String.class);
            parameterTypes.add(String.class);
            parameterTypes.add(String.class);
            parameterTypes.add(byte[].class);
            
            //invoke method exposed on the MobileBackendDC data control                        
            String jsonStringObject = (String) DataControlsUtil.invokeOnDataControl("updateCollectionObject", parameterNames, parameterValues, parameterTypes);
            
            //update the UI with the changed values
            if(jsonStringObject!= null){
                try {
                    JSONObject jsonObject = new JSONObject(jsonStringObject);
                    
                    this.setContentLength(jsonObject.optString("contentLength", ""));
                    this.setCreatedBy(jsonObject.optString("createdBy",""));
                    this.setCreatedOn(jsonObject.optString("createdOn", ""));
                    this.setDisplayName(jsonObject.optString("name", ""));
                    this.setETag(jsonObject.optString("eTag", ""));
                    this.setContentType(jsonObject.optString("contentType", ""));
                    this.setModifiedBy(jsonObject.optString("modifiedBy",""));
                    this.setModifiedOn(jsonObject.optString("modifiedOn",""));                
                } catch (JSONException e) {
                    setDisplaysErrorMessage("Request succeeded but failed in parsing the JSON response in the managed bean");
                }
            }
            else{
                setDisplaysErrorMessage("Request succeeded but no response message found");
            }
            
        }
        else{
            this.setDisplaysErrorMessage("Cannot read file because device has no file system access");
        }
    }




    /*
     * GETTER and SETTER methods
     */

    public void setDocumentName(String document) {
        String oldDocumentName = this.documentName;
        this.documentName = document;
        propertyChangeSupport.firePropertyChange("documentName", oldDocumentName, document);
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setObjectId(String objectId) {
        String oldObjectId = this.objectId;
        this.objectId = objectId;
        propertyChangeSupport.firePropertyChange("objectId", oldObjectId, objectId);
    }

    public String getObjectId() {
        return objectId;
    }


    public void setDisplayName(String displayName) {
        String oldDisplayName = this.displayName;
        
        //ensure the document name selection matches the 
        //display name if value is available in the list
        documentName = displayName;
        
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

    public void setCreatedOn(String createdOn) {
        String oldCreatedOn = this.createdOn;
        this.createdOn = createdOn;
        propertyChangeSupport.firePropertyChange("createdOn", oldCreatedOn, createdOn);
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedBy(String createdBy) {
        String oldCreatedBy = this.createdBy;
        this.createdBy = createdBy;
        propertyChangeSupport.firePropertyChange("createdBy", oldCreatedBy, createdBy);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setModifiedOn(String modifiedOn) {
        String oldModifiedOn = this.modifiedOn;
        this.modifiedOn = modifiedOn;
        propertyChangeSupport.firePropertyChange("modifiedOn", oldModifiedOn, modifiedOn);
    }

    public String getModifiedOn() {
        return modifiedOn;
    }

    public void setModifiedBy(String modifiedBy) {
        String oldModifiedBy = this.modifiedBy;
        this.modifiedBy = modifiedBy;
        propertyChangeSupport.firePropertyChange("modifiedBy", oldModifiedBy, modifiedBy);
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setETag(String eTag) {
        String oldETag = this.eTag;
        this.eTag = eTag;
        propertyChangeSupport.firePropertyChange("eTag", oldETag, eTag);
    }

    public String getETag() {
        return eTag;
    }


    public void setCanonicalLink(String canonicalLink) {
        String oldCanonicalLink = this.canonicalLink;
        this.canonicalLink = canonicalLink;
        propertyChangeSupport.firePropertyChange("canonicalLink", oldCanonicalLink, canonicalLink);
    }

    public String getCanonicalLink() {
        return canonicalLink;
    }

    public void setContentLength(String contentLength) {
        String oldContentLength = this.contentLength;
        this.contentLength = contentLength;
        propertyChangeSupport.firePropertyChange("contentLength", oldContentLength, contentLength);
    }

    public String getContentLength() {
        return contentLength;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    /**
     * Switches the value of the deck component, displaying a different area of the settings
     * @param valueChangeEvent
     */
    public void onTabMenuSelect(ValueChangeEvent valueChangeEvent) {
        setTabMenuSelection((String)valueChangeEvent.getNewValue());
    }

    public void setTabMenuSelection(String tabMenuSelection) {
        String oldTabMenuSelection = this.tabMenuSelection;
        this.tabMenuSelection = tabMenuSelection;
        propertyChangeSupport.firePropertyChange("tabMenuSelection", oldTabMenuSelection, tabMenuSelection);
    }

    public String getTabMenuSelection() {
        return tabMenuSelection;
    }

}
