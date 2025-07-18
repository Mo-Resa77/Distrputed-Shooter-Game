// " first class "
package shooter;

import java.rmi.Remote;
import java.rmi.RemoteException;

//This is the RMI interface that defines how nodes communicate: 
public interface NodeI extends Remote { //to other nodes

    void sendAction(Action action) throws RemoteException;

    void multicastAction(String encryptedAction) throws RemoteException;

    void receiveAction(String encryptedAction) throws RemoteException;
}
