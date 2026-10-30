// Bai RMI - ByteService: tim phan tu xuat hien it nhat trong mang byte
// qCode: mPZ5eOUX
// Chay: javac -d . *.java  roi  java RMI.QmPZ5eOUX
package RMI;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.LinkedHashMap;
import java.util.Map;

class QmPZ5eOUX {
    static final String HOST = "36.50.135.242";
    static final int RMI_PORT = 1099; // cong registry mac dinh
    static final String SERVICE_NAME = "RMIByteService";

    static final String MSV = "MSV_CUA_NVH";
    static final String QCODE = "mPZ5eOUX";

    public static void main(String[] args) throws Exception {
        Registry registry = LocateRegistry.getRegistry(HOST, RMI_PORT);
        ByteService service = (ByteService) registry.lookup(SERVICE_NAME);
        System.out.println("Da lookup duoc " + SERVICE_NAME + " tai " + HOST + ":" + RMI_PORT);

        // a. nhan mang byte tu server
        byte[] data = service.requestData(MSV, QCODE);
        if (data == null || data.length == 0) {
            throw new IllegalStateException("Server tra ve mang rong");
        }
        System.out.println("Nhan " + data.length + " byte");

        // b. tim phan tu co so lan xuat hien IT NHAT.
        //    LinkedHashMap giu thu tu chen = thu tu xuat hien lan dau, nen khi
        //    so sanh bang dau '<' (khong phai '<=') thi phan tu dat min dau tien
        //    se thang -> dung yeu cau "tra ve phan tu dau tien xuat hien".
        LinkedHashMap<Byte, Integer> count = new LinkedHashMap<>();
        for (byte b : data) {
            Integer old = count.get(b);
            count.put(b, old == null ? 1 : old + 1);
        }

        byte best = 0;
        int bestCount = Integer.MAX_VALUE;
        for (Map.Entry<Byte, Integer> e : count.entrySet()) {
            if (e.getValue() < bestCount) {
                bestCount = e.getValue();
                best = e.getKey();
            }
        }
        System.out.println("Phan tu it nhat = " + best + " (xuat hien " + bestCount + " lan)");

        if (bestCount > 127) {
            System.out.println("CANH BAO: so lan xuat hien " + bestCount
                    + " > 127 nen khi ep sang byte se bi tran: " + (byte) bestCount);
        }

        // c. gui ket qua {phan tu, so lan xuat hien}
        //    Vi du de bai: [1,2,3,2,1] -> gui [3,1]
        byte[] result = new byte[] { best, (byte) bestCount };
        service.submitData(MSV, QCODE, result);
        System.out.println("Da gui: [" + result[0] + ", " + result[1] + "]");

        // d. ket thuc chuong trinh
    }
}
