package com.oracle.maf.sample.mobile.mbeans.analytics;

import com.oracle.maf.sample.mobile.mbeans.utils.DataControlsUtil;

import java.util.HashMap;

import oracle.adfmf.amx.event.ActionEvent;
import oracle.adfmf.amx.event.ValueChangeEvent;
import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;


/**
 * Backing bean class to collect and post analytic events. The managed bean works through the MAF data control but not
 * the MAF binding layer. Reason is that the call addresses a method and not a collection. Data input, data submit and
 * data re-input etc. is easier to implement in a managed bean with a direct data control access than through the method
 * binding. Its not a decision against the MAF binding but a decision in favor of the user case.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class AnalyticsHelper {
    
    private String tabMenuSelection =  ((Boolean) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.more.showintroduction}"))==true? "instructions":"content";
    
    private String productId = null;
    private String productName = null;
    private String unitPrice = null;
    private String quantity = null;
    
    //name of custom event. default is "mafmcsutility-food". Note that custom event 
    //names can be freely chosen to match the mobile business case
    private String customEventName = "mafmcsutility-food";
    
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);


    public AnalyticsHelper() {
        super();
    }
    
    public void addEvent(ActionEvent action){
        
        /* ***
         * all required fields are porivded with values, so we don't need to check for null or empty values. The HashMap 
         * can have an arbitrary  number of key/value pairs. Its not restricted to the key names and values used in this 
         * sample. Custom analytic events describe an action and  thus may have many key/value pairs. The key names too 
         * are free for you to choose. Thinks "business process logging" when defining custom events.
         *
         * The key name becomes a grouping criteria for the event logged in Oracle MCS. E.g. if you send an electronics
         * purchase event (as in this sample) then this event can be alayzed by the product name occurences, totals, unit 
         * price etc.
         */
        HashMap<String,String> eventProperties = new HashMap<String,String>();
        eventProperties.put("ProductId",this.getProductId());
        eventProperties.put("ProductName",this.getProductName());
        eventProperties.put("UnitPrice", this.getUnitPrice());
        eventProperties.put("Quantity", this.getQuantity());
        
        //Note that the data control adds a "Total" field to show that indeed custom events are flexible to work with.  
        DataControlsUtil.addCustomAnalyticEvent(customEventName,eventProperties);
        
        //rest form details
        this.setProductId("");
        this.setProductName("");
        this.setQuantity("");
        this.setUnitPrice("");
        
    }
    
    public void resetForm(ActionEvent action){
        this.setProductId("");
        this.setProductName("");
        this.setQuantity("");
        this.setUnitPrice("");
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

    public void setProductId(String productId) {
        String oldProductId = this.productId;
        this.productId = productId;
        propertyChangeSupport.firePropertyChange("productId", oldProductId, productId);
    }

    public String getProductId() {
        return productId;
    }

    public void setProductName(String productName) {
        String oldProductName = this.productName;
        this.productName = productName;
        propertyChangeSupport.firePropertyChange("productName", oldProductName, productName);
    }

    public String getProductName() {
        return productName;
    }

    public void setUnitPrice(String unitPrice) {
        String oldUnitPrice = this.unitPrice;
        this.unitPrice = unitPrice;
        propertyChangeSupport.firePropertyChange("unitPrice", oldUnitPrice, unitPrice);
    }

    public String getUnitPrice() {
        return unitPrice;
    }

    public void setQuantity(String quantity) {
        String oldQuantity = this.quantity;
        this.quantity = quantity;
        propertyChangeSupport.firePropertyChange("quantity", oldQuantity, quantity);
    }

    public String getQuantity() {
        return quantity;
    }


    public void setCustomEventName(String customEventName) {
        String oldCustomEventName = this.customEventName;
        this.customEventName = customEventName;
        propertyChangeSupport.firePropertyChange("customEventName", oldCustomEventName, customEventName);
    }

    public String getCustomEventName() {
        return customEventName;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
}
