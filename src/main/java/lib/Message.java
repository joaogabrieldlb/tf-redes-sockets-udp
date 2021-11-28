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
    static final int PAYLOAD_SIZE = 512;

    public enum CommandType { SYN, ACK, SYN_ACK, RST, FIN, UPLOAD, DOWNLOAD, DATA, CHECKSUM, TIMEOUT };

    private CommandType command;
    private long sequence;
    private byte[] data = new byte[PAYLOAD_SIZE];

    public Message(CommandType command, long sequence)
    {
        this.command = command;
        this.sequence = sequence;
    }
    
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
}
