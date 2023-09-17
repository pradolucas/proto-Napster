package napsterPkg;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class Server {

	public static void main(String[] args) throws IOException, AlreadyBoundException {

		// Captura IP
		System.out.println("Entre o IP");
		Scanner scannerServer = new Scanner(System.in);
		String ipServer = scannerServer.nextLine();
		
		// Captura porta
		System.out.println("Entre a porta do Register");
		Scanner scannerReg = new Scanner(System.in);
		int portReg = scannerReg.nextInt();

		// Instancia hashmap com relacao de arquivo-Peer
		// Passa lista para implementacao
		Map<String, Set<String>> filesPeers = new HashMap<>();
		ServiceReq sq = new ServiceReqImpl(filesPeers);

		// Cria Registry
		LocateRegistry.createRegistry(portReg);

		// Acessa Registry
		Registry reg = LocateRegistry.getRegistry();

		// faz bind obj ao registry
		reg.bind("rmi://" + ipServer + "/ServiceReq", sq);
	}
}
