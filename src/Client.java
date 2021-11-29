

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.Arrays;

public class Client 
{
    private static final int FILE_BUFFER_SIZE = 512;

    private final int SERVER_PORT;
    private InetAddress serverIpAddress;
    private String fileName;
    private int sequence = 0;
    private DatagramSocket socket;
    private DatagramPacket receivePacket;
    private DatagramPacket sendPacket;
    private byte[] receiveData = new byte[1024];
    private byte[] sendData = new byte[1024];
    private Message receiveMessage;
    private Message sendMessage;

    public Client(String[] args) throws UnknownHostException
    {
        this.serverIpAddress = InetAddress.getByName(args[0]);
        this.SERVER_PORT = Integer.parseInt(args[1]);
        if(this.SERVER_PORT < 0 || this.SERVER_PORT > 65536)
        {
            throw new InvalidParameterException("Porta deve estar entre 0 e 65536.");
        }
        
        this.fileName = args[2];
        if (!Files.isReadable(Paths.get(fileName)))
        {
            throw new InvalidParameterException("Arquivo nao encontrado.");
        }
    }

    // long lastSendRate = Long.MAX_VALUE;
    // for (int i = 0; tranfe; i++)
    //      if (sendRate == lastSendRate)
    //          long sendRate = Math.round(Math.pow(2, i));
    //      else
    //          sendRate += 1;
    //      if (conection timeout)
    //          lastSendRate = sendRate;

    public boolean estabilishConnection()
    {
        try
        {
            socket = new DatagramSocket();

            // envia o comando de conexao SYN
            sendPacket = new DatagramPacket(sendData, sendData.length, serverIpAddress, SERVER_PORT);
            sendMessage = new Message(CommandType.SYN, sequence);
            sendData = ObjectConverter.convertObjectToBytes(sendMessage);
            sendPacket.setData(sendData);
            socket.send(sendPacket);

            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            receiveData = receivePacket.getData();
            receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

            if(!(receiveMessage.getCommand() == CommandType.SYN_ACK && receiveMessage.getSequence() == sequence))
            {
                System.out.println("Conexao nao estabelecida.");
                return false;
            }
            System.out.println("Conexao do servidor recebida com sucesso.");

            // envia mensagem ACK para o server
            sendMessage = new Message(CommandType.ACK, sequence);
            sendData = ObjectConverter.convertObjectToBytes(sendMessage);
            sendPacket.setData(sendData);
            socket.send(sendPacket);

            System.out.println("Conexao estabelecida com sucesso.");
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }
    
    private boolean sendFile() throws IOException
    {
        if (!Files.isReadable(Paths.get(this.fileName)))
        {
            System.out.println("Arquivo nao encontrado.");
            return false;
        }
        final File file = Paths.get(this.fileName).toFile();

        // envia informacoes do arquivo (FileInfo)
        FileInfo fileInfo = new FileInfo(file, FILE_BUFFER_SIZE);
        byte[] sendFileInfo = ObjectConverter.convertObjectToBytes(fileInfo);
        sendMessage = new Message(CommandType.UPLOAD, ++sequence, sendFileInfo);
        sendData = ObjectConverter.convertObjectToBytes(sendMessage);
        sendPacket.setData(sendData);
        socket.send(sendPacket);

        // recebe mensagem de ACK
        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        receiveData = receivePacket.getData();
        receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

        if(!(receiveMessage.getCommand() == CommandType.ACK && receiveMessage.getSequence() == sequence))
        {
            System.out.println("Conexao perdida no envio de FileInfo.");
            connectionReset();
            return false;
        }

        //envia dados do arquivo
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file)))
        {
            byte[] buffer = new byte[FILE_BUFFER_SIZE];

            int i = 0;
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // tratamento do buffer para o último pacote
                if (bytesRead < FILE_BUFFER_SIZE)
                {
                    buffer = Arrays.copyOf(buffer, bytesRead);
                }

                // envia pacote DATA
                sendMessage = new Message(CommandType.DATA, ++sequence, buffer);
                sendData = ObjectConverter.convertObjectToBytes(sendMessage);
                sendPacket.setData(sendData);
                socket.send(sendPacket);
                System.out.println("Realizado envio do pacote " + i + "/" + fileInfo.getTotalPackets() + ".");

                // recebe mensagem de ACK
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                receiveData = receivePacket.getData();
                receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

                if(!(receiveMessage.getCommand() == CommandType.ACK && receiveMessage.getSequence() == sequence))
                {
                    System.out.println("Conexao perdida no envio de DATA.");
                    connectionReset();
                    return false;
                }
                System.out.println("Confirmaçao de recebimento do pacote " + i + "/" + fileInfo.getTotalPackets() + " pelo server.");
                i++;
            }
        }

        // aguarda status do envio do arquivo
        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        receiveData = receivePacket.getData();
        receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);
        CommandType result = receiveMessage.getCommand();

        if(!((result == CommandType.SUCCESS || result == CommandType.FAILURE ) && receiveMessage.getSequence() == ++sequence))
        {
            System.out.println("Conexao perdida no envio do arquivo.");
            // connectionReset();
            return false;
        }

        System.out.println("Envio do arquivo concluído com status: " + result.name());

        // envia mensagem ACK para o server
        sendMessage = new Message(CommandType.ACK, sequence);
        sendData = ObjectConverter.convertObjectToBytes(sendMessage);
        sendPacket.setData(sendData);
        socket.send(sendPacket);
        
        return true;
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

    private boolean finallizeConnection()
    {
        try {
            // envia mensagem FIN
            sendPacket = new DatagramPacket(sendData, sendData.length, serverIpAddress, SERVER_PORT);
            sendMessage = new Message(CommandType.FIN, ++sequence);
            sendData = ObjectConverter.convertObjectToBytes(sendMessage);
            sendPacket.setData(sendData);
            socket.send(sendPacket);

            // recebe mensagem de ACK
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            receiveData = receivePacket.getData();
            receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

            if(!(receiveMessage.getCommand() == CommandType.ACK && receiveMessage.getSequence() == sequence))
            {
                System.out.println("Falha no encerramento da conexao.");
                return false;
            }
            System.out.println("Pedido de encerramento para o servidor recebida pelo servidor.");

            // recebe mensagem de FIN
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            receiveData = receivePacket.getData();
            receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

            if(!(receiveMessage.getCommand() == CommandType.FIN && receiveMessage.getSequence() == ++sequence))
            {
                System.out.println("Falha no encerramento da conexao.");
                return false;
            }
            System.out.println("Pedido de encerramento do servidor recebida com sucesso.");

            // envia mensagem ACK para o server
            sendMessage = new Message(CommandType.ACK, sequence);
            sendData = ObjectConverter.convertObjectToBytes(sendMessage);
            sendPacket.setData(sendData);
            socket.send(sendPacket);

            System.out.println("Conexao encerrada com sucesso.");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }

        return false;
    }

    
    private static void printHelp()
    {
        System.out.println("Usage: Client <server IP address> <port> <filename>");
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
            client.estabilishConnection();
            client.sendFile();
            client.finallizeConnection();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}

