package com.oracle.maf.sample.mcs.shared.mbe.config;

import com.oracle.maf.sample.mcs.shared.mbe.config.base.MBEConfiguration;

import oracle.adfmf.framework.exception.IllegalArgumentException;

/**
 *
 * MAF MCS Utility supports MCS basic and OAUTH authentication. This configuration class is created to simplify the creation
 * of MBEConfiguration objects that use OAUTH authentication. The resulting configuration object is accessed from internally
 * but can also be accessed from MAF applications through the MBE.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class OauthMBEConfiguration extends MBEConfiguration {
    /**
     * Creates an instance of Mobile Backend Configuration for OAUTH authentication. Each instance contains the configuration for an instance
     * of MobileBackend, allowing the MobileBackend instance to access the remore MBE in MCS.
     *
     * @param mafRestConnectionName Name of the REST connection created in the MAF application for the MBE Root URL. The REST connection must contain the base URL of the MBE
     * @param oauthEndpointConnectionName Name of the REST connection created in the MAF application for the OAuth endpoint listed in the MBE settings. If authentication is through MAF then this value can be set to null
     * @param oAuthClientId Unique string identifier for the remote OAUTH client (MBE)
     * @param oAuthClientKey client secret used for authenticating MAF MCS Utility MBE to the MCS OAUTH server
     * @param mbeClientApplicationKey application client key created in the MCS MBE for Android and iOS. The key is required for the Analytic feature. Note that the key values are different for Android and iOS. Ensure the correctkey to be passed at runtime.
     * @param identityDomain The Oracle Cloud identity domain (not the realm) that the user is associated with 
     *
     * @throws IllegalArgumentException if mafRestConnectionName or mobileBackendIdentifier is null or empty. Note that these two vaues are the minimal configuration required to instantiate an MBE instance
     */
    public OauthMBEConfiguration(String mafRestConnectionName, String oauthEndpointConnectionName, String oAuthClientId, String oAuthClientKey, String mbeClientApplicationKey, String identityDomain) throws IllegalArgumentException {
       super(mafRestConnectionName, oauthEndpointConnectionName, oAuthClientId, oAuthClientKey, mbeClientApplicationKey, identityDomain);
    }
}
