package com.oracle.maf.sample.mobile.mbeans.storage;

import oracle.adfmf.amx.event.ValueChangeEvent;
import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;


/**
 * Storage options allows you to view the content of a storage collection to perform actions on it. For example, you
 * may want to upload content from a mobile device, download content from MCS or delete resources
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class CollectionDetailsBacking {
    
    private String tabMenuSelection =  ((Boolean) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.more.showintroduction}"))==true? "instructions":"content";
    
    
    private String selectedCollectionId = null;
    
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public CollectionDetailsBacking() {
        super();
    }

    /**
     * Method called from a setPropertyListener tag on the Storage.amx page to set the ID of the selected collection. This
     * id is then used in querying the content of the collection for display on the StorageOptions page.
     * 
     * @param selectedCollectionId
     */
    public void setSelectedCollectionId(String selectedCollectionId) {
        String oldSelectedCollectionId = this.selectedCollectionId;
        this.selectedCollectionId = selectedCollectionId;
        propertyChangeSupport.firePropertyChange("selectedCollectionId", oldSelectedCollectionId, selectedCollectionId);
    }

    public String getSelectedCollectionId() {
        return selectedCollectionId;
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
