import java.io.*; 
import java.net.*; 

class UDPClient {    
	
	public static void main(String args[]) throws Exception{
		/* Permite leitura de dados do teclado */
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		
		/* DatagramSocket implementa socket UDP */
		DatagramSocket clientSocket = new DatagramSocket();
		
		/* Faz a tradução do nome do host para o seu IP (DNS). Também pode-se entrar com o IP no formato literal, exemplo 127.0.0.1.*/
		InetAddress IPAddress = InetAddress.getByName("localhost");
		
		/* Buffer reservado para envio dos dados */
		byte[] sendData = new byte[1024];
		
		/* Lê uma sentença digitada pelo usuário e converte para o buffer de envio */
		String sentence = inFromUser.readLine();
		sendData = sentence.getBytes();
		
		/* Cria um novo pacote UDP, configurando com o IP e porta de destino.*/
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
		
		/* Envia o pacote UDP */
		clientSocket.send(sendPacket);
		
		clientSocket.close();
	}
} 