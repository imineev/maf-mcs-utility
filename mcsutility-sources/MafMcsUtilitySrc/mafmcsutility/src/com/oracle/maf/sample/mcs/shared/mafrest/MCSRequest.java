package com.oracle.maf.sample.mcs.shared.mafrest;

import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;

import java.util.HashMap;

import oracle.adfmf.dc.ws.rest.RestServiceAdapter;
import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;


/**
 *
 * The MCSRequest class wraps the REST Service Adapter request configuration, including the
 * http method, the payload, header parameters, rest uri and MAF REST connection name.
 * 
 * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public final class MCSRequest {

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    /**
     * ENUM class to select http method from to use with REST call
     */
    public enum HttpMethod {
        GET  {
            @Override
            public String toString() {
                return RestServiceAdapter.REQUEST_TYPE_GET;
            }
        },
        PUT  {
            @Override
            public String toString() {
                return RestServiceAdapter.REQUEST_TYPE_PUT;
            }
        },
        POST  {
            @Override
            public String toString() {
                return RestServiceAdapter.REQUEST_TYPE_POST;
            }
        },
        DELETE  {
            @Override
            public String toString() {
                return RestServiceAdapter.REQUEST_TYPE_DELETE;
            }
        },
        HEAD  {
            @Override
            public String toString() {
                return "HEAD";
            }
        }
    };

    private Object payload = "";
    
    private String connectionName = "";
    private String requestURI = "";

    private HttpMethod httpMethod = HttpMethod.GET;
    private  HashMap<String,String> httpHeaders = new  HashMap<String,String>();
    private MBEConfiguration mbeConfig = null;
    private int retryLimit = 1;

    /**
     * Adding an instance of MBEConfiguration to the MCSRequest allows the REST client to log messages on behalf of a
     * specific MBE instance, which allows better analyzes of log entries in case of an error when multiple backends are
     * used with this MCS MAF Utility. Generic logger is used in case where mbeConfig argument value is passed as null.
     * @param mbeConfig MBEConfiguration object containing the base configuration settings for the MBE. This object cannot be null!
     * @throws IllegalArgumentException if mbeConfig argument is null
     */
    public MCSRequest(MBEConfiguration mbeConfig) {
        super();
        
        if(mbeConfig == null){
            throw new IllegalArgumentException("MBEConfiguration object argument in constructor call of MCSRequest cannot be null.");
        }        
        this.mbeConfig = mbeConfig;
        this.setConnectionName(mbeConfig.getMafRestConnectionName());
    }


    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }

    /**
     * The MAF REST connection name is require. The REST connection name is defined in the connections.xml file in MAF and
     * holds the root URL for the REST service. For MCS this usually is the mobile backend REST root URL.
     * @param connectionName
     */
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getConnectionName() {
        return connectionName;
    }

    /**
     * The relative addressing that is appended the REST root URL to build the full REST URL pointing to a resource. The
     * URI contains Query Parameters if needed.
     * @param requestURI
     */
    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public String getRequestURI() {
        return requestURI;
    }

    /**
     * GET, POST, PUT, DELETE and PATCH method to define the operation to be executed on a MCS REST resource. 
     * @param httpMethod MCSRequest.HttpMethod enum entry
     */
    public void setHttpMethod(MCSRequest.HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public MCSRequest.HttpMethod getHttpMethod() {
        return httpMethod;
    }
    
    /**
     * Sets the http method name: GET, POST, PUT, DELETE, HEAD
     * @param httpMethod GET, POST, PUT, DELETE, HEAD string
     */
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = this.findHttpMethodForString(httpMethod);
    }
    
    /**
     * translates http method name  strings to MCSRequest.HttpMethod Enum
     * @param httpMethodString GET, PUT, POST, DELETE, HEAD as string
     * @return HttpMethod enum entry
     */
    private HttpMethod findHttpMethodForString(String httpMethodString){
        
        HttpMethod _httpMethod = HttpMethod.GET;
        
        String _methodString = httpMethodString.toUpperCase();
        
        switch (_methodString) {
        case "POST":
            _httpMethod = HttpMethod.POST;
           break;
        case "PUT":
            _httpMethod = HttpMethod.PUT;
           break;
        case "GET":
            _httpMethod = HttpMethod.GET;
           break;
        case "DELETE":
            _httpMethod = HttpMethod.DELETE;
            break;
        case "HEAD":
            _httpMethod = HttpMethod.HEAD;
            break;
        }
        return _httpMethod;
    }

    public String getHttpMethodAsString() {
        return httpMethod.toString();
    }

    public void setHttpHeaders( HashMap<String,String> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    public  HashMap<String,String> getHttpHeaders() {
        return httpHeaders;
    }

    public void setRetryLimit(int retryLimit) {
        this.retryLimit = retryLimit;
    }

    public int getRetryLimit() {
        return retryLimit;
    }

    public MBEConfiguration getMbeConfig() {
        return mbeConfig;
    }
}
