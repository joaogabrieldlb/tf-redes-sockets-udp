
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileInfo implements Serializable
{
    //private Path filePath;
    private int fileBufferSize;
    private String fileName;
    private long fileSize;
    private long totalPackets;
    private String fileHash;

    public FileInfo(File file, int fileBufferSize)
    {
        try
        {
            if (!Files.isReadable(file.toPath()))
            {
                System.out.println("Arquivo não encontrado.");
                return;
            }
            
            this.fileName = file.getName();
            this.fileSize = file.length();
            this.fileBufferSize = fileBufferSize;
            this.totalPackets = (long) Math.ceil((double) this.fileSize / this.fileBufferSize);
            this.fileHash = computeMD5(file);
        }
        catch (NoSuchAlgorithmException | IOException e)
        {
            e.printStackTrace();
        }
    }

    public int getFileBufferSize() {
        return fileBufferSize;
    }

    public long getTotalPackets() {
        return totalPackets;
    }

    public String getFileHash() {
        return fileHash;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public static String computeMD5(File file) throws NoSuchAlgorithmException, IOException
    {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(Files.readAllBytes(file.toPath()));
        byte[] digest = md.digest();
        return byteArrayToHex(digest);
    }
    
    private static String byteArrayToHex(byte[] array) {
        StringBuilder hashString = new StringBuilder(array.length * 2);
        for(byte b: array)
           hashString.append(String.format("%02x", b));
        return hashString.toString().toUpperCase();
    }
}
