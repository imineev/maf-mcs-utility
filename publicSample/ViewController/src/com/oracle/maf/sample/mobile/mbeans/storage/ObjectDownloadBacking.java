package com.oracle.maf.sample.mobile.mbeans.storage;

import com.oracle.maf.sample.mobile.mbeans.utils.DataControlsUtil;

import java.io.UnsupportedEncodingException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import java.util.ArrayList;

import oracle.adf.model.datacontrols.device.DeviceManagerFactory;

import oracle.adfmf.amx.event.ActionEvent;
import oracle.adfmf.amx.event.ValueChangeEvent;
import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;


/**
 *
 * Managed bean that downloads file from MCS Storage (through data control). The file is then attempted to be displayed
 * using DeviceManagerFactory.getDeviceManager().displayFile(). If the file cannot be opened because the device does not
 * support the file type (MimeType) then this display fails with an unhandled error. Play it safe and use image with this
 * MAF MCS Utility sample and you will be okay.
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class ObjectDownloadBacking extends SharedObjectHandling{
    
    
    private String tabMenuSelection =  ((Boolean) AdfmfJavaUtilities.getELValue("#{preferenceScope.application.more.showintroduction}"))==true? "instructions":"content";
    
    // MCS Collection Object Attributes
    private String objectId          = null;
    private String displayName       = null;
    private String contentType       = null;
    private String createdOn         = null;
    private String createdBy         = null; 
    private String modifiedOn        = null;
    private String modifiedBy        = null;
    private String eTag              = null;
    private String contentLength     = null;
    private String targetFileName    = null;
    private String canonicalLink     = null;
        
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);


    public void setObjectId(String objectId) {
        String oldObjectId = this.objectId;
        this.objectId = objectId;
        propertyChangeSupport.firePropertyChange("objectId", oldObjectId, objectId);
    }

    public String getObjectId() {
        return objectId;
    }

    public void setDisplayName(String displayName) {
        String oldDisplayName = this.displayName;
        this.displayName = displayName;
        propertyChangeSupport.firePropertyChange("displayName", oldDisplayName, displayName);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setContentType(String contentType) {
        String oldContentType = this.contentType;
        this.contentType = contentType;
        propertyChangeSupport.firePropertyChange("contentType", oldContentType, contentType);
    }

    public String getContentType() {
        return contentType;
    }

    public void setCreatedOn(String createdOn) {
        String oldCreatedOn = this.createdOn;
        this.createdOn = createdOn;
        propertyChangeSupport.firePropertyChange("createdOn", oldCreatedOn, createdOn);
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedBy(String createdBy) {
        String oldCreatedBy = this.createdBy;
        this.createdBy = createdBy;
        propertyChangeSupport.firePropertyChange("createdBy", oldCreatedBy, createdBy);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setModifiedOn(String modifiedOn) {
        String oldModifiedOn = this.modifiedOn;
        this.modifiedOn = modifiedOn;
        propertyChangeSupport.firePropertyChange("modifiedOn", oldModifiedOn, modifiedOn);
    }

    public String getModifiedOn() {
        return modifiedOn;
    }

    public void setModifiedBy(String modifiedBy) {
        String oldModifiedBy = this.modifiedBy;
        this.modifiedBy = modifiedBy;
        propertyChangeSupport.firePropertyChange("modifiedBy", oldModifiedBy, modifiedBy);
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setETag(String eTag) {
        String oldETag = this.eTag;
        this.eTag = eTag;
        propertyChangeSupport.firePropertyChange("eTag", oldETag, eTag);
    }

    public String getETag() {
        return eTag;
    }

    public void setContentLength(String contentLength) {
        String oldContentLength = this.contentLength;
        this.contentLength = contentLength;
        propertyChangeSupport.firePropertyChange("contentLength", oldContentLength, contentLength);
    }

    public String getContentLength() {
        return contentLength;
    }


    public void setCanonicalLink(String canonicalLink) {
        String oldCanonicalLink = this.canonicalLink;
        this.canonicalLink = canonicalLink;
        propertyChangeSupport.firePropertyChange("canonicalLink", oldCanonicalLink, canonicalLink);
    }

    public String getCanonicalLink() {
        return canonicalLink;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }


    public void setTargetFileName(String targetFileName) {
        String oldTargetFileName = this.targetFileName;
        this.targetFileName = targetFileName;
        propertyChangeSupport.firePropertyChange("targetFileName", oldTargetFileName, targetFileName);
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    /**
     * Download the object from MCS Storage, save it on the local file system and display the file.
     * The first two tasks are handled by the MobileBackendDC data control.
     * @param event AMX action event
     */
    public void downloadObject(ActionEvent event){        
        
        //reset error messages
        setDisplaysErrorMessage("");
        
        //prepare arguments
        ArrayList<String> parameterNames = new ArrayList<String>();
        parameterNames.add("uri");
        parameterNames.add("mimeType");
        parameterNames.add("targetFileName");     
        
        ArrayList<Object> parameterValues = new ArrayList<Object>();
        parameterValues.add(this.getCanonicalLink());
        parameterValues.add(this.getContentType());
        parameterValues.add(this.verifyValidFileName());
        
        ArrayList<Class> parameterTypes = new ArrayList<Class>();
        parameterTypes.add(String.class);
        parameterTypes.add(String.class);
        parameterTypes.add(String.class);
        
        //invoke method exposed on the MobileBackendDC data control                        
        String fileLocation = (String) DataControlsUtil.invokeOnDataControl("downloadStorageObject", parameterNames, parameterValues, parameterTypes);
        
        //from here on, its handled by the mobile device. If the device doesn't kow how to display the file, an exception 
        //with be thrown that is not handled by this sample. Good Luck!
        if(fileLocation != null){
            
            /* from the MAF docs
             * Attempts to display file that is located on device. For a list of file types supported on iOS, see 
             * 
             * http://developer.apple.com/library/ios/#documentation/FileManagement/Conceptual/DocumentInteraction_TopicsForIOS
             * /Articles/UsingtheQuickLookFramework.html 
             * 
             * On Android, this method attempts to open the file using an app associated with the file's MIME type. The MIME type is 
             * derived from the file extension. If no such app is installed, an error is displayed. The file path represents a URL on 
             * iOS and thus must be encoded. 
             * 
             * In particular, the iOS parser does not handle "+" as an encoding for space. Instead, "%20" 
             * must be used. Furthermore, if slashes are encoded to "%2F", then the leading slash must not be encoded. It must remain a slash. 
            */
            String os = DeviceManagerFactory.getDeviceManager().getOs();
            
            //use iOS encoded file reference
            DeviceManagerFactory.getDeviceManager().displayFile(os.equalsIgnoreCase("IOS")==true? iOSPathEncoding(fileLocation):androidPathEncoding(fileLocation) ,this.getDisplayName());
        }
        else{
            setDisplaysErrorMessage("Not a valid file name to display");
        }
    }
    
    /**
     * Ensure a vald file name even if extension is not provided or no value provided
     * @return
     */
    private String verifyValidFileName(){
        
        String fileName = "mafmcsutilityfile";
        String extensionBasedOnMimeType = "";
        
        //check if name was provided
        if(targetFileName != null && !targetFileName.isEmpty()){
            extensionBasedOnMimeType = this.getFileExtension(this.getContentType());
            if (!targetFileName.endsWith(extensionBasedOnMimeType)){
                //add extension
                fileName = targetFileName+extensionBasedOnMimeType;
            }
        }
        else{
            //no file name provided, so we default the name
            extensionBasedOnMimeType = getFileExtension(this.getContentType());
            fileName = fileName+extensionBasedOnMimeType;
        }
        
        return fileName;
    }
    
    
    /**
     * The file path represents a URL on iOS and thus must be encoded. In particular, the iOS parser does not handle "+" 
     * as an encoding for space. Instead, "%20" must be used. Furthermore, if slashes are encoded to "%2F", then the 
     * leading slash must not be encoded. It must remain a slash. 
     * 
     * @param fileLocation location where the fie is saved to
     * @return iOS encoded String 
     */
    private String iOSPathEncoding(String fileLocation){
        
        StringBuffer buffer = new StringBuffer();      
        String filePath     = null;
        
        try {
        filePath = URLEncoder.encode(fileLocation, "UTF-8");        
        
        // replace "+" with "%20"
         String stringToReplace = "+";
         String replacementString = "%20";
         int index = 0, previousIndex = 0;
         index = filePath.indexOf(stringToReplace, index);
        
         while (index != -1)
         {
           buffer.append(filePath.substring(previousIndex, index)).append(replacementString);
           previousIndex = index + 1;
           index = filePath.indexOf(stringToReplace, index + stringToReplace.length());
         }
        
         buffer.append(filePath.substring(previousIndex, filePath.length()));
         
         // revert the leading encoded slash ("%2F") to a literal slash ("/")
         if (buffer.indexOf("%2F") == 0)
         {
           buffer.replace(0, 3, "/");
         }
                           
         URL localURL = new URL("file", "localhost", buffer.toString());
         String localURLString = localURL.toString(); 
         
         return localURLString;
            
        } catch (UnsupportedEncodingException e) {
            setDisplaysErrorMessage("Failed iOS encoding: "+e.getMessage());
            
        } catch (MalformedURLException e) {
            setDisplaysErrorMessage("Failed to create file URL: "+e.getMessage());
            
        }
        
        return null;
    }
    
    
    /**
     * Android ocation encoding is similar to iOS. To be flexible for future changes, this class uses a separate method 
     * for Android
     * @param fileLocation the file location of the downloaded string as returned by the data control
     * @return The URL of the file to display or null if path parsing failed
     */
    private String androidPathEncoding(String fileLocation){
        
        StringBuffer buffer    = new StringBuffer();     
        String filePath        = null;
            
        try {
          filePath = URLEncoder.encode(fileLocation, "UTF-8");                    
          // replace "+" with "%20"
          String stringToReplace = "+";
          String replacementString = "%20";
          int index = 0, previousIndex = 0;
          
          index = filePath.indexOf(stringToReplace, index);               
          while (index != -1){
                buffer.append(filePath.substring(previousIndex, index)).append(replacementString);
                previousIndex = index + 1;
                index = filePath.indexOf(stringToReplace, index + stringToReplace.length());
           }
              
           buffer.append(filePath.substring(previousIndex, filePath.length()));             
           // revert the leading encoded slash ("%2F") to a literal slash ("/")
           if (buffer.indexOf("%2F") == 0)
           {
              buffer.replace(0, 3, "/");
            }                               

           URL localURL = new URL("file", "", buffer.toString()); 
           String localURLString = localURL.toString();
                 
           return localURLString;                  

         } catch (UnsupportedEncodingException e) {
             setDisplaysErrorMessage("Failed to create Android file encoding: "+e.getMessage());
         } catch (MalformedURLException e) {
             setDisplaysErrorMessage("Failed to create file URL: "+e.getMessage());               
         }            
                return null;
    }
    
    /**
     * return file extension according to mime type
     * @param mimeType
     * @return
     */
    private String getFileExtension(String mimeType){
        
        //sample documents used in this sample are
        //doc.docx", "mp4.mp4", "pdf.pdf", "ppt.pptx", "xls.xlsx","png.png"         
        String fileExtension = ".png";        
        switch (mimeType) {
            case "image/png":
                fileExtension = ".png";
                break;
            case "application/msword":
                fileExtension = ".docx";
                break;
            case "application/vnd.ms-powerpoint":
                fileExtension = ".pptx";
                break;
            case "application/pdf":
                fileExtension = ".pdf";
                break;
            case "video/mp4":
                fileExtension = ".mp4";
                break;
            default:
                fileExtension = ".png"; 
        }
        return fileExtension;
    }    
    
    /**
     * Switches the value of the deck component, displaying a different area of the settings
     * @param valueChangeEvent
     */
    public void onTabMenuSelect(ValueChangeEvent valueChangeEvent) {
        setTabMenuSelection((String)valueChangeEvent.getNewValue());
    }

    public void setTabMenuSelection(String tabMenuSelection) {
        String oldTabMenuSelection = this.tabMenuSelection;
        this.tabMenuSelection = tabMenuSelection;
        propertyChangeSupport.firePropertyChange("tabMenuSelection", oldTabMenuSelection, tabMenuSelection);
    }

    public String getTabMenuSelection() {
        return tabMenuSelection;
    }
}
