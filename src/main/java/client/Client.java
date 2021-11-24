package main.java.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import main.java.lib.Message;

public class Client
{
    // long lastSendRate = Long.MAX_VALUE;
    // for (int i = 0; tranfe; i++)
    //      if (sendRate == lastSendRate)
    //          long sendRate = Math.round(Math.pow(2, i));
    //      else
    //          sendRate += 1;
    //      if (conection timeout)
    //          lastSendRate = sendRate;

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

    public static void main(String[] args)
    {
        // char[] chars = "ab".toCharArray();
        // var msg = new Message(chars, 1, null);
        // var byteArray = convertObjectToBytes(msg);
        // System.out.println(byteArray.length);
    }
}
