import java.io.*; 
import java.net.*; 

class UDPClient2 {    
	
	public static void main(String args[]) throws Exception{
		if (args.length != 2) {
			System.out.println("Usage: UDPClient2 <server ip> <port>");
			
			return;
		}


		/* Permite leitura de dados do teclado */
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		
		/* DatagramSocket implementa socket UDP */
		DatagramSocket clientSocket = new DatagramSocket();
		
		/* Faz a tradução do nome do host para o seu IP (DNS). Também pode-se entrar com o IP no formato literal, exemplo 127.0.0.1.*/
		InetAddress IPAddress = InetAddress.getByName(args[0]);

		while (true) {
			/* Buffer reservado para envio dos dados */
			byte[] sendData = new byte[1024];
			
			/* Lê uma sentença digitada pelo usuário e converte para o buffer de envio */
			String sentence = inFromUser.readLine();
			sendData = sentence.getBytes();

			if (sentence.equals("quit")) break;
			
			/* Cria um novo pacote UDP, configurando com o IP e porta de destino.*/
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, Integer.parseInt(args[1]));
			
			/* Envia o pacote UDP */
			clientSocket.send(sendPacket);
			
			/* Buffer para recepção de dados */
			byte[] receiveData = new byte[1024];

			/* DatagramPacket representa um pacote UDP, repare que ele esta associado ao buffer reservado para recepção */
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			/* Aguarda o recebimento de uma mensagem. O servidor fica aguardando neste ponto 
			 * até que uma mensagem seja recebida */
			clientSocket.receive(receivePacket);
			
			/* Converte o buffer recebido para um objeto string. Isso é válido porque estamos aguardando mensagens textuais. */
			sentence = new String(receivePacket.getData());
			
			/* Pega o IP do remetente */
			IPAddress = receivePacket.getAddress();
			
			/* Pega o número de porta do remetente */
			int port = receivePacket.getPort();
			
			/* Exibe, IP:port => msg */
			System.out.println(IPAddress.getHostAddress() + ":" + port + " => " + sentence);
		}
		
		clientSocket.close();
	}
}
