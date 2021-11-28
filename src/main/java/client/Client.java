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

import main.java.lib.*;
import main.java.lib.Message.CommandType;

public class Client 
{
    private final int SERVER_PORT;
    private InetAddress serverIpAddress;
    private String fileName;
    private int sequence = 0;
    private DatagramSocket socket;
    private DatagramPacket receivePacket;
    private DatagramPacket sendPacket;
    private Message receiveMessage;
    private Message sendMessage;
    private byte[] receiveData = new byte[1024];
    private byte[] sendData = new byte[1024];

    // private InetAddress clientAddress;
    // private int clientPort;

    public Client(String[] args) throws UnknownHostException
    {
        this.serverIpAddress = InetAddress.getByName(args[0]);
        this.SERVER_PORT = Integer.parseInt(args[1]);
        if(this.SERVER_PORT < 0 || this.SERVER_PORT > 65536)
        {
            throw new InvalidParameterException("Porta deve estar entre 0 e 65536.");
        }
        this.fileName = args[2];
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
        try
        {
            socket = new DatagramSocket();
            sendPacket = new DatagramPacket(sendData, sendData.length, serverIpAddress, SERVER_PORT);
            sendMessage = new Message(CommandType.SYN, sequence);
            sendData = ObjectConverter.convertObjectToBytes(sendMessage);
            sendPacket.setData(sendData);
            socket.send(sendPacket);
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            receiveData = receivePacket.getData();
            receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);
            if(receiveMessage.getCommand() == CommandType.SYN_ACK && receiveMessage.getSequence() == sequence)
            {
                System.out.println("Conexão do servidor recebida com sucesso.");
                sequence = receiveMessage.getSequence();
                sendMessage = new Message(CommandType.SYNACK, sequence);
                sendData = ObjectConverter.convertObjectToBytes(sendMessage);
                sendPacket.setData(sendData);
                socket.send(sendPacket);


                sequence++;
            }
            else
            {
                System.out.println("Conexão não estabelecida.");
                System.out.println("Conexão estabelecida com sucesso.");
                System.out.println("Erro na conexão.");
            }



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
        if(args.length != 3)
        {
            printHelp();
            return;
        }

        try
        {
            Client client = new Client(args);
            client.createConnection();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void printHelp()
    {
        System.out.println("Usage: Client <server IP address> <port> <filename>");
    }
}
