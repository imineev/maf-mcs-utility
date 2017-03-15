package com.oracle.maf.sample.mcs.shared.mbe.proxy;

import com.oracle.maf.sample.mcs.shared.mbe.MBE;


/**
 *
 * A mobile backend (MBE) provides a set of services - e.g. analytics, or notification - that mobile clients can invoke.
 * This abstract class defines methods and constants that are common for all services.
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public abstract class MBEServiceProxy {

    MBE mbe = null;

    public void setMbe(MBE mbe) {
        this.mbe = mbe;
    }

    public MBE getMbe() {
        return mbe;
    }
    
    //wrap instance of Mobile Backend
    protected void init(MBE mbe){
        this.mbe = mbe;
    }

}
