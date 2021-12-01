import java.io.Serializable;

public class Message implements Serializable
{
    static final int PAYLOAD_SIZE = 512;

    private CommandType command;
    private long sequence;
    private byte[] data;

    public Message(CommandType command, long sequence)
    {
        this.command = command;
        this.sequence = sequence;
    }
    
    public Message(CommandType command, long sequence, byte[] data)
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
