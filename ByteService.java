// Giao dien tu xa - BUOC PHAI dung package RMI va dung ten/chu ky phuong thuc,
// neu khac thi lookup se khong cast duoc stub tra ve tu registry.
//
// Phai de public va nam trong file ByteService.java: Java bat buoc moi type
// public phai o file cung ten. De package-private trong cung file voi client
// thi javac chiu, nhung RMI tao dynamic proxy cho stub se de vo IllegalAccessError.
package RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ByteService extends Remote {
    public byte[] requestData(String studentCode, String qCode) throws RemoteException;

    public void submitData(String studentCode, String qCode, byte[] data) throws RemoteException;
}
