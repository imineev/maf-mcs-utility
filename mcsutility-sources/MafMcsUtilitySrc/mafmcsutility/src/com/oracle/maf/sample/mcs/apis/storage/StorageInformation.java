package com.oracle.maf.sample.mcs.apis.storage;

import java.util.ArrayList;

import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;

/**
 * Entity class that holds information about a list of storage collections queried from an Oracle MBE instance. A query to MCS for all
 * collections of an MBE returns the following REST response (note that the response below only shows content that is accessible through
 * this class
 *
 * {
 *   "items": [
 *     {
 *     "id": "PrivateContent",
 *     "description": "Private user library\n",
 *     "userIsolated": true,
 *     "contentLength": 0,
 *     "eTag": "\"1.0\"",
 *     },
 *     {
 *     "id": "PublicBooks",
 *     "description": "Public book library",
 *     "userIsolated": false,
 *     "contentLength": 0,
 *     "eTag": "\"1.0\""
 *     }
 *   ],
 *   "hasMore": false,
 *   "limit": 100,
 *   "offset": 0,
 *   "count": 2,
 *   "totalResults": 2
 *   }
 *
 * @author   Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class StorageInformation {
    
    private boolean hasMore = false;
    private int     limit = 0; 
    private int     offset = 0;
    private int     count = 0;
    private int     totalResults = 0;
    
    private ArrayList<StorageCollection> items = new ArrayList<StorageCollection>();
    
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public StorageInformation() {
        super();
    }

    /**
     * INTERNAL API - Applications should not use this method
     * indicates if the MBE instance has more collections than could be queried because of the query limit set. The
     * default query limit is 100 but can be overridden in the call to Storage :: getListOfStorageCollection
     * @param hasMore
     */
    public void setHasMore(boolean hasMore) {
        boolean oldHasMore = this.hasMore;
        this.hasMore = hasMore;
        propertyChangeSupport.firePropertyChange("hasMore", oldHasMore, hasMore);
    }

    /**
     * If there are more collections available for an MBE that could not be queried and that require a seond request 
     * with a new offset value 
     * @return
     */
    public boolean isHasMore() {
        return hasMore;
    }

    /**
     * The limit value that determined the number of collection information queried from the MCS MBE
     * @param limit
     */
    public void setLimit(int limit) {
        int oldLimit = this.limit;
        this.limit = limit;
        propertyChangeSupport.firePropertyChange("limit", oldLimit, limit);
    }

    /**
     * number of collections that could be queried max. from a MBE. If this value is set to a value below the totalNumber of 
     * collections available then chances are that no all collections are contained in the queried information
     * @return
     */
    public int getLimit() {
        return limit;
    }

    /**
     * INTERNAL API - Applications should not use this method
     * @param offset
     */
    public void setOffset(int offset) {
        int oldOffset = this.offset;
        this.offset = offset;
        propertyChangeSupport.firePropertyChange("offset", oldOffset, offset);
    }

    /**
     * The start index (counting starts with 0) for the queried collection from a MCS MBE
     * @return
     */
    public int getOffset() {
        return offset;
    }

    /**
     * INTERNAL API - Applications should not use this method
     * @param count
     */
    public void setCount(int count) {
        int oldCount = this.count;
        this.count = count;
        propertyChangeSupport.firePropertyChange("count", oldCount, count);
    }

    /**
     * The number of collections contained in the response to the query
     * @return
     */
    public int getCount() {
        return count;
    }

     /**
      * INTERNAL API - Applications should not use this method
     * @param totalResults
     */
    public void setTotalResults(int totalResults) {
        int oldTotalResults = this.totalResults;
        this.totalResults = totalResults;
        propertyChangeSupport.firePropertyChange("totalResults", oldTotalResults, totalResults);
    }

    /**
     * Returns the total number vaue of collections available in MCS MBE. This is only the numeric value of the number
     * of collections in MCS and is not the number of collections contained in the uery response
     * @return
     */
    public int getTotalResults() {
        return totalResults;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }


    /**
     * INTERNAL API - Applications should not use this method
     * @param items
     */
    public void setItems(ArrayList<StorageCollection> items) {
        ArrayList<StorageCollection> oldItems = this.items;
        this.items = items;
        propertyChangeSupport.firePropertyChange("items", oldItems, items);
    }

    /**
     * Method that returns a list of StorageObject queried from the MCS MBE based on the query conditions specified in 
     * Storage :: getListOfStorageCollection
     * @return
     */
    public ArrayList<StorageCollection> getItems() {
        return items;
    }
}
