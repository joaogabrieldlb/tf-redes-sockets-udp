import java.io.*; 
import java.net.*;

class TCPClient2 {

	public static void main(String args[]) throws Exception { 
		if (args.length != 2) {
			System.out.println("Usage: TCPClient2 <server ip> <port>");
			
			return;
		}
		
		String sentence;
		String echo;

		/* Permite leitura de dados do teclado */
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

		/* Cria o socket cliente indicando o IP e porta de destino. */
		Socket clientSocket = new Socket(args[0], Integer.parseInt(args[1]));

		/* Cria uma stream de saída para enviar dados para o servidor */
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

		/* Cria uma stream de entrada para receber os dados do servidor */
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		while (true) {
			/* Lê uma mensagem digitada pelo usuário */
			sentence = inFromUser.readLine();

			/* Envia para o servidor. Não esquecer do \n no final para permitir que o servidor use readLine */
			outToServer.writeBytes(sentence + '\n');

			/* Lê mensagem de resposta do servidor */
			echo = inFromServer.readLine();

			System.out.println("FROM SERVER: " + echo);
			
			if (sentence.equals("quit")) break;
		}

		/* Encerra conexão */
		clientSocket.close();
	}
}
