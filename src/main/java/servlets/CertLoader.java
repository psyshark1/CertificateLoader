package servlets;

import DataBase.Dbconn;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.Props;
import utils.SecurityUtils;
;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(name = "CertLoader",urlPatterns = "/v1/api")
@MultipartConfig
public class CertLoader extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        //String description = request.getParameter("description"); // Retrieves <input type="text" name="description">
        //Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">

        Boolean badIP = (Boolean) request.getAttribute("badIP");
        assert badIP != null;

        if (!badIP){
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            WriteMsgResponse(response, GetJSONString(GetJSON("Error", "Description", "Неверный IP")));
            return;
        }

        List<JSONObject> respData = new ArrayList<>();

        try {

            for (Part filePart : request.getParts()) {

                String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // MSIE fix.
                InputStream fileContent = filePart.getInputStream();

                Props props = Props.getInstance();
                File certFile = new File(props.getTempFolder() + fileName);
                saveCert(fileContent, certFile);

                if (certFile.exists()) {
                    String contentCert = readCert(certFile.getAbsolutePath());

                    FileInputStream bincontentCert = new FileInputStream(certFile.getAbsolutePath());
                    if (!contentCert.equals("Некорректный пароль")) {

                        Map<String, String> certContent = new HashMap<>();

                        certContent.put("certSerial", getCertContentData("Серийный номер:\\s([\\S\\s]+?)\\n", contentCert, false, false));
                        certContent.put("certFrom", getCertContentData("NotBefore:\\s([\\S\\s]+?)\\n", contentCert, true, false));
                        certContent.put("certTo", getCertContentData("NotAfter:\\s([\\S\\s]+?)\\n", contentCert, true, false));
                        certContent.put("certOrg", getCertContentData("Субъект:[\\S\\s]*?O=([\\S\\s]+?)\\,", contentCert, false, false));
                        certContent.put("certSubject", getCertContentData("Субъект:[\\S\\s]*?SN=([\\S\\s]+?)\\,", contentCert, false, false) + " " +
                                getCertContentData("Субъект:[\\S\\s]*?G=([\\S\\s]+?)\\,", contentCert, false, false));
                        certContent.put("certOgrn", getCertContentData("Субъект:[\\S\\s]*?OGRN=([\\S\\s]+?)\\,", contentCert, false, false));
                        certContent.put("certInn", getCertContentData("OID.[\\S\\s]*?=([\\S\\s]+?)-", contentCert, false, false));
                        certContent.put("certKpp", getCertContentData("OID.[\\S\\s]*?-([\\S\\s]+?)-", contentCert, false, false));
                        certContent.put("certThumbprint", getCertContentData("\\(sha1\\):\\s([\\S\\s]+?)\\n", contentCert, false, true));

                        try {
                            Dbconn dbf = Dbconn.getInstance();
                            dbf.InsertCert(
                                    encodeCert(bincontentCert),
                                    certContent.get("certSerial"),
                                    certContent.get("certFrom"),
                                    certContent.get("certTo"),
                                    certContent.get("certOrg"),
                                    certContent.get("certSubject"),
                                    certContent.get("certThumbprint"),
                                    certContent.get("certInn"),
                                    certContent.get("certKpp"),
                                    certContent.get("certOgrn"));
                            bincontentCert.close();
                        } catch (SQLException | ClassNotFoundException | IOException throwables) {
                            bincontentCert.close();
                            Files.delete(Paths.get(certFile.getAbsolutePath()));
                            respData.add(GetJSON("SQL Error", "Description", throwables.toString()));
                            //WriteMsgResponse(response, GetJSONString(err));
                            continue;
                        }

                        Files.delete(Paths.get(certFile.getAbsolutePath()));
                        respData.add(GetJSON("OK", "SerialNumber",certContent.get("certSerial")));
                        continue;
                    }

                    bincontentCert.close();
                    Files.delete(Paths.get(certFile.getAbsolutePath()));
                    respData.add(GetJSON("Error", "Description","Некорректный пароль сертификата " + fileName));
                    continue;
                }
                respData.add(GetJSON("Error", "Description","Ошибка сохранения файла на сервере: " + fileName));

            }
        } catch (ServletException se) {
            respData.add(new JSONObject().put("Error", se.toString()));
            WriteMsgResponse(response, GetStringFromJSONList(respData));
            return;
        }
        WriteMsgResponse(response, GetStringFromJSONList(respData));
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        WriteMsgResponse(response, GetJSONString(GetJSON("Error","Description", "Возможны только POST запросы")));
        //RequestDispatcher reqD = request.getRequestDispatcher("/pages/load.jsp");
        //reqD.forward(request, response);
    }

    private void saveCert (InputStream in, File certFile) throws IOException {
        FileOutputStream out = new FileOutputStream(certFile);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.close();
    }

    private String readCert(String certPath) throws IOException {
        Process process = Runtime.getRuntime().exec("certutil -p \"1234567890\" -dump \"" + certPath + "\"");
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), "windows-1251"));
        StringBuilder tmpsB = new StringBuilder();
        while (true) {
            String line = br.readLine();
            if (line == null) { break; }
            tmpsB.append(line).append("\n");
        }
        if (tmpsB.indexOf("ERROR_INVALID_PASSWORD") > -1){
            return "Некорректный пароль";
        }

        return tmpsB.substring(tmpsB.lastIndexOf("Серийный"), tmpsB.lastIndexOf("--"));
        //return tmpsB.toString();
    }

    private String getCertContentData(String regex, String text, boolean isDate, boolean spaceReplace) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        String res = null;
        if (matcher.find()) {
            res = matcher.group(1);
        }
        if (res == null){
            return "";
        }
        if (isDate) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.yyyy H:mm", Locale.ENGLISH);
            LocalDateTime datt = LocalDateTime.parse(res, formatter);
            return datt.format(DateTimeFormatter.ofPattern("yyyy-M-d H:mm", Locale.ENGLISH));
        }
        if (spaceReplace){
            return res.replaceAll("\\s","");
        }
        return res;
    }

    private String encodeCert(InputStream is) throws IOException {
        ByteArrayOutputStream bys = new ByteArrayOutputStream();
        byte[] arr = new byte[1024];
        int read;
        while ((read = is.read(arr)) != -1) {
            bys.write(arr, 0, read);
        }

        String certBase64 = SecurityUtils.encByte64(bys.toByteArray());
        bys.flush();
        bys.close();
        return SecurityUtils.encPass228(certBase64);

    }

    private String GetJSONString(JSONObject data) {
        JSONArray ja = new JSONArray();
        ja.put(data);
        return ja.toString();
    }

    private JSONObject GetJSON(String status, String infoName, String descr) {
        JSONObject jso = new JSONObject();
        jso.put("Status",status);
        jso.put(infoName,descr);
        return jso;
    }

    private String GetStringFromJSONList(List<JSONObject> data) {
        JSONArray ja = new JSONArray();
        for (JSONObject jso : data){
            ja.put(jso);
        }
        return ja.toString();
    }


    private void WriteMsgResponse(HttpServletResponse response, String text) throws IOException {
        response.setContentType("application/json;charset=windows-1251");
        //response.setHeader("Access-Control-Allow-Origin", request.getScheme() + "://"+ request.getServerName() + ":" + request.getServerPort());
        PrintWriter writer = response.getWriter();
        //byte[] bytes = tmpsB.toString().getBytes();
        //new String(bytes, "windows-1251");
        try {
            writer.print(text);
        } finally {
            writer.flush();
            writer.close();
        }
    }
}
