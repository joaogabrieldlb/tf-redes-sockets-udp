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
    private char c;
    private int s;
    private String d;
    
    public Message(char command, int sequence, String data) {
        this.c = command;
        this.s = sequence;
        this.d = data;
    }
    
    public char getCommand() {
        return c;
    }

    public int getSequence() {
        return s;
    }

    public String getData() {
        return d;
    }

	public static byte[] convertObjectToBytes(Message obj) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(obj);
			
			return baos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			
			return null;
		}
	}

    public static Message convertBytesToObject(byte[] bytes) {
		InputStream is = new ByteArrayInputStream(bytes);
		try (ObjectInputStream ois = new ObjectInputStream(is)) {
			
			return (Message) ois.readObject();
		} catch (IOException | ClassNotFoundException ioe) {
			ioe.printStackTrace();
			
			return null;
		}
	}
}
