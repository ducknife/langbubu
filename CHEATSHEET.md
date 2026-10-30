# Khung cú pháp cần nhớ — RMI & SOAP

## RMI client (3 dòng cốt lõi)

```java
Registry reg = LocateRegistry.getRegistry("36.50.135.242", 1099);
ByteService s = (ByteService) reg.lookup("RMIByteService");
byte[] d = s.requestData(MSV, QCODE);
s.submitData(MSV, QCODE, result);
```

Interface phải tự viết lại, **đúng package + đúng chữ ký**, để `public`, file riêng:

```java
package RMI;
import java.rmi.*;
public interface ByteService extends Remote {
    byte[] requestData(String studentCode, String qCode) throws RemoteException;
    void submitData(String studentCode, String qCode, byte[] data) throws RemoteException;
}
```

```
javac -d . *.java
java RMI.QmPZ5eOUX
```

Nhớ: `LocateRegistry.getRegistry(host, 1099)` → `lookup(tên)` → cast → gọi như hàm thường.

---

## SOAP client (JDK 8 — đường ngắn, nhớ cái này)

Bước 1, sinh stub từ WSDL:

```
wsimport -keep -p client http://36.50.135.242:2221/DataService?wsdl
```

Bước 2, 4 dòng:

```java
DataServiceService svc = new DataServiceService();   // tên lớp do WSDL sinh ra
DataService port = svc.getDataServicePort();         // getXxxPort()
List<Integer> data = port.getData(MSV, QCODE);
port.submitDataStringArray(MSV, QCODE, Arrays.asList("EVEN=" + even, "ODD=" + odd));
```

Tên `DataServiceService` / `getDataServicePort` là do `wsimport` sinh — mở thư mục
`client/` xem tên thật, quy tắc là `<Tên>Service` và `get<Tên>Port`.

**Bẫy:** `wsimport` chỉ có trên JDK 8. JDK 11+ đã bỏ JAX-WS khỏi JDK → không có
`wsimport`, không có `javax.xml.ws`. Kiểm tra trước bằng `java -version`.

---

## Đếm chẵn/lẻ

```java
for (int v : data) if (v % 2 == 0) even++; else odd++;
```

Dùng `v % 2 == 0` chứ đừng dùng `v % 2 == 1` — số âm cho `-1` nên sẽ đếm sai.

---

## Khung chung mọi bài

1. Gửi `studentCode` + `qCode` để lấy dữ liệu.
2. Xử lý.
3. Gửi kết quả lên bằng phương thức submit tương ứng.
4. Kết thúc.
