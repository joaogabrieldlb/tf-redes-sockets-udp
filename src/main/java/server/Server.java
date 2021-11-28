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
import main.java.lib.Message.CommandType;

public class Server 
{
    private final int PORT;
    private int sequence = 0;
    private DatagramSocket socket;
    DatagramPacket receivePacket;
    DatagramPacket sendPacket;
    
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
        byte[] incomingData = new byte[1024];
        byte[] sendData = new byte[1024];
        try
        {
            // implementa socket UDP com a porta
            socket = new DatagramSocket(PORT);
            System.out.println("Server escutando na porta " + PORT);

            while(true)
            {
                // instancia pacote UDP de recepcao
                receivePacket = new DatagramPacket(incomingData, incomingData.length);
                // inicializado null pois aguarda recepcao de pacote UDP
                sendPacket = null;
                // guarda a mensagem recebida do cliente
                Message receiveMessage = null;
                
                // estabelece a conexao
                // retorna true se a conexao foi corretamente estabelecida
                if (!establishedConnection(incomingData, sendData, sendPacket, receivePacket))
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
                            Object data = receiveMessage.getData();
                            FileInfo fileInfo;
                            if (data instanceof FileInfo)
                            {
                                fileInfo = (FileInfo) ObjectConverter.convertBytesToObject(receiveMessage.getData());
                            }
                            else
                            {
                                System.out.println("Erro: Não foi recebido informacoes validas sobre o arquivo!");
                                continue;
                            }
                            transferFile(fileInfo, receivePacket, sendPacket);
                            break;
                        case FIN:
                            
                        default:
                            continue;
                    }
                    break;
                }

                // finaliza conexao
                


                // Message receiveMessage = Message.convertBytesToObject(receivePacket.getData());

                // if (receiveMessage.getCommand() == 'U' && receiveMessage.getSequence() == ++sequence)
                // {
                //     System.out.println("Recebido pedido de upload de arquivo.");
                //     sendData = Message.convertObjectToBytes(new Message('U', sequence, "Send File name"));
                //     sendPacket.setData(sendData);
                //     serverSocket.send(sendPacket);
                // }
                // else
                // {
                //     System.out.println("Pedido de conexao invalido.");
                //     System.out.println("Resetando conexao.");
                //     sequence = 0;
                //     sendData = Message.convertObjectToBytes(new Message('R', sequence, ""));
                //     sendPacket.setData(sendData);
                //     serverSocket.send(sendPacket);
                //     return;
                // }

                // serverSocket.receive(receivePacket);
                // if (receiveMessage.getCommand() == 'U' && receiveMessage.getSequence() == ++sequence)
                // {
                //     System.out.println("Recebido nome de arquivo: " + receiveMessage.getData());
                //     file = new FileWriter(receiveMessage.getData());

                //     sendData = Message.convertObjectToBytes(new Message('U', sequence, "Send File lenght"));
                //     sendPacket.setData(sendData);
                //     serverSocket.send(sendPacket);
                // }

                // serverSocket.receive(receivePacket);
                // if (receiveMessage.getCommand() == 'U' && receiveMessage.getSequence() == ++sequence)
                // {
                //     System.out.println("Recebido tamanho do arquivo: " + receiveMessage.getData());
                //     int fileSize = Integer.parseInt(receiveMessage.getData());
                //     int packetsCount = (int) Math.ceil(fileSize / 400.0);

                //     sendData = Message.convertObjectToBytes(new Message('U', sequence, "Send File data"));
                //     sendPacket.setData(sendData);
                //     serverSocket.send(sendPacket);
                // }
                // {
                //     System.out.println("Recebido pedido de arquivo.");
                //     file = new FileInfo(receiveMessage.getFileName(), receiveMessage.getFileSize());
                //     sendMessage(new Message('s', 0, ""), sendPacket);
                // }
                // else if (receiveMessage.getCommand().equals('f'))
                // {
                //     System.out.println("Recebido pedido de fragmento.");
                //     if (file == null)
                //     {
                //         System.out.println("Arquivo não foi recebido.");
                //         sendMessage(new Message('f', 0, ""), sendPacket);
                //     }
                //     else
                //     {
                //         file.addFragment(receiveMessage.getSequence(), receiveMessage.getData());
                //         if (file.isComplete())
                //         {
                //             System.out.println("Arquivo completo.");
                //             sendMessage(new Message('f', 0, ""), sendPacket);
                //         }
                //         else
                //         {
                //             System.out.println("Arquivo incompleto.");
                //             sendMessage(new Message('f', 0, ""), sendPacket);
                //         }
                //     }
                // }
                // else if (receiveMessage.getCommand().equals('c'))
                // {
                //     System.out.println("Recebido pedido de confirmação.");
                //     if (file == null)
                //     {
                //         System.out.println("Arquivo não foi recebido.");
                //         sendMessage(new Message('c', 0, ""), sendPacket);
                //     }
                //     else
                //     {
                //         if (file.isComplete())
                //         {
                //             System.out.println("Arquivo completo.");
                //             sendMessage(new Message('c', 0, ""), sendPacket);
                //         }
                //         else
                //         {
                //             System.out.println("Arquivo incompleto.");
                //             sendMessage(new Message('c', 0, ""), sendPacket);
                //         }
                //     }
                // }
                // else
                // {
                //     System.out.println("Comando inválido.");
                // {
                //     break;
                // }

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

                // byte[] data = receivePacket.getData();
                // file = (FileInfo) convertToObject(data);
                // System.out.println("Received file: " + file.getFileName());
                // System.out.println("Saving file to: " + file.getDestinationDirectory());
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

    private boolean establishedConnection(byte[] incomingData, byte[] sendData,
        DatagramPacket sendPacket, DatagramPacket receivePacket) throws IOException
    {
        // aguarda o recebimento de uma mensagem
        socket.receive(receivePacket);
        
        // coleta dados do cliente que enviou a mensagem
        clientAddress = receivePacket.getAddress();
        clientPort = receivePacket.getPort();
        
        sendPacket = new DatagramPacket(sendData, sendData.length,
            clientAddress, clientPort);

        // converte byte array recebido no objeto mensagem
        Message receiveMessage = (Message) ObjectConverter.convertBytesToObject(receivePacket.getData());
        
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
            System.out.println("Resetando conexao sequencia: " + sequence);
            sequence = 0;
            // envia mensagem de RST
            sendData = ObjectConverter.convertObjectToBytes(new Message(CommandType.RST, sequence));
            sendPacket.setData(sendData);
            socket.send(sendPacket);
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
            System.out.println("Resetando conexao sequencia: " + sequence);
            sequence = 0;
            // envia mensagem de RST
            sendData = ObjectConverter.convertObjectToBytes(new Message(CommandType.RST, sequence));
            sendPacket.setData(sendData);
            socket.send(sendPacket);
            // conexao nao estabelecida
            return false;
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


