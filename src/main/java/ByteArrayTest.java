package main.java;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class ByteArrayTest {
    public static void main(String[] args) {
        var msg = new Message(CommandCode.ACK,1,null);
        var byteArray = convertObjectToBytes(msg);
        System.out.println(byteArray.length);
    }

    private static byte[] convertObjectToBytes(Object object)
    {
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
		try (ObjectOutputStream ois = new ObjectOutputStream(boas))
        {
			ois.writeObject(object);
			return boas.toByteArray();
		}
        catch (IOException ioe)
        {
			ioe.printStackTrace();
			return null;
		}
    }
}

