import java.io.*; 
import java.net.*; 

class Server {
	// Converte object para byte[]
	public static byte[] convertObjectToBytes(Object obj) {
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		try (ObjectOutputStream ois = new ObjectOutputStream(boas)) {
			ois.writeObject(obj);
			
			return boas.toByteArray();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			
			return null;
		}
	}

	// Converte byte[] para object
	public static Object convertBytesToObject(byte[] bytes) {
		InputStream is = new ByteArrayInputStream(bytes);
		try (ObjectInputStream ois = new ObjectInputStream(is)) {
			
			return ois.readObject();
		} catch (IOException | ClassNotFoundException ioe) {
			ioe.printStackTrace();
			
			return null;
		}
	}

	public static void main(String args[]) throws Exception{
		if (args.length != 1) {
			System.out.println("Usage: Server <port>");
			
			return;
		}

		/* DatagramSocket implementa socket UDP */
		DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(args[0]));
		
		/* Servidor fica recebendo mensagens por tempo indeterminado */
		while (true) {
			/* Buffer para recepção de dados */
			byte[] receiveData = new byte[1024];

			/* DatagramPacket representa um pacote UDP, repare que ele esta associado ao buffer reservado para recepção */
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			/* Aguarda o recebimento de uma mensagem. O servidor fica aguardando neste ponto 
			 * até que uma mensagem seja recebida */
			serverSocket.receive(receivePacket);
			
			Person p = (Person) convertBytesToObject(receiveData);
			
			/* Pega o IP do remetente */
			InetAddress IPAddress = receivePacket.getAddress();
			
			/* Pega o número de porta do remetente */
			int port = receivePacket.getPort();
			
			/* Exibe, IP:port => msg */
			System.out.println(IPAddress.getHostAddress() + ":" + port + " => ");

			System.out.println("Name: " + p.name);
			System.out.println("Birth year: " + p.birthYear);
			System.out.println("Occupation: " + p.occupation);
		}       
	} 
}
