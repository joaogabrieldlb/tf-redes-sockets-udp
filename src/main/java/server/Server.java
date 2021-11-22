package main.java.server;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.security.InvalidParameterException;

import main.java.FileInfo;

public class Server 
{
    private final int port;
    private DatagramSocket socket = null;
    private FileInfo file = null;

    public Server(int port) throws InvalidParameterException
    {
        if(port < 0)
        {
            throw new InvalidParameterException();
        }
        this.port = port;
    }

    public void createAndListenSocket()
    {
        /*
        try
        {
            socket = new DatagramSocket(port);
            System.out.println("Server is listening on port " + port);
            byte[] incomingData = new byte[512];
            while(true)
            {
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                socket.receive(incomingPacket);
                byte[] data = incomingPacket.getData();
                ByteArrayInputStream in = new ByteArrayInputStream(data);
                ObjectInputStream is = new ObjectInputStream(in);
                file = (FileInfo) is.readObject();
                System.out.println("Received file: " + file.getFileName());
                System.out.println("Saving file to: " + file.getDestinationDirectory());
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if(socket != null)
            {
                socket.close();
            }
        }
        */
    }

    public static void main(String[] args) throws Exception 
    {
        if(args.length != 1)
        {
            printHelp();
            return;
        }

        try
        {
            Server server = new Server(Integer.parseInt(args[0]));
            server.createAndListenSocket();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
    }

    private static void printHelp()
    {
        System.out.println("Usage: Server <port>");
    }
}


