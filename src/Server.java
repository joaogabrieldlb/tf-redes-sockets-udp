
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;

public class Server 
{
    private final int PORT;
    // private static final int FILE_BUFFER_SIZE = 512;  
    private int sequence = 0;
    private FileInfo fileInfo;
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

    private boolean receiveFile() throws IOException, NoSuchAlgorithmException
    {
        socket.receive(receivePacket);
        receiveData = receivePacket.getData();
        receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);
        
        if(!(receiveMessage.getCommand() == CommandType.UPLOAD && receiveMessage.getSequence() == ++sequence))
        {
            System.out.println("Falha no recebimento do FileInfo.");
            return false;
        }
        
        System.out.println("FileInfo recebido com sucesso.");
        fileInfo = (FileInfo) ObjectConverter.convertBytesToObject(receiveMessage.getData());

        // envia mensagem ACK para o client
        sendMessage = new Message(CommandType.ACK, sequence);
        sendData = ObjectConverter.convertObjectToBytes(sendMessage);
        sendPacket.setData(sendData);
        socket.send(sendPacket);
        
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileInfo.getFileName()));

        for(int i = 1; i <= fileInfo.getTotalPackets(); i++)
        {
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            receiveData = receivePacket.getData();
            receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

            if(!(receiveMessage.getCommand() == CommandType.DATA && receiveMessage.getSequence() == ++sequence))
            {
                System.out.println("Conexao perdida no recebimento dos dados do arquivo.");
                connectionReset();
                return false;
            }
            System.out.println("Pacote #" + i + " de " + fileInfo.getTotalPackets() + " recebido com sucesso.");

            outputStream.write(receiveMessage.getData());

            // envia mensagem ACK para o client
            sendMessage = new Message(CommandType.ACK, sequence);
            sendData = ObjectConverter.convertObjectToBytes(sendMessage);
            sendPacket.setData(sendData);
            socket.send(sendPacket);
        }
        outputStream.close();

        File file = new File(fileInfo.getFileName());
        boolean result = FileInfo.computeMD5(file).equals(fileInfo.getFileHash());

        if (result)
        {
            // envia mensagem SUCCESS para o client
            sendMessage = new Message(CommandType.SUCCESS, ++sequence);
            sendData = ObjectConverter.convertObjectToBytes(sendMessage);
            sendPacket.setData(sendData);
            socket.send(sendPacket);

            System.out.println("Arquivo recebido com sucesso.");
        }
        else
        {
            // envia mensagem FAILURE para o client
            sendMessage = new Message(CommandType.FAILURE, ++sequence);
            sendData = ObjectConverter.convertObjectToBytes(sendMessage);
            sendPacket.setData(sendData);
            socket.send(sendPacket);

            System.out.println("Falha no recebimento do arquivo.");
        }
        
        // recebe mensagem ACK para do client
        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        receiveData = receivePacket.getData();
        receiveMessage = (Message) ObjectConverter.convertBytesToObject(receiveData);

        if(!(receiveMessage.getCommand() == CommandType.ACK && receiveMessage.getSequence() == sequence))
        {
            System.out.println("Falha na confirmaçao do status do arquivo.");
            // connectionReset();
            return false;
        }

        return result;
    }

    // private void writeFileFragment(byte[] data, String fileName)
    // {
    //     try (OutputStream os = new FileOutputStream(fileName))
    //     {
    //         byte[] buffer = new byte[FILE_BUFFER_SIZE];
    //         buffer = data;
    //         os.write(buffer);
    //     }
    //     catch(Exception e)
    //     {
    //         e.printStackTrace();
    //     }
    // }

    private boolean establishedConnection() throws IOException
    {
        // implementa socket UDP com a porta
        socket = new DatagramSocket(PORT);
        System.out.println("Server escutando na porta " + PORT);
        receivePacket = new DatagramPacket(receiveData, receiveData.length);
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
            System.out.println("Pedido de conexao invalido.");
            connectionReset();
            // conexao nao estabelecida
            return false;
        }

        System.out.println(clientAddress + ":" + clientPort + " - " + "SYN recebido");
        // prepara e envia mensagem de ACK
        sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
        sendData = ObjectConverter.convertObjectToBytes(new Message(CommandType.SYN_ACK, sequence));
        sendPacket.setData(sendData);
        socket.send(sendPacket);

        // aguardando o recebimento do reconhecimento pelo cliente
        socket.receive(receivePacket);

        // valida se a menssagem recebida nao e ACK ou se nao e o numero de sequencia esperado
        if (!(receiveMessage.getCommand() == CommandType.ACK 
                && receiveMessage.getSequence() == sequence))
        {
            System.out.println("Pedido de conexao invalido.");
            connectionReset();
            // conexao nao estabelecida
            return false;
        }

        System.out.println(clientAddress + ":" + clientPort + " - " + "ACK recebido");
        System.out.println("Conexao estabelecida com sucesso!");
        // conexao estabelecida
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
                    System.out.println("Falha no recebimento de encerramento da conexao.");
                    return false;
                }
                System.out.println("Pedido de encerramento recebido.");

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
            System.out.println("Pedido de encerramento enviado.");

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
            System.out.println("Confirmaçao do encerramento da conexao recebida com sucesso pelo client.");

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
        System.out.println("Usage: Server <port>");
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
            boolean status;
            status = server.establishedConnection();
            if (status)
                status = server.receiveFile();
            server.finallizeConnection(status);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

}


