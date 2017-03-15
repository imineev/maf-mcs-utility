package com.oracle.maf.sample.mcs.shared.utils;

import java.util.Iterator;
import java.util.Map;

/**
 *
 *  *** INTERNAL USE ONLY  ***
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class MapUtils {
    private MapUtils() {}
    
    /**
     * Flattens properties saved in a HashMap into an array representation of key/value pairs
     * 
     * @param properties
     * @return String of key/value pairs
     */
    public static String dumpStringProperties(Map<String,String> properties){
        if(properties != null){
        Iterator keyIterator = properties.keySet().iterator();
        
        StringBuffer propertiesDump = new StringBuffer();
        propertiesDump.append("[");
        while(keyIterator.hasNext()){
            String key = (String) keyIterator.next();
            propertiesDump.append(key+":"+properties.get(key));
            if(keyIterator.hasNext()){
                propertiesDump.append(";");
            }
            else{
                propertiesDump.append("]");
            }
        }
        return  propertiesDump.toString();
      }
        return "";
    }
    
    /**
     * Flattens properties saved in a HashMap into an array representation of key/value pairs
     * 
     * @param properties
     * @return String of key/value pairs
     */
    public static String dumpObjectProperties(Map<String,Object> properties){
        if(properties != null){
        
        Iterator keyIterator = properties.keySet().iterator();
        
        StringBuffer propertiesDump = new StringBuffer();
        propertiesDump.append("[");
        while(keyIterator.hasNext()){
            String key = (String) keyIterator.next();
            Object obj = properties.get(key);
            
            if (obj instanceof String){
                propertiesDump.append(key+":"+(String)properties.get(key));
            }
            else {
                propertiesDump.append(key+":"+properties.get(key));
            }
                        
            if(keyIterator.hasNext()){
                propertiesDump.append(";");
            }
            else{
                propertiesDump.append("]");
            }
        }
        return  propertiesDump.toString();
      }
       return "";
    }
    
}
