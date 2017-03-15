package com.oracle.maf.sample.mobile.mbeans.custom;

import com.oracle.maf.sample.mobile.mbeans.utils.DataControlsUtil;
import com.oracle.maf.sample.mobile.utils.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;

import oracle.adfmf.amx.event.ActionEvent;
import oracle.adfmf.amx.event.ValueChangeEvent;
import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;
import oracle.adfmf.java.beans.ProviderChangeListener;
import oracle.adfmf.java.beans.ProviderChangeSupport;


/**
 * Managed beans to hold custom API call information and to display API responses the sample is
 * limited to custom APIs that return String responses like JSON responses. String responses are
 * printed in a output text field in their raw format. The point is not to show a nice structure
 * JSON output but that the custom API actually works and that responses are handled properly.
 *
 * Note that MAF MCS Utility is able to also invoke custom APIs that return binary content too. This
 * however is not in the focus of this public sample to reduce the complexity of the code.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class CustomAPIBacking {
    
    private String tabMenuSelection =  ((Boolean) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.more.showintroduction}"))==true? "instructions":"content";
    
    private String uri               = "";
    private String httpMethod        = "GET";
    private String headerKeyName     = "";
    private String headerKeyValue    = "";
    private String payload           = "";
    //payload is only an option if POST or PUT is selected
    private boolean showPayloadOption = false;
    
    private HashMap<String,String> httpHeaders = new HashMap<String, String>();
    private String                 printedMap  = "Empty";
    
    protected transient ProviderChangeSupport providerChangeSupport = new ProviderChangeSupport(this);
    
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);


    public CustomAPIBacking() {
        super();
        //ensure application/json is set
        httpHeaders.put("Accept", "application/json");
        //show preset value
        this.setPrintedMap(MapUtils.dumpProperties(this.httpHeaders));
    }

    public void addProviderChangeListener(ProviderChangeListener l) {
      providerChangeSupport.addProviderChangeListener(l);
    }

    public void removeProviderChangeListener(ProviderChangeListener l) {
       providerChangeSupport.removeProviderChangeListener(l);
    }

    
    /**
     * Adds a parameter to the httpHeaders map. This enabled sample users to add http headers that are specific to the 
     * custom API they want to invoke.
     * @param event
     */
    public void addHeaderParameter(ActionEvent event){

        //setting headers only make sense if they have a name and a value
        if (this.getHeaderKeyName().length()>0 && this.getHeaderKeyValue().length() >0) {
            if (!this.getHeaderKeyName().equalsIgnoreCase("Accept")) {
                //read the values
                this.httpHeaders.put(this.getHeaderKeyName(), this.getHeaderKeyValue());
                
                this.setHeaderKeyName("");
                this.setHeaderKeyValue("");
                
                //update Display
                this.setPrintedMap(MapUtils.dumpProperties(this.httpHeaders));
            }
            else{
                //ignore attempts to set the accept header parameter as this sample only supports
                //json responses. Note that other response formats are possible but would require 
                //additional conversion and handling (a usecase that goes beyond of this sample)
                this.setHeaderKeyName("");
                this.setHeaderKeyValue("");
            }
        }            
    }
    
    /**
     * Set header parameter back to an empty HashMap
     * @param event
     */
    public void resetForm(ActionEvent event){
        
        this.setHeaderKeyName("");
        this.setHeaderKeyValue("");
        
        this.setPayload("");
        
        HashMap<String,String> __httpHeaders = new HashMap<String, String>();
        __httpHeaders.put("Accept", "application/json");
        this.setHttpHeaders(__httpHeaders);
        
        //reset URI
        this.setUri("");
        
        //set http method to GET
        this.setHttpMethod("GET");
        
        //diabled payload
        this.setShowPayloadOption(false);
        
        //reset print form of header map
        this.setPrintedMap(MapUtils.dumpProperties(this.httpHeaders));
        
        //and there is one binding that needs to be reset
        AdfmfJavaUtilities.setELValue("#{bindings.customAPIResponse.inputValue}","");
        
        //fire provider refreh to display changed key-value set
        
                
    }
    

    public void invokeCustomApiOnDC(ActionEvent event){
                        
        //Here is how the method signature in the MobileBackednDC looks like: invokeCustomMcsAPI(String mafConnection, 
        //String requestURI, String httpMethod, String payload, HashMap httpHeaders)
        
        ArrayList<String>   paramNames = new ArrayList<String>();        
        paramNames.add("mafConnection");
        paramNames.add("requestURI");
        paramNames.add("httpMethod");
        paramNames.add("payload");
        paramNames.add("httpHeaders");
        
        ArrayList<Object>   paramValues = new ArrayList<Object>();        
        paramValues.add("MCSUTILRESTCONN"); //as defined in Application Resources --> Connections --> REST
        
        //add leading slash if missing
        
        String _uri = this.getUri().startsWith("/")?this.getUri():"/"+this.getUri();
        //remove training slash if provided. 
        _uri = _uri.lastIndexOf("/") == _uri.length()-1? _uri.substring(0,_uri.length()-1):_uri;
                
        paramValues.add(_uri);
        paramValues.add(this.getHttpMethod());
        paramValues.add(this.getPayload());
        paramValues.add(this.getHttpHeaders());
        
        ArrayList<Class> paramTypes = new ArrayList<Class>();
        paramTypes.add(String.class);
        paramTypes.add(String.class);
        paramTypes.add(String.class);
        paramTypes.add(String.class);
        paramTypes.add(HashMap.class);
        
        DataControlsUtil.invokeOnDataControl("invokeCustomMcsAPI", paramNames, paramValues, paramTypes);                                    
    }


    public void setUri(String uri) {
        String oldUri = this.uri;
        this.uri = uri;
        propertyChangeSupport.firePropertyChange("uri", oldUri, uri);
    }

    public String getUri() {
        return uri;
    }

    /**
     * Sets the HTTP method. If the http method changes, reset the payload value
     * @param httpMethod
     */
    public void setHttpMethod(String httpMethod) {
        String oldHttpMethod = this.httpMethod;
        this.httpMethod = httpMethod;
        propertyChangeSupport.firePropertyChange("httpMethod", oldHttpMethod, httpMethod);
        
        
        //hide / show payload option
        if (httpMethod.equalsIgnoreCase("POST") ||httpMethod.equalsIgnoreCase("PUT")){
            this.setPayload("");
            setShowPayloadOption(true);
        }
        else{
            this.setPayload("");
            setShowPayloadOption(false);
        }
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHeaderKeyName(String headerKeyName) {
        String oldHeaderKeyName = this.headerKeyName;
        this.headerKeyName = headerKeyName;
        propertyChangeSupport.firePropertyChange("headerKeyName", oldHeaderKeyName, headerKeyName);
    }

    public String getHeaderKeyName() {
        return headerKeyName;
    }

    /**
     * Displays the content in the view. 
     * @param printedMap
     */
    public void setPrintedMap(String printedMap) {
        String oldPrintedMap = this.printedMap;
        this.printedMap = printedMap;
        propertyChangeSupport.firePropertyChange("printedMap", oldPrintedMap, printedMap);
    }

    public String getPrintedMap() {
        return printedMap;
    }

    public void setHeaderKeyValue(String headerKeyValue) {
        String oldHeaderKeyValue = this.headerKeyValue;
        this.headerKeyValue = headerKeyValue;
        propertyChangeSupport.firePropertyChange("headerKeyValue", oldHeaderKeyValue, headerKeyValue);
    }

    public String getHeaderKeyValue() {
        return headerKeyValue;
    }

    public void setPayload(String payload) {
        String oldPayload = this.payload;
        this.payload = payload;
        propertyChangeSupport.firePropertyChange("payload", oldPayload, payload);
    }

    public String getPayload() {
        return payload;
    }

    public void setHttpHeaders(HashMap<String, String> httpHeaders) {
        HashMap<String, String> oldHttpHeaders = this.httpHeaders;
        this.httpHeaders = httpHeaders;
        propertyChangeSupport.firePropertyChange("httpHeaders", oldHttpHeaders, httpHeaders);
    }


    public void setShowPayloadOption(boolean showPayloadOption) {
        boolean oldShowPayloadOption = this.showPayloadOption;
        this.showPayloadOption = showPayloadOption;
        propertyChangeSupport.firePropertyChange("showPayloadOption", oldShowPayloadOption, showPayloadOption);
    }

    public boolean isShowPayloadOption() {
        return showPayloadOption;
    }

    public HashMap<String, String> getHttpHeaders() {
        return httpHeaders;
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
