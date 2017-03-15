package com.oracle.maf.sample.mobile.mbeans.storage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import oracle.adfmf.framework.api.AdfmfJavaUtilities;
import oracle.adfmf.java.beans.PropertyChangeListener;
import oracle.adfmf.java.beans.PropertyChangeSupport;

/**
 *
 *
  * @author Frank Nimphius
 * @copyright Copyright (c) 2015, 2016 Oracle. All rights reserved.
 */
public class SharedObjectHandling {
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public SharedObjectHandling() {
        super();
    }
    
    
    private String displaysErrorMessage = "";

    
    /**
     * Creates file URL to selected documentName (files are deployed with the sample and are located in the appliation
     * .adf/META-INF directory) for the selected documentName for upload to Oracle MCS
     */
    protected String getSelectedDocumentNameAndPath(String documentName) {
        String dir = AdfmfJavaUtilities.getDirectoryPathRoot(AdfmfJavaUtilities.DownloadDirectory);
        String path = dir + "/" + documentName;
        // Encoding is necessary so the URL does not contain spaces - replace " " with "%20"
        return path.replace(" ", "%20");
    }

    /**
     * Returns the byte[] array with the content of the selected document as read from the file system
     * @return byte[] array of the document content
     */
    protected byte[] getDocumentContent(String documentName) {

        String filePathAndName = getSelectedDocumentNameAndPath(documentName);
        File documentFile = new File(filePathAndName);
        byte[] documentByteArray = null;
        
        if (documentFile.exists()) {            
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(documentFile);                
                documentByteArray = convertInputStreamToByteArray(fileInputStream);
                
                //documentByteArray = new byte[fileInputStream.available()];            
                //fileInputStream.read(documentByteArray);

            } catch (FileNotFoundException e) {
                setDisplaysErrorMessage("File " + filePathAndName + " not found.");
            } 
            try {
                fileInputStream.close();
            } catch (IOException e) {
                //could not close file handle. However, to this point we are unable to
                //do anything here. So lets just share this information with the user
                this.setDisplaysErrorMessage("Successed reading file " + filePathAndName +
                                             ", but failed releasing handle to it.");
            }
        }
        else{
            setDisplaysErrorMessage("File" + filePathAndName +"does not exist.");
        }

        return documentByteArray;
    }
    
    private byte[] convertInputStreamToByteArray(InputStream inputStream) {
        
        if(inputStream == null)
            return new byte[0];

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] buff = new byte[2048];
        int bytesRead;

        try {
            while ((bytesRead = inputStream.read(buff)) != -1) {
                outputStream.write(buff, 0, bytesRead);
            }
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] content = outputStream.toByteArray();

        return content;
    }
    
    /**
     * Return the MIME type for the selected document
     * @return
     */
    protected String getSelectedDocumentMimeType(String documentName) {
        String mime = "image/png";
        switch (documentName) {
        case "png.png":
            mime = "image/png";
            break;
        case "doc.docx":
            mime = "application/msword";
            break;
        case "ppt.pptx":
            mime = "application/vnd.ms-powerpoint";
            ;
            break;
        case "pdf.pdf":
            mime = "application/pdf";
            break;
        case "mp4.mp4":
            mime = "video/mp4";
            break;
        default:
            mime = "image/png";
        }
        return mime;
    }

    public void setDisplaysErrorMessage(String displaysErrorMessage) {
        String oldDisplaysErrorMessage = this.displaysErrorMessage;
        this.displaysErrorMessage = displaysErrorMessage;
        propertyChangeSupport.firePropertyChange("displaysErrorMessage", oldDisplaysErrorMessage, displaysErrorMessage);
    }

    public String getDisplaysErrorMessage() {
        return displaysErrorMessage;
    }


    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
}
