package main.java.server;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private long sequence = 0;

    public Server(int port) throws InvalidParameterException
    {
        if(port < 0 || port > 65536)
        {
            throw new InvalidParameterException("Porta deve estar entre 0 e 65536.");
        }
        this.port = port;
    }

    public void createAndListenSocket()
    {
        byte[] incomingData = new byte[512];
        byte[] sendData = new byte[512];
        try
        {
            socket = new DatagramSocket(port);
            System.out.println("Server is listening on port " + port);
            while(true)
            {
                DatagramPacket receivePacket = new DatagramPacket(incomingData, incomingData.length);
                DatagramPacket sendPackage = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
                socket.receive(receivePacket);

                String message = new String(receivePacket.getData());
                if (!message.equals("SYN"))
                {
                    break;
                }

                sendPackage.setData(buf, offset, length);

                byte[] data = receivePacket.getData();
                file = (FileInfo) convertToObject(data);
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
    }

    private Object convertToObject(byte[] bytes)
    {
        InputStream is = new ByteArrayInputStream(bytes);
		try (ObjectInputStream ois = new ObjectInputStream(is))
        {
			return ois.readObject();
		}
        catch (IOException | ClassNotFoundException ioe) 
        {
			ioe.printStackTrace();	
			return null;
		}
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


