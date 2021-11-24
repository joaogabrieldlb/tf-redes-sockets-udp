package main.java.lib;
import java.io.Serializable;

public class FileInfo implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String destinationDirectory;
    private String sourceDirectory;
    private String fileName;
    private long fileSize;
    private byte[] fileData;

    public String getDestinationDirectory() 
    {
        return destinationDirectory;
    }

    public void setDestinationDirectory(String destinationDirectory) 
    {
        this.destinationDirectory = destinationDirectory;
    }

    public String getSourceDirectory() 
    {
        return sourceDirectory;
    }

    public void setSourceDirectory(String sourceDirectory) 
    {
        this.sourceDirectory = sourceDirectory;
    }

    public String getFileName() 
    {
        return fileName;
    }

    public void setFileName(String fileName) 
    {
        this.fileName = fileName;
    }

    public long getFileSize() 
    {
        return fileSize;
    }

    public void setFileSize(long fileSize) 
    {
        this.fileSize = fileSize;
    }

    public byte[] getFileData() 
    {
        return fileData;
    }

    public void setFileData(byte[] fileData) 
    {
        this.fileData = fileData;
    }
}
