package com.oracle.maf.sample.mobile.mbeans.storage;

import com.oracle.maf.sample.mobile.mbeans.utils.DataControlsUtil;

import oracle.adfmf.amx.event.ActionEvent;
import oracle.adfmf.amx.event.ValueChangeEvent;
import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;


/**
 *
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class ObjectDeleteBacking extends SharedObjectHandling{
    
    private String tabMenuSelection =  ((Boolean) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.more.showintroduction}"))==true? "instructions":"content";
    
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
        
   
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public ObjectDeleteBacking() {
        super();
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


    public void setPropertyChangeSupport(PropertyChangeSupport propertyChangeSupport) {
        PropertyChangeSupport oldPropertyChangeSupport = this.propertyChangeSupport;
        this.propertyChangeSupport = propertyChangeSupport;
        propertyChangeSupport.firePropertyChange("propertyChangeSupport", oldPropertyChangeSupport,
                                                 propertyChangeSupport);
    }

    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    public void onDeleteObject(ActionEvent actionEvent) {
        
        setDisplaysErrorMessage("");
        
        if(this.getObjectId() != null && !this.getObjectId().isEmpty()){
            
            //call data control to delete object. Any error message - or success information - will be set through the
            //data control
            DataControlsUtil.invokeDCSingleStringParameterMethod("deleteObjectFromCollection", "canonicalUri", this.canonicalLink);                
        }
        else{
            this.setDisplaysErrorMessage("Object id cannot be null or empty");
        }
        
    }
    
    
    /**
     * reset form
     * @param event
     */
    public void resetForm(ActionEvent event){
        
        setDisplaysErrorMessage("");
        
        objectId                 = "";
        displayName              = "";
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
