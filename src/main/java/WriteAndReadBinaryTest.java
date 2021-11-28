package main.java;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.swing.plaf.synth.SynthScrollBarUI;

import main.java.lib.Message;

public class WriteAndReadBinaryTest {
    private static final int BUFFER_SIZE = 1024 * 1024; // 4KB
 
    public static void main(String[] args) throws NoSuchAlgorithmException {
        // if (args.length < 2) {
        //     System.out.println("Please provide input and output files");
        //     System.exit(0);
        // }
 
        String inputFile = "teste.exe";
        String outputFile = "buffer2 teste.exe";
 
 
        try (
            FileInputStream fis = new FileInputStream(inputFile);
            FileOutputStream fos = new FileOutputStream(outputFile);
            BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
        ) {
 
            // File selectedFile = Paths.get(inputFile).toFile();
            // int fileSize = (int) selectedFile.length();
            // int transmitionCount = fileSize / BUFFER_SIZE;
            // int lastTransmitionSize = fileSize % BUFFER_SIZE;


            byte[] buffer = new byte[BUFFER_SIZE];
            // byte[] lastBuffer;

            // for (int i = 0; i < transmitionCount; i++) {
            //     is.read(buffer);
            //     bos.write(buffer);
            // }
            // if (lastTransmitionSize > 0) {
            //     lastBuffer = new byte[lastTransmitionSize];
            //     is.read(lastBuffer);
            //     bos.write(lastBuffer);
            // }


            // while (is.read(buffer) != -1) {
            //     bos.write(buffer);
            //     // buffer = new byte[BUFFER_SIZE];
            // }
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                // System.out.println(bytesRead);
                if (bytesRead < BUFFER_SIZE) {
                    buffer = Arrays.copyOf(buffer, bytesRead);
                }
                bos.write(buffer);
            }
            bis.close();
            bos.flush();
            bos.close();

            FileInputStream fis2 = new FileInputStream(inputFile);
            BufferedInputStream bis2 = new BufferedInputStream(fis2, BUFFER_SIZE);

        
            File selectedFile = Paths.get(inputFile).toFile();
            var time = System.currentTimeMillis();
            System.out.println("======== entrou md5 #1 =============" + time);
            MessageDigest hash = MessageDigest.getInstance("MD5");
            hash.update(Files.readAllBytes(Paths.get(inputFile)));
            String checksum = byteArrayToHex(hash.digest());
            System.out.println(checksum);
            System.out.println("======== saiu md5 #1 =============" + (System.currentTimeMillis() - time));
            

            byte[] bytesBuffer = new byte[1024];
            int bytesRead1 = -1;
            var time2 = System.currentTimeMillis();
     
            System.out.println("======== entrou md5 #2 =============" + time2);
            while ((bytesRead1 = bis2.read(bytesBuffer)) != -1) {
                hash.update(bytesBuffer, 0, bytesRead1);
            }
     
            byte[] hashedBytes = hash.digest();
     
            String checksum2 = convertByteArrayToHexString(hashedBytes);
            System.out.println(checksum2);
            System.out.println("======== saiu md5 #2 =============" + (System.currentTimeMillis() - time2));

        } catch (NoSuchAlgorithmException | IOException ex) {
            ex.printStackTrace();
            // throw new Exception("Could not generate hash from file", ex);
        }
    }

    public static String byteArrayToHex(byte[] array) {
        StringBuilder hashString = new StringBuilder(array.length * 2);
        for(byte b: array)
           hashString.append(String.format("%02x", b));
        return hashString.toString().toUpperCase();
     }

     private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < arrayBytes.length; i++) {
            stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return stringBuffer.toString().toUpperCase();
    }
}