

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ObjectConverter 
{
	public static byte[] convertObjectToBytes(Object obj) 
    {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) 
        {
			oos.writeObject(obj);
			return baos.toByteArray();
		} 
        catch (IOException e) 
        {
			e.printStackTrace();
			return null;
		}
	}

    public static Object convertBytesToObject(byte[] bytes) 
    {
		InputStream is = new ByteArrayInputStream(bytes);
		try (ObjectInputStream ois = new ObjectInputStream(is)) 
        {
			return (Message) ois.readObject();
		} 
        catch (IOException | ClassNotFoundException ioe) 
        {
			ioe.printStackTrace();
			return null;
		}
	}
}