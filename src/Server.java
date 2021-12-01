import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;

public class Server 
{
    private static final String DEFAULT_FOLDER = "./Files/";
    private static final int SOCKET_TIMEOUT = 500;
    private static int fileBufferSize = 512;  

    private final int port;
    private long sequence = 0;
    private FileInfo fileInfo;
    private DatagramSocket socket;
    private DatagramPacket receivePacket;
    private DatagramPacket sendPacket;
    private byte[] receiveData = new byte[1024];
    private byte[] sendData = new byte[1024];
    private Message receiveMessage;
    private Message sendMessage;

    private InetAddress clientAddress;
    private int clientPort;

    public Server(int port) throws InvalidParameterException, IOException
    {
        this.port = port;
        if(port < 0 || port > 65536)
        {
            throw new InvalidParameterException("Porta deve estar entre 0 e 65536.");
        }

        if (!Files.isDirectory(Paths.get(DEFAULT_FOLDER)))
        {
            Files.createDirectory(Paths.get(DEFAULT_FOLDER));
        }
    }

    private boolean establishConnection() throws IOException
    {
        // implementa socket UDP com a porta
        socket = new DatagramSocket(port);
        System.out.println("Server escutando na porta " + port);
        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        int errorCount = 0;

        // permite 3 erros de conexao antes do RST
        while(errorCount < 3)
        {
            // aguarda o recebimento de uma mensagem
            socket.receive(receivePacket);
            
            // coleta dados do cliente que enviou a mensagem
            clientAddress = receivePacket.getAddress();
            clientPort = receivePacket.getPort();

            // converte byte array recebido no objeto mensagem
            receiveMessage = (Message) ObjectConverter.convertBytesToObject(receivePacket.getData());
            
            // valida se a menssagem recebida nao e ACK ou se nao e o numero de sequencia esperado
            if (!(receiveMessage.getCommand() == CommandType.SYN && receiveMessage.getSequence() == sequence))
            {
                System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Pedido de conexao invalido.");
                sendConnectionReset(sequence);
                // conexao nao estabelecida
                errorCount++;
                continue;
            }

            System.out.println("S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> " + clientAddress + ":" + clientPort);

            // envia mensagem de SYN_ACK
            sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            sendData = ObjectConverter.convertObjectToBytes(new Message(CommandType.SYN_ACK, sequence));
            sendPacket.setData(sendData);
            socket.send(sendPacket);

            // aguardando o recebimento do reconhecimento pelo cliente
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            receiveData = receivePacket.getData();
            receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

            // valida se a menssagem recebida nao e ACK ou se nao e o numero de sequencia esperado
            if (!(receiveMessage.getCommand() == CommandType.ACK 
                    && receiveMessage.getSequence() == sequence))
            {
                System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Confirmacao de conexao invalido.");
                sendConnectionReset(sequence);
                // conexao nao estabelecida
                errorCount++;
                continue;
            }

            System.out.println("S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> " + clientAddress + ":" + clientPort);
            
            // conexao estabelecida
            System.out.println("LOG> Conexao estabelecida com sucesso.");
            return true;
        }
        return false;
    }
    
    private void sendConnectionReset(long sequence) throws SocketException, IOException
    {
        while (true)
        {
            System.out.println("LOG> Resetando conexao para sequencia: " + sequence);
            this.sequence = sequence;
            // envia mensagem de RST
            sendMessage = new Message(CommandType.RST, sequence);
            sendData = ObjectConverter.convertObjectToBytes(sendMessage);
            sendPacket.setData(sendData);
            socket.send(sendPacket);

            socket.setSoTimeout(SOCKET_TIMEOUT);

            while (true)
            {
                // aguardando o recebimento do ACK
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                try {
                    socket.receive(receivePacket);
                } catch (SocketTimeoutException e) {
                    System.out.println("ERRO> Timeout para recebimento da confirmacao.");
                    continue;
                }
                receiveData = receivePacket.getData();
                receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

                // valida se a menssagem recebida nao e ACK ou se nao e o numero de sequencia esperado
                if (!(receiveMessage.getCommand() == CommandType.ACK && receiveMessage.getSequence() == sequence))
                {
                    System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Confirmacao de reset invalido.");
                    continue;
                }
                System.out.println("S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Confirmacao de reset recebido.");

                socket.setSoTimeout(0);
                return;
            }
        }
    }

    private boolean receiveFile() throws IOException, NoSuchAlgorithmException
    {
        socket.receive(receivePacket);
        receiveData = receivePacket.getData();
        receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);
        
        if(!(receiveMessage.getCommand() == CommandType.UPLOAD && receiveMessage.getSequence() == ++sequence))
        {
            System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Falha no recebimento do FileInfo.");
            return false;
        }
        
        System.out.println("S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> FileInfo recebido com sucesso.");
        fileInfo = (FileInfo) ObjectConverter.convertBytesToObject(receiveMessage.getData());
        fileBufferSize = fileInfo.getFileBufferSize();
        fileInfo.setFileName(DEFAULT_FOLDER + fileInfo.getFileName());

        // envia mensagem ACK para o client
        sendMessage = new Message(CommandType.ACK, sequence);
        sendData = ObjectConverter.convertObjectToBytes(sendMessage);
        sendPacket.setData(sendData);
        socket.send(sendPacket);

        // chama metodo de recebimento
        // if (!writeFileNormal()) return false;
        if (!writeFileSlowStart()) return false;

        File file = new File(fileInfo.getFileName());
        boolean result = FileInfo.computeMD5(file).equals(fileInfo.getFileHash());

        if (result)
        {
            // envia mensagem SUCCESS para o client
            sendMessage = new Message(CommandType.SUCCESS, ++sequence);
            sendData = ObjectConverter.convertObjectToBytes(sendMessage);
            sendPacket.setData(sendData);
            socket.send(sendPacket);

            System.out.println("LOG> Arquivo recebido com sucesso.");
        }
        else
        {
            // envia mensagem FAILURE para o client
            sendMessage = new Message(CommandType.FAILURE, ++sequence);
            sendData = ObjectConverter.convertObjectToBytes(sendMessage);
            sendPacket.setData(sendData);
            socket.send(sendPacket);

            System.out.println("LOG> Falha no recebimento do arquivo.");
        }
        
        // recebe mensagem ACK do client
        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        receiveData = receivePacket.getData();
        receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

        if(!(receiveMessage.getCommand() == CommandType.ACK && receiveMessage.getSequence() == sequence))
        {
            System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Falha na confirmaçao do status do arquivo.");
            // connectionReset(sequence);
            return false;
        }
        System.out.println("S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Confirmacao de recebimento do status do envio.");

        return result;
    }
    
    private boolean writeFileNormal() throws FileNotFoundException, IOException
    {
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileInfo.getFileName()))) {
            for(int i = 1; i <= fileInfo.getTotalPackets(); i++)
            {
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                receiveData = receivePacket.getData();
                receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

                if(!(receiveMessage.getCommand() == CommandType.DATA & receiveMessage.getSequence() == ++sequence))
                {
                    System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Conexao perdida no recebimento dos dados do arquivo.");
                    sendConnectionReset(sequence);
                    sequence--;
                    i--;
                    continue;
                }

                if (receiveMessage.getSequence() < sequence)
                {
                    System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Reenvio do ACK S#" + receiveMessage.getSequence());

                    // reenvia mensagem ACK para o client
                    sendMessage = new Message(CommandType.ACK, receiveMessage.getSequence());
                    sendData = ObjectConverter.convertObjectToBytes(sendMessage);
                    sendPacket.setData(sendData);
                    socket.send(sendPacket);
                    continue;
                }

                if (receiveMessage.getSequence() > sequence)
                {
                    System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Resetando conexao para S#" + sequence);
                    sendConnectionReset(sequence);
                    sequence--;
                    i--;
                    continue;
                }


                System.out.println("S#" + receiveMessage.getSequence()  + " " + receiveMessage.getCommand().name() + "> Pacote #" + i + " de " + fileInfo.getTotalPackets() + " recebido.");

                outputStream.write(receiveMessage.getData());

                // envia mensagem ACK para o client
                sendMessage = new Message(CommandType.ACK, sequence);
                sendData = ObjectConverter.convertObjectToBytes(sendMessage);
                sendPacket.setData(sendData);
                socket.send(sendPacket);
            }
        }
        return true;
    }

    private boolean writeFileSlowStart() throws NoSuchAlgorithmException, IOException
    {
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileInfo.getFileName())))
        {
            for(long i = 1; i <= fileInfo.getTotalPackets(); i++)
            {
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.setSoTimeout(SOCKET_TIMEOUT);
                try {
                    socket.receive(receivePacket);
                } catch (SocketTimeoutException e) {
                    System.out.println("ERRO> Timeout para recebimento da confirmacao.");
                    sendConnectionReset(sequence);
                    sequence--;
                    i--;
                    continue;
                }
                socket.setSoTimeout(0);
                receiveData = receivePacket.getData();
                receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

                if(receiveMessage.getCommand() == CommandType.DATA)
                {
                    if (receiveMessage.getSequence() == ++sequence)
                    {
                        outputStream.write(receiveMessage.getData());
                        System.out.println("S#" + receiveMessage.getSequence()  + " " + receiveMessage.getCommand().name() + "> Pacote #" + i + " de " + fileInfo.getTotalPackets() + " recebido.");
                        
                        // envia mensagem ACK para o client
                        sendMessage = new Message(CommandType.ACK, sequence);
                        sendData = ObjectConverter.convertObjectToBytes(sendMessage);
                        sendPacket.setData(sendData);
                        socket.send(sendPacket);
                        continue;
                    }
                    else
                    {
                        if (receiveMessage.getSequence() < sequence)
                        {
                            System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Reenvio do ACK S#" + receiveMessage.getSequence());

                            // reenvia mensagem ACK para o client
                            sendMessage = new Message(CommandType.ACK, receiveMessage.getSequence());
                            sendData = ObjectConverter.convertObjectToBytes(sendMessage);
                            sendPacket.setData(sendData);
                            socket.send(sendPacket);
                            continue;
                        }
                        if (receiveMessage.getSequence() > sequence)
                        {
                            System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Resetando conexao para S#" + sequence);
                            sendConnectionReset(sequence);
                            sequence--;
                            i--;
                            continue;
                        }
                    }
                }
                else
                {
                    System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Resetando conexao para S#" + sequence);
                    sendConnectionReset(sequence);
                    sequence--;
                    i--;
                }
            }
        }
        return true;
    }

    private boolean finallizeConnection(boolean status)
    {
        try {
            if (status)
            {
                // recebe mensagem FIN
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                receiveData = receivePacket.getData();
                receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

                if(!(receiveMessage.getCommand() == CommandType.FIN && receiveMessage.getSequence() == ++sequence))
                {
                    System.out.println("ERRO> <S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Falha no recebimento de encerramento da conexao.");
                    return false;
                }
                System.out.println("S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Pedido de encerramento da conexao recebido do <client>.");

                // envia mensagem ACK para o client
                sendMessage = new Message(CommandType.ACK, sequence);
                sendData = ObjectConverter.convertObjectToBytes(sendMessage);
                sendPacket.setData(sendData);
                socket.send(sendPacket);
            }

            // envia mensagem FIN para o client
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
            System.out.println("S#" + receiveMessage.getSequence() + " " + receiveMessage.getCommand().name() + "> Confirmacao do recebimento do pedido de encerramento pelo <server>.");

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
        System.out.println("Uso do programa: Server <port>");
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
            boolean status = server.establishConnection();
            if (status)
            {
                status = server.receiveFile();
                server.finallizeConnection(status);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}


