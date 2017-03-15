package com.oracle.maf.sample.mcs.shared.mbe.config;

import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;

import oracle.adfmf.framework.exception.IllegalArgumentException;

/**
 *
 * MAF MCS Utility supports MCS basic and OAUTH authentication. This configuration class is created to simplify the creation
 * of MBEConfiguration objects that use basic authentication. The resulting configuration object is accessed from internally
 * but can also be accessed from MAF applications through the MBE.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class BasicAuthMBEConfiguration extends MBEConfiguration{
    /**
     * Creates an instance of Mobile Backend Configuration for basic authentication. Each instance contains the configuration for an instance
     * of MobileBackend, allowing the MobileBackend instance to access the remore MBE in MCS.
     *
     * @param mafRestConnectionName Name of the REST connection creat in the MAF application. The REST connection must contain the base URL of the MBE
     * @param mobileBackendId Unique string identifier for the remote Mobile Backend in MCS. Use the MCS portal to obtain the ID
     * @param mbeAnonymousKey Unique string identifier used with analytics and diagnostics. Use the MCS portal to obtain the ID
     * @param mbeClientApplicationKey application client key created in the MCS MBE for Android and iOS. The key is required for the Analytic feature. Note that the key values are different for Android and iOS. Ensure the correctkey to be passed at runtime.
     *
     * @throws IllegalArgumentException if mafRestConnectionName or mobileBackendIdentifier is null or empty. Note that these two vaues are the minimal configuration required to instantiate an MBE instance
     */    
    public BasicAuthMBEConfiguration(String mafRestConnectionName, String mobileBackendId, String mbeAnonymousKey, String mbeClientApplicationKey) throws IllegalArgumentException {    
        super(mafRestConnectionName,mobileBackendId,mbeAnonymousKey,mbeClientApplicationKey);        
    }
}
