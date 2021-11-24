package main.java.server;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.InvalidParameterException;

import main.java.lib.*;

public class Server 
{
    private final int port;
    private DatagramSocket serverSocket;
    private FileWriter file;
    // private FileInfo file;
    private int sequence = 0;
    private InetAddress clientAddress;
    private int clientPort;

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
            serverSocket = new DatagramSocket(port);
            System.out.println("Server escutando porta " + port);

            while(true)
            {
                DatagramPacket receivePacket = new DatagramPacket(incomingData, incomingData.length);
                clientAddress = receivePacket.getAddress();
                clientPort = receivePacket.getPort();

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());

                serverSocket.receive(receivePacket);
                Message receiveMessage = Message.convertBytesToObject(receivePacket.getData());
                if (receiveMessage.getCommand() == 'S' && receiveMessage.getSequence() == sequence)
                {
                    System.out.println("Recebido pedido de conexao.");
                    sendData = Message.convertObjectToBytes(new Message('A', sequence, "Send Command"));
                    sendPacket.setData(sendData);
                    serverSocket.send(sendPacket);
                }
                else
                {
                    System.out.println("Pedido de conexao invalido.");
                    System.out.println("Resetando conexao para sequencia: " + sequence);
                    sequence = 0;
                    sendData = Message.convertObjectToBytes(new Message('R', sequence, ""));
                    sendPacket.setData(sendData);
                    serverSocket.send(sendPacket);
                    return;
                }

                serverSocket.receive(receivePacket);
                if (receiveMessage.getCommand() == 'U' && receiveMessage.getSequence() == ++sequence)
                {
                    System.out.println("Recebido pedido de upload de arquivo.");
                    sendData = Message.convertObjectToBytes(new Message('U', sequence, "Send File name"));
                    sendPacket.setData(sendData);
                    serverSocket.send(sendPacket);
                }
                else
                {
                    System.out.println("Pedido de conexao invalido.");
                    System.out.println("Resetando conexao.");
                    sequence = 0;
                    sendData = Message.convertObjectToBytes(new Message('R', sequence, ""));
                    sendPacket.setData(sendData);
                    serverSocket.send(sendPacket);
                    return;
                }

                serverSocket.receive(receivePacket);
                if (receiveMessage.getCommand() == 'U' && receiveMessage.getSequence() == ++sequence)
                {
                    System.out.println("Recebido nome de arquivo: " + receiveMessage.getData());
                    file = new FileWriter(receiveMessage.getData());

                    sendData = Message.convertObjectToBytes(new Message('U', sequence, "Send File lenght"));
                    sendPacket.setData(sendData);
                    serverSocket.send(sendPacket);
                }

                serverSocket.receive(receivePacket);
                if (receiveMessage.getCommand() == 'U' && receiveMessage.getSequence() == ++sequence)
                {
                    System.out.println("Recebido tamanho do arquivo: " + receiveMessage.getData());
                    int fileSize = Integer.parseInt(receiveMessage.getData());
                    int packetsCount = (int) Math.ceil(fileSize / 400.0);

                    sendData = Message.convertObjectToBytes(new Message('U', sequence, "Send File data"));
                    sendPacket.setData(sendData);
                    serverSocket.send(sendPacket);
                }
                {
                    System.out.println("Recebido pedido de arquivo.");
                    file = new FileInfo(receiveMessage.getFileName(), receiveMessage.getFileSize());
                    sendMessage(new Message('s', 0, ""), sendPacket);
                }
                else if (receiveMessage.getCommand().equals('f'))
                {
                    System.out.println("Recebido pedido de fragmento.");
                    if (file == null)
                    {
                        System.out.println("Arquivo não foi recebido.");
                        sendMessage(new Message('f', 0, ""), sendPacket);
                    }
                    else
                    {
                        file.addFragment(receiveMessage.getSequence(), receiveMessage.getData());
                        if (file.isComplete())
                        {
                            System.out.println("Arquivo completo.");
                            sendMessage(new Message('f', 0, ""), sendPacket);
                        }
                        else
                        {
                            System.out.println("Arquivo incompleto.");
                            sendMessage(new Message('f', 0, ""), sendPacket);
                        }
                    }
                }
                else if (receiveMessage.getCommand().equals('c'))
                {
                    System.out.println("Recebido pedido de confirmação.");
                    if (file == null)
                    {
                        System.out.println("Arquivo não foi recebido.");
                        sendMessage(new Message('c', 0, ""), sendPacket);
                    }
                    else
                    {
                        if (file.isComplete())
                        {
                            System.out.println("Arquivo completo.");
                            sendMessage(new Message('c', 0, ""), sendPacket);
                        }
                        else
                        {
                            System.out.println("Arquivo incompleto.");
                            sendMessage(new Message('c', 0, ""), sendPacket);
                        }
                    }
                }
                else
                {
                    System.out.println("Comando inválido.");
                {
                    break;
                }


                // {
                //     file = new FileInfo(receiveMessage.getFileName(), receiveMessage.getFileSize());
                //     System.out.println("Recebido arquivo: " + file.getFileName() + " (" + file.getFileSize() + " bytes)");
                //     sequence = 0;
                // }
                // else if (receiveMessage.getCommand() == 'b')
                // {
                //     if (file == null)
                //     {
                //         System.out.println("Arquivo não foi recebido.");
                //         break;
                //     }
                //     if (receiveMessage.getSequence() != sequence)
                //     {
                //         System.out.println("Sequencia inválida.");
                //         break;
                //     }
                //     file.addChunk(receiveMessage.getData());
                //     sequence++;
                //     if (sequence == file.getFileSize())
                //     {
                //         System.out.println("Arquivo recebido com sucesso.");
                //         break;
                //     }
                // }
                // else
                // {
                //     System.out.println("Comando inválido.");
                //     break;
                // }


                // sendPackage.setData(buf, offset, length);

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


