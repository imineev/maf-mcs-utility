package com.oracle.maf.sample.mcs.apis.policy;

import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;

/**
 * Entity class for application Policies
 *
 * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */

public class Policy {
    
    String policyName = "";
    Object policyValue = null;
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public Policy() {
        super();
    }

    public void setPolicyName(String policyName) {
        String oldPolicyName = this.policyName;
        this.policyName = policyName;
        propertyChangeSupport.firePropertyChange("policyName", oldPolicyName, policyName);
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyValue(Object policyValue) {
        Object oldPolicyValue = this.policyValue;
        this.policyValue = policyValue;
        propertyChangeSupport.firePropertyChange("policyValue", oldPolicyValue, policyValue);
    }

    public Object getPolicyValue() {
        return policyValue;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
}
