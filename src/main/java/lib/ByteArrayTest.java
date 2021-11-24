package main.java.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ByteArrayTest implements Serializable {

    public Command a;
    public short b;
    public String c;

    public ByteArrayTest(Command a, short b, String c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public static void main(String[] args) {
        var ba = new ByteArrayTest(Command.S, (short)2, "3");
        var baO = convertObjectToBytes(ba);
        System.out.println("ByteArray lenght: " + baO.length);

        var message = new Message('a',(short) 0, "");
        var messageO = convertObjectToBytes(message);
        int s = 0;
        System.out.println("Message lenght: " + messageO.length);
        System.out.println(++s);
        System.out.println(Command.A);
        System.out.println(Command.A.name());
        System.out.println(Command.A.name().length());
        System.out.println(Command.S.ordinal());
        System.out.println(Command.A.toString());
        System.out.println(Command.A.toString().length());
        System.out.println(Command.valueOf(String.valueOf('A')));
        System.out.println(Command.values()[0]);
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

