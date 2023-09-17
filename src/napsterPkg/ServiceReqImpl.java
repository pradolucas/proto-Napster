package napsterPkg;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServiceReqImpl extends UnicastRemoteObject implements ServiceReq {
	private Map<String, Set<String>> filesPeers;

	public ServiceReqImpl(Map<String, Set<String>> filesPeers) throws RemoteException {
		super();
		this.filesPeers = filesPeers;
	}

	@Override
	public String join(String ip, int port, List<String> files) throws RemoteException {
		// Usa a instancia de hashmap e a atualiza conforma a chave existir ou nao
		String ipport = ip + ":" + port;
		for (String file : files) {
			if (this.filesPeers.containsKey(file)) {
				this.filesPeers.get(file).add(ipport);
			} else {
				Set<String> ipportSet = new HashSet<>(Arrays.asList(ipport));
				this.filesPeers.put(file, ipportSet);
			}
		}
		System.out.println("Peer " + ipport + " adicionado com arquivos " + String.join(", ", files));
		return "JOIN_OK";
	}

	@Override
	public List<String> search(String fileName, String ip, int port) throws RemoteException {
		// Procura se existe a chave no HashMap, caso nao exista devolve uma lista vazia
		Set<String> response = this.filesPeers.getOrDefault(fileName, new HashSet<>());
		List<String> castResponse = new ArrayList<>(response);
		System.out.println("Peer " + ip + ":" + port + " solicitou arquivo " + fileName);
		return castResponse;
	}

	@Override
	public String update(String fileName, String ip, int port) throws RemoteException {
		// Atualizacao da tabela Hash
		String ipport = ip + ":" + port;
		Set<String> ipportSet = new HashSet<>(Arrays.asList(ipport));
		if (this.filesPeers.containsKey(fileName)) {
			this.filesPeers.get(fileName).add(ipport);
		} else {
			this.filesPeers.put(fileName, ipportSet);
		}
		return "UPDATE_OK";
	}

}
