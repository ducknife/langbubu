// Bai SOAP - DataService: dem phan tu chan / le
// qCode: p0nReUnP
// Endpoint: http://36.50.135.242:2221/DataService?wsdl  (SOAP 1.1)
//
// Ban nay KHONG dung wsimport: tu doc WSDL de lay targetNamespace + ten tham so
// that, roi tu dap SOAP envelope va POST bang HttpURLConnection. Ly do: tu JDK 11
// tro len javax.xml.ws / javax.xml.soap da bi bo khoi JDK nen wsimport va
// Service.create khong con san; con DOM parser (javax.xml.parsers) thi van con.
// => file nay compile & chay duoc bang `javac` tran tren JDK 8 ... 21+.
//
// Neu phong thi dung JDK 8 va ban muon ban ngan goi, xem SOAP-RMI-cheatsheet.md.
import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

class Qp0nReUnP {
    static final String HOST = "36.50.135.242";
    static final int PORT = 2221;
    static final String ENDPOINT = "http://" + HOST + ":" + PORT + "/DataService";
    static final String WSDL = ENDPOINT + "?wsdl";

    static final String MSV = "MSV_CUA_NVH";
    static final String QCODE = "p0nReUnP";

    static final String XSD = "http://www.w3.org/2001/XMLSchema";
    static final String ENV = "http://schemas.xmlsoap.org/soap/envelope/";

    // targetNamespace + co qualify tham so con hay khong, doc tu WSDL
    static String targetNs = "";
    static boolean qualified = false;

    // ---------- helpers ----------

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static Document parse(InputStream in) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        DocumentBuilder b = f.newDocumentBuilder();
        return b.parse(in);
    }

    static List<Element> childElements(Node n) {
        List<Element> out = new ArrayList<>();
        NodeList kids = n.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            if (kids.item(i) instanceof Element) out.add((Element) kids.item(i));
        }
        return out;
    }

    // Tim <xs:element name="opName"> trong inline schema cua WSDL roi tra ve
    // ten cac tham so con theo dung thu tu, vd [arg0, arg1, arg2].
    static List<String> paramNames(Document wsdl, String opName) {
        NodeList els = wsdl.getElementsByTagNameNS(XSD, "element");
        for (int i = 0; i < els.getLength(); i++) {
            Element e = (Element) els.item(i);
            if (!opName.equals(e.getAttribute("name"))) continue;

            // elementFormDefault nam tren the <xs:schema> bao ngoai
            Node p = e.getParentNode();
            while (p != null && !(p instanceof Element && XSD.equals(p.getNamespaceURI())
                    && "schema".equals(p.getLocalName()))) {
                p = p.getParentNode();
            }
            if (p != null) {
                qualified = "qualified".equals(((Element) p).getAttribute("elementFormDefault"));
            }

            // element > complexType > sequence > element*
            List<String> names = new ArrayList<>();
            for (Element ct : childElements(e)) {
                if (!"complexType".equals(ct.getLocalName())) continue;
                for (Element seq : childElements(ct)) {
                    for (Element arg : childElements(seq)) {
                        if ("element".equals(arg.getLocalName())) {
                            names.add(arg.getAttribute("name"));
                        }
                    }
                }
            }
            if (!names.isEmpty()) return names;
        }
        return null;
    }

    static String openTag(String name) {
        return qualified ? "<ns:" + name + ">" : "<" + name + ">";
    }

    static String closeTag(String name) {
        return qualified ? "</ns:" + name + ">" : "</" + name + ">";
    }

    // Goi mot operation, tra ve Document cua SOAP response
    static Document call(String opName, List<String> params, List<String> values) throws Exception {
        StringBuilder body = new StringBuilder();
        body.append("<ns:").append(opName).append(" xmlns:ns=\"").append(targetNs).append("\">");
        // params.size() co the nho hon values.size(): tham so cuoi la List<String>
        // nen trong schema chi khai bao 1 lan (maxOccurs unbounded) nhung phai
        // lap lai the do cho tung phan tu.
        for (int i = 0; i < values.size(); i++) {
            String tag = params.get(Math.min(i, params.size() - 1));
            body.append(openTag(tag)).append(esc(values.get(i))).append(closeTag(tag));
        }
        body.append("</ns:").append(opName).append(">");

        String envelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"" + ENV + "\">"
                + "<soapenv:Body>" + body + "</soapenv:Body>"
                + "</soapenv:Envelope>";

        byte[] payload = envelope.getBytes("UTF-8");
        HttpURLConnection c = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        // SOAP 1.1 bat buoc co header SOAPAction (de rong la duoc voi JAX-WS)
        c.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        c.setRequestProperty("SOAPAction", "\"\"");
        c.setRequestProperty("Content-Length", String.valueOf(payload.length));
        try (OutputStream os = c.getOutputStream()) {
            os.write(payload);
        }

        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        Document res = parse(in);
        c.disconnect();

        // Bao loi ro rang neu server tra SOAP Fault
        NodeList faults = res.getElementsByTagNameNS(ENV, "Fault");
        if (faults.getLength() > 0) {
            StringBuilder sb = new StringBuilder("SOAP Fault: ");
            NodeList fs = ((Element) faults.item(0)).getElementsByTagName("*");
            for (int i = 0; i < fs.getLength(); i++) {
                Element f = (Element) fs.item(i);
                if ("faultstring".equals(f.getLocalName()) || "faultcode".equals(f.getLocalName())) {
                    sb.append(f.getLocalName()).append("=").append(f.getTextContent()).append(" ");
                }
            }
            throw new IOException(sb.toString().trim() + " (HTTP " + code + ")");
        }
        if (code >= 400) throw new IOException("HTTP " + code + " khi goi " + opName);
        return res;
    }

    public static void main(String[] args) throws Exception {
        // Doc WSDL de lay targetNamespace + ten tham so that
        Document wsdl;
        try (InputStream in = new URL(WSDL).openStream()) {
            wsdl = parse(in);
        }
        Element defs = wsdl.getDocumentElement();
        targetNs = defs.getAttribute("targetNamespace");
        System.out.println("targetNamespace = " + targetNs);

        List<String> getParams = paramNames(wsdl, "getData");
        List<String> subParams = paramNames(wsdl, "submitDataStringArray");
        // Fallback: JAX-WS mac dinh dat ten tham so la arg0, arg1, ...
        if (getParams == null) getParams = Arrays.asList("arg0", "arg1");
        if (subParams == null) subParams = Arrays.asList("arg0", "arg1", "arg2");
        System.out.println("getData params = " + getParams
                + " | submit params = " + subParams + " | qualified = " + qualified);

        // b. getData(studentCode, qCode) -> List<Integer>
        Document r1 = call("getData", getParams, Arrays.asList(MSV, QCODE));
        // JAX-WS dat ten the ket qua la <return>, co the lap lai nhieu lan
        List<Integer> data = new ArrayList<>();
        NodeList all = r1.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element e = (Element) all.item(i);
            if (!"return".equals(e.getLocalName())) continue;
            String t = e.getTextContent().trim();
            if (!t.isEmpty()) data.add(Integer.parseInt(t));
        }
        System.out.println("Nhan " + data.size() + " so: " + data);
        if (data.isEmpty()) throw new IllegalStateException("Khong doc duoc so nao tu phan hoi");

        // c. dem chan / le
        int even = 0, odd = 0;
        for (int v : data) {
            // dung v % 2 != 0 chu khong phai == 1, de so am (-3) van tinh la le
            if (v % 2 == 0) even++;
            else odd++;
        }
        List<String> answer = Arrays.asList("EVEN=" + even, "ODD=" + odd);
        System.out.println("Nop: " + answer);

        // d. submitDataStringArray(studentCode, qCode, data)
        List<String> values = new ArrayList<>();
        values.add(MSV);
        values.add(QCODE);
        values.addAll(answer);
        call("submitDataStringArray", subParams, values);
        System.out.println("Da nop xong");

        // e. ket thuc client
    }
}
