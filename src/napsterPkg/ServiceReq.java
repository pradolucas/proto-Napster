package napsterPkg;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServiceReq extends Remote {
	// assinatura dos metodos
	public String join(String ip, int port, List<String> files) throws RemoteException;
	public List<String> search(String fileName, String ip, int port) throws RemoteException;
	public String update(String fileName, String ip, int port) throws RemoteException;
}
