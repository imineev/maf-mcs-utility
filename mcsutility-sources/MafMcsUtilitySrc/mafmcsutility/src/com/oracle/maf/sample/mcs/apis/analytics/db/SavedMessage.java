package com.oracle.maf.sample.mcs.apis.analytics.db;

import java.util.HashMap;

/**
 *
 * For querying saved analytic events, this entity class holds a row of the Anatylitics table
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class SavedMessage {
    
    private String payload = "";
    private String headerString = "";
    
    public SavedMessage() {
        super();
    }


    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }

    public void setHeaderString(String headers) {
        this.headerString = headers;
    }

    public String getHeaderString() {
        return headerString;
    }
    
    /**
     * Parses the header String message to a HashMap that hold header key:value pairs
     * @return HashMap&gt;String, String>
     */
    public  HashMap<String,String> getHeaderMap(){
        
         HashMap<String,String> headerMap = new  HashMap<String,String>();
        
        if (headerString!= null && !headerString.isEmpty()) {
            //JSON string has format of "key:value," So the first array is a split by ",".
            String[] headerArray = this.headerString.split(",");
            //now we have an array of key:value
            for (int i = 0; i < headerArray.length; i++) {
                String[] keyValuePair = ((String)headerArray[i]).split(":");
               headerMap.put(keyValuePair[0], keyValuePair[1]);
            }
          }
         return headerMap;        
       }
}
