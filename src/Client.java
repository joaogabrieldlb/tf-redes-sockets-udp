

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class Client 
{
    private static final int FILE_BUFFER_SIZE = 512;
    private static final int SOCKET_TIMEOUT = 500;

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
                System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Conexao nao estabelecida.");
                return false;
            }
            System.out.println("S#" + sequence + " " + receiveMessage.getCommand().name() + "> Conexao do servidor recebida com sucesso.");

            // envia mensagem ACK para o server
            sendMessage = new Message(CommandType.ACK, sequence);
            sendData = ObjectConverter.convertObjectToBytes(sendMessage);
            sendPacket.setData(sendData);
            socket.send(sendPacket);

            System.out.println("LOG> Conexao estabelecida com sucesso.");
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
            System.out.println("LOG_ERRO> Arquivo nao encontrado.");
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
            System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Conexao perdida no envio de FileInfo.");
            connectionReset();
            return false;
        }
        System.out.println("S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Recebida confirmacao do FileInfo.");

        //envia dados do arquivo
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file)))
        {
            byte[] buffer = new byte[FILE_BUFFER_SIZE];

            int i = 1;
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

                // recebe mensagem de ACK
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                receiveData = receivePacket.getData();
                receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

                if(!(receiveMessage.getCommand() == CommandType.ACK && receiveMessage.getSequence() == sequence))
                {
                    System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Conexao perdida no envio de DATA.");
                    connectionReset();
                    return false;
                }
                System.out.println("S#" + receiveMessage.getSequence()  + " " + receiveMessage.getCommand().name() + "> Confirmacao de recebimento do pacote #" + i + " de " + fileInfo.getTotalPackets() + " pelo servidor.");
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
            System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Conexao perdida no envio do arquivo.");
            // connectionReset();
            return false;
        }

        System.out.println("S#" + receiveMessage.getSequence() + " " + result.name() + "> Recebido status do envio do arquivo: " + result.name());

        // envia mensagem ACK para o server
        sendMessage = new Message(CommandType.ACK, sequence);
        sendData = ObjectConverter.convertObjectToBytes(sendMessage);
        sendPacket.setData(sendData);
        socket.send(sendPacket);
        
        return true;
    }
    
    private boolean sendFileSlowStart() throws IOException
    {
        if (!Files.isReadable(Paths.get(this.fileName)))
        {
            System.out.println("LOG_ERRO> Arquivo nao encontrado.");
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
            System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Conexao perdida no envio de FileInfo.");
            connectionReset();
            return false;
        }
        System.out.println("S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Recebida confirmacao do FileInfo.");

        //envia dados do arquivo
        // socket.setSoTimeout(SOCKET_TIMEOUT);
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file)))
        {
            byte[][] cachedBuffers;

            long packetNumber = 1;
            int bytesRead;
            long remainingPackets = fileInfo.getTotalPackets();

            while (remainingPackets > 0)
            {
                for (int parallelSend = 1; parallelSend <= remainingPackets || parallelSend <= Integer.MAX_VALUE; )
                {
                    cachedBuffers = new byte[parallelSend][];
                    int reads = 0;
                    byte[] buffer = new byte[FILE_BUFFER_SIZE];

                    // coloca em cache os buffers necessarios para envio dos pacotes
                    while ((bytesRead = inputStream.read(buffer)) != -1 && reads < parallelSend) {
                        // tratamento do buffer para o último pacote
                        if (bytesRead < FILE_BUFFER_SIZE)
                        {
                            buffer = Arrays.copyOf(buffer, bytesRead);
                        }
                        System.out.println("parallelsend: " + parallelSend);
                        System.out.println("reads: " + reads);
                        System.out.println("cachedBuffers: " + cachedBuffers.length);
                        cachedBuffers[reads] = buffer;
                        reads++;
                    }
                
                    long initialSequence = sequence;

                    for (int sends = 0; sends < reads; sends++)
                    {
                        // envia pacote DATA
                        sendMessage = new Message(CommandType.DATA, ++sequence, cachedBuffers[sends]);
                        sendData = ObjectConverter.convertObjectToBytes(sendMessage);
                        sendPacket.setData(sendData);
                        socket.send(sendPacket);
                    }

                    int lastConfirmedSend = 0;

                    for (int confirmedSends = 0; confirmedSends < reads; confirmedSends++)
                    {
                        // recebe mensagem de ACK
                        receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        socket.receive(receivePacket);
                        receiveData = receivePacket.getData();
                        receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

                        if(!(receiveMessage.getCommand() == CommandType.ACK && receiveMessage.getSequence() == sequence))
                        {
                            System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Conexao perdida no envio de DATA.");
                            parallelSend = 1;
                            connectionReset();
                            break;
                        }
                        System.out.println("S#" + receiveMessage.getSequence()  + " " + receiveMessage.getCommand().name() + "> Confirmacao de recebimento do pacote #" + confirmedSends + " de " + fileInfo.getTotalPackets() + " pelo servidor.");
                        parallelSend *= 2;
                        lastConfirmedSend++;
                        remainingPackets--;
                    }
                }
            }

            
        } catch (Exception e) {
            System.out.println("excecao?");
        }

        socket.setSoTimeout(0);

        // aguarda status do envio do arquivo
        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        receiveData = receivePacket.getData();
        receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);
        CommandType result = receiveMessage.getCommand();

        if(!((result == CommandType.SUCCESS || result == CommandType.FAILURE ) && receiveMessage.getSequence() == ++sequence))
        {
            System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Conexao perdida no envio do arquivo.");
            // connectionReset();
            return false;
        }

        System.out.println("S#" + receiveMessage.getSequence() + " " + result.name() + "> Recebido status do envio do arquivo: " + result.name());

        // envia mensagem ACK para o server
        sendMessage = new Message(CommandType.ACK, sequence);
        sendData = ObjectConverter.convertObjectToBytes(sendMessage);
        sendPacket.setData(sendData);
        socket.send(sendPacket);
        
        return true;
    }

    private void connectionReset() throws IOException
    {
        System.out.println("LOG> Resetando conexao sequencia: " + sequence);
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
                System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Falha no encerramento da conexao.");
                return false;
            }
            System.out.println("S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Confirmacao do recebimento do pedido de encerramento pelo <client>.");

            // recebe mensagem de FIN
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            receiveData = receivePacket.getData();
            receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

            if(!(receiveMessage.getCommand() == CommandType.FIN && receiveMessage.getSequence() == ++sequence))
            {
                System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Falha no encerramento da conexao.");
                return false;
            }
            System.out.println("S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Pedido de encerramento da conexao recebido do <server>.");

            // envia mensagem ACK para o server
            sendMessage = new Message(CommandType.ACK, sequence);
            sendData = ObjectConverter.convertObjectToBytes(sendMessage);
            sendPacket.setData(sendData);
            socket.send(sendPacket);

            System.out.println("LOG> Conexao encerrada com sucesso.");
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
        System.out.println("Uso do programa: Client <server IP address> <port> <filename>");
    }

    public static void main(String[] args)
    {
        args = new String[]{ "localhost", "1234", "teste.png" };
        if(args.length != 3)
        {
            printHelp();
            return;
        }

        try
        {
            Client client = new Client(args);
            client.estabilishConnection();
            // client.sendFile();
            client.sendFileSlowStart();
            client.finallizeConnection();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}

