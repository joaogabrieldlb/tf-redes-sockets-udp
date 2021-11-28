package main.java.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;

import main.java.lib.Message;

public class Client
{
    private DatagramSocket socket;
    private DatagramPacket receivePacket;
    private DatagramPacket sendPacket;
    private String hostName = "localhost";
    private final int SERVER_PORT;

    public Client(int serverPort)
    {
        if(serverPort < 0 || serverPort > 65536)
        {
            throw new InvalidParameterException("Porta deve estar entre 0 e 65536.");
        }
        SERVER_PORT = serverPort;
    }

    // long lastSendRate = Long.MAX_VALUE;
    // for (int i = 0; tranfe; i++)
    //      if (sendRate == lastSendRate)
    //          long sendRate = Math.round(Math.pow(2, i));
    //      else
    //          sendRate += 1;
    //      if (conection timeout)
    //          lastSendRate = sendRate;

    public void createConnection()
    {
        byte[] sendData = new byte[1024];
        byte[] incomingData = new byte[1024];
        try
        {
            socket = new DatagramSocket();
            InetAddress IPAddress = InetAddress.getByName(hostName);
            sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, SERVER_PORT);
            
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        catch (SocketException e)
        {

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        if(args.length != 1)
        {
            printHelp();
            return;
        }

        try
        {
            Client client = new Client(Integer.parseInt(args[0]));
            client.createConnection();
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
