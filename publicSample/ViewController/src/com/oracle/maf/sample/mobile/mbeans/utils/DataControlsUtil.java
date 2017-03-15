package com.oracle.maf.sample.mobile.mbeans.utils;

import com.oracle.maf.sample.mobile.logger.McsSampleLogger;

import java.util.ArrayList;
import java.util.HashMap;

import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.framework.exception.AdfInvocationException;


/**
 * A utility class that simplifies the access to the MAF data control from managed beans and other Java objects. The call
 * API to invoke methods on a data control requires HashMaps for argument names, values and data types. This is fine to use
 * if you need it only once. For more frequent access, convenient methods make sense to create up-front as shown here
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class DataControlsUtil {
    public DataControlsUtil() {
        super();
    }
        
   /**
     * Invokes a method on a data control. The argument names, values and types need to be provided in instances of ArrayList
     * @param dataControl enum value that identifies the data control to invoke this method on
     * @param methodName The name of the method to invoke on a data control
     * @param paramNames ArrayList&lt;String> of method argument names 
     * @param paramValues ArrayList&lt;Object> of method argument values
     * @param paramTypes ArrayList&lt;Class> of method argument data types
     * 
     * @return Object that represents the returned entity or null. The Object is of type ConcreteJavaBeanObject. A call to getInstance() will return the entity object produced by the data control method
     */
   public static final Object invokeOnDataControl(String methodName, ArrayList<String> paramNames,  ArrayList<Object> paramValues, ArrayList<Class> paramTypes){
             
        Object outcome  = null;    
        
        try {            
            outcome = AdfmfJavaUtilities.invokeDataControlMethod("MobileBackendDC", null,methodName,paramNames, paramValues, paramTypes);
            return outcome;          
            
        } catch (AdfInvocationException e) {
            McsSampleLogger logger = new McsSampleLogger();
            String message = e.getMessage();
            String severity = e.getSeverity();
            logger.logError("Invoke failed on DataControl: "+message+", severity: "+severity, "com.oracle.maf.sample.mobile.mbeans.utils.DataControlsUtil", "invokeOnDataControl");
            return outcome;
        }
    }
    
    /**
     * Method that simplifies calls to data control methods that require a single argument of type boolean
     * @param dataControl enum value that identifies the data control to invoke this method on
     * @param methodName The name of the method to invoke on a data control
     * @param argumentName name of the argument
     * @param argumentValue boolean value true/false
     * @return Object that represents the returned entity or null. The Object is of type ConcreteJavaBeanObject. A call to getInstance() will return the entity object produced by the data control method
     */
    public static final Object invokeDCSingleBooleanParameterMethod(String methodName, String argumentName, Boolean argumentValue){
        ArrayList<String>   paramNames = new ArrayList<String>();
        paramNames.add(argumentName);
        ArrayList<Object>   paramValues = new ArrayList<Object>();
        paramValues.add(argumentValue);
        ArrayList<Class>           paramTypes = new ArrayList<Class>();
        paramTypes.add(Boolean.class);
        return invokeOnDataControl(methodName,paramNames,paramValues,paramTypes);
    } 
    
    /**
     * Method that simplifies calls to data control methods that require a single argument of type String
     * @param dataControl enum value that identifies the data control to invoke this method on
     * @param methodName The name of the method to invoke on a data control
     * @param argumentName name of the argument
     * @param argumentValue String value
     * @return Object that represents the returned entity or null. The Object is of type ConcreteJavaBeanObject. A call to getInstance() will return the entity object produced by the data control method
     */
    public static final Object invokeDCSingleStringParameterMethod(String methodName, String argumentName, String argumentValue){
        ArrayList<String>   paramNames = new ArrayList<String>();
        paramNames.add(argumentName);
        ArrayList<Object>   paramValues = new ArrayList<Object>();
        paramValues.add(argumentValue);
        ArrayList<Class>           paramTypes = new ArrayList<Class>();
        paramTypes.add(String.class);
        return invokeOnDataControl(methodName,paramNames,paramValues,paramTypes);
    }
    
    /**
     * Method to quickla invoke a data control methods that doesn't require any argument to be passed
     * @param dataControl enum value that identifies the data control to invoke this method on
     * @param methodName The name of the method to invoke on a data control
     * @return Object that represents the returned entity or null. The Object is of type ConcreteJavaBeanObject. A call to getInstance() will return the entity object produced by the data control method
     */
    public static final Object invokeDCNoArgumentMethod(String methodName){
        ArrayList<String>   paramNames  = new ArrayList<String>();
        ArrayList<Object>   paramValues = new ArrayList<Object>();
        ArrayList<Class>    paramTypes  = new ArrayList<Class>();
        return invokeOnDataControl(methodName,paramNames,paramValues,paramTypes);
    }
    
    /**
     * Helper method to collect a custom analytic event. The custom event requires an event name and custom key-values 
     * defining the event properties. The event name is set to "MAF MCS Utilities Sample" in the addCustomAnalyticEvent 
     * method on the MobileBackendDC data control. The event name is what shows in the list of events logged for the MBE
     * in the Analytic window of the OraceMCS developer portal
     * 
     * @param customEventName name of the custom event. This sample uses three different name for custom events: mafmcsutility-food, mafmcsutility-electronics, mafmcsutility-apparel
     * @param eventProperties HashMap&lt;String,String> that defines custom key/value pairs of interest for a event
     * @return null
     */
    public static final Object addCustomAnalyticEvent(String customEventName, HashMap<String,String>eventProperties){
        
        ArrayList<String>   paramNames = new ArrayList<String>();
        paramNames.add("customEventName");
        paramNames.add("eventProperties");        
        ArrayList<Object>   paramValues = new ArrayList<Object>();
        paramValues.add(customEventName);
        paramValues.add(eventProperties);
        ArrayList<Class>           paramTypes = new ArrayList<Class>();
        paramTypes.add(String.class);
        paramTypes.add(HashMap.class);
        return invokeOnDataControl("addCustomAnalyticEvent",paramNames,paramValues,paramTypes);
    }   
    
    /**
     * Flushes collected custom and system events to the MCS MBE
     * @return null;
     */
    public static final Object sendAnalyticEventsToMCS(){
        return invokeDCNoArgumentMethod("postAnalyticEventsToServer");
    }
}
