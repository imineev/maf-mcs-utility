package com.oracle.maf.sample.mcs.apis.analytics;

import java.util.Date;
import java.util.UUID;

/**
 *
 *  *** INTERNAL USE ONLY  ***
 *
 * Analytic Events in Mobile Cloud Service (MCS) have a Context and are grouped into Sessions.
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class Session {

    private String mSessionId   = null;
    private Date mStartTime     = null;
    private Date mEndTime       = null;

    protected Session(){
        //create a universally unique identifier for the session Id
        mSessionId = UUID.randomUUID().toString();
        mStartTime = new Date();
    }

    public String getSessionId() {
        return mSessionId;
    }


    public Date getStartTime() {
        return mStartTime;
    }

    public Date getEndTime() {
        return mEndTime;
    }

    public void setEndTime(Date mEndTime) {
        this.mEndTime = mEndTime;
    }
}
