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
 * Managed bean supporting the creation of a new storage object in Oracle MCS for the current collection. In Oracle MCS
 * you create new storage objects with a PUT or POST call to the storage API. The difference between PUT and POST is that
 * POST reates a random (uniwue) object Id. This managed bean invoked the POST method exposed on the MobileBackendDC data
 * control. The use of PUT is shown in ObjectUpdateBacking
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class ObjectCreateBacking extends SharedObjectHandling{

    private String tabMenuSelection =  ((Boolean) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.more.showintroduction}"))==true? "instructions":"content";
    
    // MCS Collection Object Attributes
    private String collectionId             = null;
    private String objectId                 = null;
    private String displayName              = null;
    private String displayNameSetByUser     = null;
    private String contentType              = null;
    private String createdOn                = null;
    private String createdBy                = null; 
    private String modifiedOn               = null;
    private String modifiedBy               = null;
    private String eTag                     = null;
    private String contentLength            = null;
    

       
    private String documentName = "png.png";


    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    
    public ObjectCreateBacking() {
        super();
    }
    

    /**
     * Upload the device-local file and file information to Oracle MCS. 
     * @param actionEvent
     */
    public void createStorageObject(ActionEvent actionEvent){        
        //reset error messages
        setDisplaysErrorMessage("");
        
        if (DeviceUtil.hasFileAccess()) {            
            
            byte[] fileContent = getDocumentContent(this.documentName);
            String mimeTye = this.getSelectedDocumentMimeType(this.documentName);
                        
            //prepare arguments
            ArrayList<String> parameterNames = new ArrayList<String>();
            parameterNames.add("displayName");
            parameterNames.add("contentType");
            parameterNames.add("byteContent");            
            
            ArrayList<Object> parameterValues = new ArrayList<Object>();
            
            //set the display name to value chosen by the user or document name if no value has been set. If display-name is set, append the file type
            String dName = (this.displayNameSetByUser == null || this.displayNameSetByUser.isEmpty())? this.getDocumentName():this.getDisplayNameSetByUser()+this.getDocumentName().substring(this.getDocumentName().indexOf("."));
            
            parameterValues.add(dName);
            parameterValues.add(mimeTye);
            parameterValues.add(fileContent);
            
            ArrayList<Class> parameterTypes = new ArrayList<Class>();
            parameterTypes.add(String.class);
            parameterTypes.add(String.class);
            parameterTypes.add(byte[].class);
            
            //invoke method exposed on the MobileBackendDC data control                        
            String jsonStringObject = (String) DataControlsUtil.invokeOnDataControl("createCollectionObject", parameterNames, parameterValues, parameterTypes);
            
            
            //reset display name set by user
            this.setDisplayNameSetByUser(null);
            
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
                    setDisplaysErrorMessage(" ");
                    
                } catch (JSONException e) {
                    setDisplaysErrorMessage("Request succeeded. Parsing JSON response failed in managed bean");
                }
            }
            else{
                setDisplaysErrorMessage("Request succeeded. No response found from server.");
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


    public void setContentLength(String contentLength) {
        String oldContentLength = this.contentLength;
        this.contentLength = contentLength;
        propertyChangeSupport.firePropertyChange("contentLength", oldContentLength, contentLength);
    }


    public String getContentLength() {
        return contentLength;
    }


    public void setDisplayNameSetByUser(String displayNameSetByUser) {
        String oldDisplayNameSetByUser = this.displayNameSetByUser;
        this.displayNameSetByUser = displayNameSetByUser;
        propertyChangeSupport.firePropertyChange("displayNameSetByUser", oldDisplayNameSetByUser, displayNameSetByUser);
    }

    public String getDisplayNameSetByUser() {
        return displayNameSetByUser;
    }

    public void setCollectionId(String collectionId) {
        String oldCollectionId = this.collectionId;
        this.collectionId = collectionId;
        propertyChangeSupport.firePropertyChange("collectionId", oldCollectionId, collectionId);
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
    
    /**
     * reset form
     * @param event
     */
    public void resetForm(ActionEvent event){
        resetScreen();
    }
    
    private void resetScreen(){
        
        //reset error messages
        setDisplaysErrorMessage(null);
        
        collectionId             = "";
        objectId                 = "";
        displayName              = "";
        displayNameSetByUser     = "";
        contentType              = "";
        createdOn                = "";
        createdBy                = ""; 
        modifiedOn               = "";
        modifiedBy               = "";
        eTag                     = "";
        contentLength            = "";
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
