import java.io.*; 
import java.net.*; 

class Client {    
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
		if (args.length != 2) {
			System.out.println("Usage: Client <server ip> <port>");
			
			return;
		}

		/* Permite leitura de dados do teclado */
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		
		/* DatagramSocket implementa socket UDP */
		DatagramSocket clientSocket = new DatagramSocket();
		
		/* Faz a tradução do nome do host para o seu IP (DNS). Também pode-se entrar com o IP no formato literal, exemplo 127.0.0.1.*/
		InetAddress IPAddress = InetAddress.getByName(args[0]);

		while (true) {
			Person p = new Person();
			
			System.out.println("Name:");
			p.name = inFromUser.readLine();
			System.out.println("Birth year:");
			p.birthYear = Short.parseShort(inFromUser.readLine());
			System.out.println("Occupation:");
			p.occupation = inFromUser.readLine();

			if (p.name.equals("quit")) break;

			byte[] sendData = convertObjectToBytes(p);
			
			/* Cria um novo pacote UDP, configurando com o IP e porta de destino.*/
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, Integer.parseInt(args[1]));
			
			/* Envia o pacote UDP */
			clientSocket.send(sendPacket);
		}
		
		clientSocket.close();
	}
}
