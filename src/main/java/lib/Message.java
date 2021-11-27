package main.java.lib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


public class Message implements Serializable
{
    private enum CommandType { SYN, ACK, SYN_ACK, DATA, FIN };
    private CommandType command;
    private long sequence;
    private byte[] data;
    
    public Message(CommandType command, int sequence, byte[] data) 
    {
        this.command = command;
        this.sequence = sequence;
        this.data = data;
    }
    
    public CommandType getCommand()
    {
        return command;
    }

    public long getSequence() 
    {
        return sequence;
    }

    public byte[] getData() 
    {
        return data;
    }

	public static byte[] convertObjectToBytes(Message obj) 
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

    public static Message convertBytesToObject(byte[] bytes) 
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
