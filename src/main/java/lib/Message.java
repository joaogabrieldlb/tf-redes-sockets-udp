package main.java.lib;

import java.io.Serializable;

public class Message implements Serializable
{
    static final int PAYLOAD_SIZE = 512;

    public enum CommandType { SYN, SYN_ACK, ACK, UPLOAD, DOWNLOAD, DATA, SUCCESS, FAILURE, FIN, RST, TIMEOUT };

    private CommandType command;
    private long sequence;
    private byte[] data;

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
