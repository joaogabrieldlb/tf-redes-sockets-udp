import java.io.*; 
import java.net.*; 

class UDPServer {    
	public static void main(String args[]) throws Exception{
		/* DatagramSocket implementa socket UDP */
		DatagramSocket serverSocket = new DatagramSocket(9876);
		
		/* Servidor fica recebendo mensagens por tempo indeterminado */
		while (true) {
			/* Buffer para recepção de dados */
			byte[] receiveData = new byte[1024];

			/* DatagramPacket representa um pacote UDP, repare que ele esta associado ao buffer reservado para recepção */
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			/* Aguarda o recebimento de uma mensagem. O servidor fica aguardando neste ponto 
			 * até que uma mensagem seja recebida */
			serverSocket.receive(receivePacket);
			
			/* Converte o buffer recebido para um objeto string. Isso é válido porque estamos aguardando mensagens textuais. */
			String sentence = new String(receivePacket.getData());
			
			/* Pega o IP do remetente */
			InetAddress IPAddress = receivePacket.getAddress();
			
			/* Pega o número de porta do remetente */
			int port = receivePacket.getPort();
			
			/* Exibe, IP:port => msg */
			System.out.println(IPAddress.getHostAddress() + ":" + port + " => " + sentence);
		}       
	} 
}
