package napsterPkg;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Peer extends Thread {
	public static String ipAddr;
	public static int numberPort;
	public static String pathFolder;
	private static ServiceReq sreq;

	public Peer(String ip, int numberPort, String pathFolder) {
		Peer.ipAddr = ip;
		Peer.numberPort = numberPort;
		Peer.pathFolder = pathFolder;
	}

	@Override
	public void run() {
		// Cria registry no ato de instancia da classe Peer
		try {
			Registry reg = LocateRegistry.getRegistry();
			Peer.sreq = (ServiceReq) reg.lookup("rmi://127.0.0.1/ServiceReq");
		} catch (RemoteException | NotBoundException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {

		// Captura IP, port e pasta dos arquivos
		System.out.println("Insira o IP:");
		BufferedReader bufUser1 = new BufferedReader(new InputStreamReader(System.in));
		String ip = bufUser1.readLine();

		System.out.println("Insira a Porta:");
		BufferedReader bufUser2 = new BufferedReader(new InputStreamReader(System.in));
		Integer port = Integer.parseInt(bufUser2.readLine());

		System.out.println("Insira a pasta dos arquivos:");
		BufferedReader bufUser3 = new BufferedReader(new InputStreamReader(System.in));
		String folderPath = bufUser3.readLine();
		
		// Instancia um peer e inicia sua thread
		Peer p = new Peer(ip, port, folderPath);
		p.start();
		
		// funcao para escutar requisicoes de outros Peers
		listenPeers();

		List<String> searchResponse = null;
		String searchFile = null;
		while (true) {
			System.out.print("\nJOIN(1)\nSEARCH(2)\nDOWNLOAD(3)\n>>>");
			try {
				Scanner scanner = new Scanner(System.in);
				int option = scanner.nextInt();
				switch (option) {
				case 1:
					// chamada para funcao que realiza chamada remota para o servidor da funcao join
					joinPeer(ipAddr, numberPort, listFiles(pathFolder));
					break;
				case 2:
					// Nome do arquivo a ser pesquisado
					System.out.print("\nNome do arquivo\n>>>");
					BufferedReader bufUser4 = new BufferedReader(new InputStreamReader(System.in));
					searchFile = bufUser4.readLine();

					// chamada para funcao que realiza chamada remota para o servidor da funcao search
					searchResponse = searchFile(searchFile, ipAddr, numberPort);
					break;
				case 3:
					if (searchResponse == null) {
						break;
					}
					System.out.println("Selecione um dos peers que possuem o arquivo:" + joinFilesStr(searchResponse));
					System.out.println("Insira o IP:");
					BufferedReader bufUserIp = new BufferedReader(new InputStreamReader(System.in));
					String ipDownload = bufUserIp.readLine();

					System.out.println("Insira a Porta:");
					BufferedReader bufUserPort = new BufferedReader(new InputStreamReader(System.in));
					Integer portDownload = Integer.parseInt(bufUserPort.readLine());

					// Instanciacao e execucao de thread que realiza download de arquivo
					DownloadThread Download = new DownloadThread(searchFile, ipDownload, portDownload);
					Download.start();
					break;
				default:
					System.out.print("Opção inválida\n");
				}
			} catch (NumberFormatException | InputMismatchException e) {
				System.out.print("Opção inválida\n");
			}
		}
	}

	private static String joinFilesStr(List<String> list) {
		// Pretty print
		return String.join(", ", list);
	}

	public static List<String> listFiles(String pathFolder) throws IOException {
		// Listar arquivos no diretorio
		return Files.walk(Paths.get(pathFolder)).filter(Files::isRegularFile).map(Path::getFileName).map(Path::toString)
				.collect(Collectors.toList());
	}

	private static void joinPeer(String ip, Integer port, List<String> files) throws RemoteException {
		// Chamada remota para o servidor
		String joinResponse = sreq.join(ip, port, files); // Blocking
		System.out.println("Sou peer " + ip + ":" + port + " com arquivos " + joinFilesStr(files));
	}

	private static List<String> searchFile(String searchFile, String ip, int port) throws IOException, RemoteException {
		// Chamada remota para o servidor
		List<String> searchResponse = sreq.search(searchFile, ip, port);
		System.out.println("peers com arquivo solicitado:" + joinFilesStr(searchResponse));
		return searchResponse;
	}

	private static void downloadFileFromPeer(String fileName, String ipDownload, Integer portDownload)
			throws IOException {

		// Socket para conexao
		Socket s = new Socket(ipDownload, portDownload);

		// Stream para Escrita
		OutputStream ostream = s.getOutputStream();
		DataOutputStream writer = new DataOutputStream(ostream);
		
		// Envio de mensagem
		writer.writeBytes("DOWNLOAD|" + fileName + "\n");

		// Leitura do arquivo que será recebido
		// buffer de tamanho 1024 para a leitura do arquivo recebido
		byte[] buffer = new byte[1024];
		int count;
		InputStream inputStream = s.getInputStream();
		// Caso o buffer tenha lido mais que 0 bytes, grave
		if ((count = inputStream.read(buffer)) > 0) {
			// Arquivo de output
			FileOutputStream fileOutputStream = new FileOutputStream(pathFolder + "\\" + fileName);
			do {
				// Escrita do arquivo
				fileOutputStream.write(buffer, 0, count);
			} while ((count = inputStream.read(buffer)) > 0);
			fileOutputStream.close();
			System.out.println("\nArquivo " + fileName + " baixado com sucesso na pasta " + pathFolder);
			// Update indicando ao servido que o Peer agora possui o arquivo
			String updateResponse = sreq.update(fileName, ipAddr, numberPort); // Blocking
		}
		inputStream.close();
		s.close();
	}

	private static void sendFileToPeer(String filePath, Socket s) throws IOException {
		// Checagem se arquivo existe no diretório do Peer
		File file = new File(filePath);
		if (file.exists()) {
			
			// Stream para leitura do arquivo
			FileInputStream fileInputStream = new FileInputStream(filePath);
			// buffer com vazao de tamanho 1024 bytes.
			byte[] buffer = new byte[1024];
			int bytesRead;
			OutputStream ostream = s.getOutputStream();

			// -1 indica final do arquivo
			// Grava os bytes no buffer que sao enviados através do ostream
			while ((bytesRead = fileInputStream.read(buffer)) != -1) {
				ostream.write(buffer, 0, bytesRead);
			}
			fileInputStream.close();
		}
	}

	// Thread para aceitar requisicoes de conexao por outros peers
	private static void listenPeers() throws UnknownHostException, IOException {
		new Thread() {
			public void run() {
				ServerSocket listenSocket;
				try {
					listenSocket = new ServerSocket(numberPort);

					while (true) {
						Socket connectSocket = listenSocket.accept();
						ConnThread ListeningThread = new ConnThread(connectSocket);
						ListeningThread.start();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	// Thread de atendimento de pedido de Download por outro Peer
	static class ConnThread extends Thread {
		private Socket node;

		public ConnThread(Socket node) {
			this.node = node;
		}

		@Override
		public void run() {
			try {
				// buffer para Leitura
				InputStreamReader istream = new InputStreamReader(this.node.getInputStream());
				BufferedReader reader = new BufferedReader(istream);

				// Leitura de mensagem
				String msg = reader.readLine();
				String[] splits = msg.split("\\|");
				if (splits[0].equals("DOWNLOAD")) {
					String filename = splits[1];
					// Escrita de mensagem
					sendFileToPeer(pathFolder + "\\" + filename, this.node);
				}
				this.node.close();
				System.out.print("\n>>>");
			} catch (IOException e) {
			}
		}
	}
	
	// Thread para pedido de download
	static class DownloadThread extends Thread {

		private String fileName;
		private String ipDownload;
		private Integer portDownload;

		public DownloadThread(String fileName, String ipDownload, Integer portDownload) {
			this.fileName = fileName;
			this.ipDownload = ipDownload;
			this.portDownload = portDownload;
		}

		@Override
		public void run() {
			try {
				downloadFileFromPeer(fileName, ipDownload, portDownload);
			} catch (IOException e) {
			}
		}
	}

}