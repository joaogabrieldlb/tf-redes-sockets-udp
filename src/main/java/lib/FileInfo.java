package main.java.lib;
import java.io.Serializable;
// import java.nio.file.Path;

public class FileInfo implements Serializable
{
    // private Path filePath;
    public String fileName;
    public long fileSize;
    public String fileHash;
}
