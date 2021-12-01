import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

public class Client 
{
    private static final int SLOW_START_LIMIT = 128;
    private static final int FILE_BUFFER_SIZE = 512;

    private final int serverPort;
    private InetAddress serverIpAddress;
    private String fileName;
    private long sequence = 0;
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
        this.serverPort = Integer.parseInt(args[1]);
        if(this.serverPort < 0 || this.serverPort > 65536)
        {
            throw new InvalidParameterException("Porta deve estar entre 0 e 65536.");
        }
        
        this.fileName = args[2];
        if (!Files.isReadable(Paths.get(fileName)))
        {
            throw new InvalidParameterException("Arquivo nao encontrado.");
        }
    }

    public boolean estabilishConnection()
    {
        try
        {
            socket = new DatagramSocket();
            int errorCount = 0;

            // permite 3 erros de conexao antes do RST
            while(errorCount < 3)
            {
                // envia o comando de conexao SYN
                sendPacket = new DatagramPacket(sendData, sendData.length, serverIpAddress, serverPort);
                sendMessage = new Message(CommandType.SYN, sequence);
                sendData = ObjectConverter.convertObjectToBytes(sendMessage);
                sendPacket.setData(sendData);
                socket.send(sendPacket);

                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                receiveData = receivePacket.getData();
                receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

                // recebe um SYN_ACK
                if(!(receiveMessage.getCommand() == CommandType.SYN_ACK && receiveMessage.getSequence() == sequence))
                {
                    System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Conexao nao estabelecida.");
                    // conexao nao estabelecida
                    errorCount++;
                    continue;
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
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }
    
    private void sendConnectionReset(long sequence) throws IOException
    {
        System.out.println("LOG> Resetando conexao sequencia: " + sequence);
        this.sequence = sequence;
        // envia mensagem de RST
        sendMessage = new Message(CommandType.RST, sequence);
        sendData = ObjectConverter.convertObjectToBytes(sendMessage);
        sendPacket.setData(sendData);
        socket.send(sendPacket);
    }
    
    private void receviceConnectionReset(long sequence) throws IOException
    {
        System.out.println("LOG> Resetando conexao sequencia: " + sequence);
        this.sequence = sequence;
        // envia mensagem de RST
        sendMessage = new Message(CommandType.ACK, sequence);
        sendData = ObjectConverter.convertObjectToBytes(sendMessage);
        sendPacket.setData(sendData);
        socket.send(sendPacket);
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
            return false;
        }
        System.out.println("S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Recebida confirmacao do FileInfo.");

        //envia dados do arquivo
        // if (!sendFileNormal(file, fileInfo)) return false;
        if (!sendFileSlowStart(file, fileInfo)) return false;

        // aguarda status do envio do arquivo
        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        receiveData = receivePacket.getData();
        receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);
        CommandType result = receiveMessage.getCommand();

        if(!((result == CommandType.SUCCESS || result == CommandType.FAILURE ) && receiveMessage.getSequence() == ++sequence))
        {
            System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Conexao perdida no envio do arquivo.");
            // connectionReset(sequence);
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
    
    
    private boolean sendFileNormal(File file, FileInfo fileInfo) throws FileNotFoundException, IOException {
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
                    // connectionReset(0);
                    return false;
                }
                System.out.println("S#" + receiveMessage.getSequence()  + " " + receiveMessage.getCommand().name() + "> Confirmacao de recebimento do pacote #" + i + " de " + fileInfo.getTotalPackets() + " pelo servidor.");
                i++;
            }
        }
        return true;
    }

    private boolean sendFileSlowStart(File file, FileInfo fileInfo) throws IOException
    {
        // envia dados do arquivo
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file)))
        {
            List<byte[]> cachedBuffers = new ArrayList<>();

            long packetNumber = 0;
            int bytesRead;
            long remainingPackets = fileInfo.getTotalPackets();

            while (remainingPackets > 0)
            {
                // implementa o Slow Start
                for (int parallelSend = 1; remainingPackets > 0 && parallelSend <= SLOW_START_LIMIT; )
                {
                    // permite que no ultimo envio sejam enviados todos os pacotes remanescentes sem resetar o Slow Start
                    if(parallelSend > remainingPackets)
                    {
                        parallelSend = (int) remainingPackets;
                    }
                
                    int availableReads = cachedBuffers.size();
                    byte[] buffer = new byte[FILE_BUFFER_SIZE];
                    
                    // coloca em cache os buffers necessarios para envio dos pacotes
                    while (availableReads < parallelSend && (bytesRead = inputStream.read(buffer)) != -1) {
                        // tratamento do buffer para o último pacote
                        if (bytesRead < FILE_BUFFER_SIZE)
                        {
                            buffer = Arrays.copyOf(buffer, bytesRead);
                        }
                        cachedBuffers.add(buffer.clone());
                        availableReads++;
                    }
                
                    long initialSequence = sequence;

                    for (int sends = 0; sends < availableReads; sends++)
                    {
                        // envia pacote DATA
                        sendMessage = new Message(CommandType.DATA, ++sequence, cachedBuffers.get(sends));
                        sendData = ObjectConverter.convertObjectToBytes(sendMessage);
                        sendPacket.setData(sendData);
                        socket.send(sendPacket);
                    }

                    boolean allReceived = true;

                    for (int confirmedSends = 0; confirmedSends < availableReads; confirmedSends++)
                    {
                        // recebe mensagem de ACK
                        receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        socket.receive(receivePacket);
                        receiveData = receivePacket.getData();
                        receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

                        if(!(receiveMessage.getCommand() == CommandType.ACK))
                        {
                            System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Conexao perdida no envio de DATA.");
                            allReceived = false;
                            
                            if(receiveMessage.getCommand() == CommandType.RST)
                            {
                                receviceConnectionReset(receiveMessage.getSequence());
                                sequence--;
                            }
                            break;
                        }
                        else
                        {
                            if (!(receiveMessage.getSequence() == ++initialSequence))
                            {
                                allReceived = false;
                                sequence = initialSequence;
                                initialSequence--;
                                break;
                            }
                        }
            
                        System.out.println("S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Confirmacao de recebimento do pacote #" + ++packetNumber + " de " + fileInfo.getTotalPackets() + " pelo servidor.");
                        cachedBuffers.remove(0);
                        remainingPackets--;
                    }

                    // mantem a cadencia de transmissao no limite definido se nenhum erro tiver ocorrido
                    if (allReceived)
                    {
                        if (parallelSend < SLOW_START_LIMIT) parallelSend = parallelSend << 1;
                    }
                    else
                    {
                        parallelSend = 1;
                    }
                }
            }
        }
        return true;
    }

    private boolean finallizeConnection()
    {
        try {
            // envia mensagem FIN
            sendPacket = new DatagramPacket(sendData, sendData.length, serverIpAddress, serverPort);
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
        if(args.length != 3)
        {
            printHelp();
            return;
        }

        try
        {
            Client client = new Client(args);
            boolean status = client.estabilishConnection();
            if (status)
            {
                client.sendFile();
                client.finallizeConnection();
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
