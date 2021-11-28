package main.java.server;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.InvalidParameterException;

import main.java.lib.*;
import main.java.lib.Message.CommandType;

public class Server 
{
    private final int PORT;
    private static final int FILE_BUFFER_SIZE = 512;
    private int sequence = 0;
    private DatagramSocket socket;
    private DatagramPacket receivePacket;
    private DatagramPacket sendPacket;
    private Message receiveMessage;
    private Message sendMessage;
    private byte[] receiveData = new byte[1024];
    private byte[] sendData = new byte[1024];

    private InetAddress clientAddress;
    private int clientPort;

    public Server(int port) throws InvalidParameterException
    {
        if(port < 0 || port > 65536)
        {
            throw new InvalidParameterException("Porta deve estar entre 0 e 65536.");
        }
        this.PORT = port;
    }

    public void createAndListenSocket()
    {
        try
        {
            // implementa socket UDP com a porta
            socket = new DatagramSocket(PORT);
            System.out.println("Server escutando na porta " + PORT);

            while(true)
            {
                // instancia pacote UDP de recepcao
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                // inicializado null pois aguarda recepcao de pacote UDP
                sendPacket = null;
                
                // estabelece a conexao
                // retorna true se a conexao foi corretamente estabelecida
                if (!establishedConnection())
                {
                    continue;
                }
                System.out.println("Conexao estabelecida com sucesso!");

                while(true)
                {
                    // aguardo recepcao de pacote UDP
                    socket.receive(receivePacket);

                    // converte byte array recebido no objeto mensagem
                    receiveMessage = (Message) ObjectConverter.convertBytesToObject(receivePacket.getData());
                    switch(receiveMessage.getCommand())
                    {
                        case UPLOAD:
                            if (receiveMessage.getSequence() == ++sequence)
                            {
                                transferFile();
                            }
                            else
                            {

                            }
                            break;
                        case FIN:
                            
                        default:
                            continue;
                    }
                    break;
                }

                // finaliza conexao
                
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

    private void receiveFile()
    {
        Object data = receiveMessage.getData();
        FileInfo fileInfo;
        
        if (!(data instanceof FileInfo))
        {
            System.out.println("Erro: Não foi recebido informacoes validas sobre o arquivo!");
            return;
        }
        
        fileInfo = (FileInfo) ObjectConverter.convertBytesToObject(receiveMessage.getData());
        int amountFragments = (int) Math.ceil(fileInfo.fileSize / FILE_BUFFER_SIZE);
        String fileName = fileInfo.fileName;
        
    }

    private void writeFileFragment(byte[] data, String fileName)
    {
        try (OutputStream os = new FileOutputStream(fileName))
        {
            byte[] buffer = new byte[FILE_BUFFER_SIZE];
            buffer = data;
            os.write(buffer);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private boolean establishedConnection() throws IOException
    {
        // aguarda o recebimento de uma mensagem
        socket.receive(receivePacket);
        
        // coleta dados do cliente que enviou a mensagem
        clientAddress = receivePacket.getAddress();
        clientPort = receivePacket.getPort();
        
        sendPacket = new DatagramPacket(sendData, sendData.length,
            clientAddress, clientPort);

        // converte byte array recebido no objeto mensagem
        receiveMessage = (Message) ObjectConverter.convertBytesToObject(receivePacket.getData());
        
        // valida se a menssagem recebida é de SYN
        // ou se o numero de sequencia bate com o esperado
        if (receiveMessage.getCommand() == CommandType.SYN
                && receiveMessage.getSequence() == sequence)
        {
            System.out.println(clientAddress + ":" + clientPort + " - " + "SYN recebido");
            // prepara e envia mensagem de ACK
            sendData = ObjectConverter.convertObjectToBytes(new Message(CommandType.SYN_ACK, sequence));
            sendPacket.setData(sendData);
            socket.send(sendPacket);
        }
        else
        {
            System.out.println("Pedido de conexao invalido.");
            connectionReset();
            // conexao nao estabelecida
            return false;
        }

        // aguardando o recebimento do reconhecimento pelo cliente
        socket.receive(receivePacket);

        // valida se a menssagem recebida nao é de ACK ou se...(?)
        if (receiveMessage.getCommand() == CommandType.ACK 
                || receiveMessage.getSequence() == sequence)
        {
            // conexao estabelecida
            return true;
        }
        else
        {
            System.out.println("Pedido de conexao invalido.");
            connectionReset();
            // conexao nao estabelecida
            return false;
        }
    }

    private void connectionReset() throws IOException
    {
        System.out.println("Resetando conexao sequencia: " + sequence);
        sequence = 0;
        // envia mensagem de RST
        sendMessage = new Message(CommandType.RST, sequence);
        sendData = ObjectConverter.convertObjectToBytes(sendMessage);
        sendPacket.setData(sendData);
        socket.send(sendPacket);
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


