package com.oracle.maf.sample.mcs.shared.mbe.error;


/**
 *
 * Oracle Mobile Cloud Service (MCS) Error Message Information. If the error message is an application level error message,
 * that is an error returned by the MCS REST API due to misconfiguration or access failure, then the information is a well
 * structured JSON payload that can be parsed into this object for easy use. This class is used in the com.oracle.maf.sample
 * .mcs.shared.maf.MCSResponse
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public final class OracleMobileError {
    
    public static final String MCS_ERROR_TYPE = "type";
    public static final String MCS_ERROR_STATUS = "status";
    public static final String MCS_ERROR_TITLE = "title";
    public static final String MCS_ERROR_DETAIL = "detail";
    public static final String MCS_ERROR_ORACLE_ERROR_CODE = "0.errorCode";
    public static final String MCS_ERROR_ORACLE_ERROR_PATH = "o.errorPath";
    public static final String MCS_ERROR_ORACLE_ECID = "o:ecid";
    
    private String type = null;
    private int status;
    private String title = null;
    private String detail = null;
    private String oracleErrorCode = null;
    private String oracleErrorPath = null;
    private String oracleEcid = null;
        
    public OracleMobileError() {
        super();
    }


    public void setType(String type) {
        this.type = type;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public void setOracleErrorCode(String oracleErrorCode) {
        this.oracleErrorCode = oracleErrorCode;
    }

    public void setOracleErrorPath(String oracleErrorPath) {
        this.oracleErrorPath = oracleErrorPath;
    }


    public void setOracleEcid(String oracleEcid) {
        this.oracleEcid = oracleEcid;
    }

    /**
     * Eror type like "http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.1"
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     * HTTP status code
     * @return
     */
    public int getStatus() {
        return status;
    }

    /**
     * Short error messge like: "Mobile Backend not found"
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     * Long error message like "We cannot find the active mobile backend for the given 
     * clientId 123456-5c5c-4ec0-8383-af622f5e9fc2 and BASIC schema. Specify a valid 
     * clientId and try again."
     * 
     * @return
     */
    public String getDetail() {
        return detail;
    }

    /**
     * Oracle product specific error code like: MOBILE-58026
     * @return
     */
    public String getOracleErrorCode() {
        return oracleErrorCode;
    }

    /**
     * URI Path for which the error is raised: /mobile/platform/users/jdoe
     * @return
     */
    public String getOracleErrorPath() {
        return oracleErrorPath;
    }


    /**
     * Execution context ID (ECID) is a unique identifier to createOrUpdateObject a request into a context. An example ecid String
     * is 21e6f2ab6fd44786:5231ac6:14c22bc6f09:-8000-0000000000010010, 0. ECID allows an administrator to track the
     * flow of a particular request.
     * @return
     */
    public String getOracleEcid() {
        return oracleEcid;
    }            
}
