package servlets;

import org.json.JSONArray;
import org.json.JSONObject;
import utils.CertProcessing;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "CertLoader",urlPatterns = "/v1/api")
@MultipartConfig
public class CertLoader extends HttpServlet {

    public CertProcessing certProc;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        //String description = request.getParameter("description"); // Retrieves <input type="text" name="description">
        //Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">

        Boolean badIP = (Boolean) request.getAttribute("badIP");
        assert badIP != null;

        if (badIP){
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            WriteMsgResponse(response, GetJSONString(GetJSON("Error", "Description", "Неверный IP")));
            return;
        }

        List<JSONObject> respData = new ArrayList<>();
        try {
            certProc = new CertProcessing();
        } catch (ClassNotFoundException e) {
            respData.add(new JSONObject().put("Error", e.toString()));
            WriteMsgResponse(response, GetStringFromJSONList(respData));
            return;
        }


        for (Part filePart : request.getParts()) {

            try {

                certProc.setFilePart(filePart);
                certProc.saveCert();
                int res = certProc.setCertContent();
                switch (res){
                    case -1:
                        respData.add(GetJSON("Error", "Description","Некорректный пароль сертификата " + certProc.getFileName()));
                        break;
                    case -2:
                        respData.add(GetJSON("Error", "Description","Ошибка сохранения файла на сервере: " + certProc.getFileName()));
                        break;
                    case 1:
                        certProc.storeCertContent();
                        respData.add(GetJSON("OK", "SerialNumber",certProc.getCertAttribute("certSerial")));
                }
            } catch (SQLException | IOException e) {
                respData.add(GetJSON("Error", "Description", e.toString()));
            } catch (NullPointerException e) {
                StringWriter err = new StringWriter();
                e.printStackTrace(new PrintWriter(err));
                respData.add(GetJSON("Error", "Description", err.toString()));
            } finally {
                try {
                    certProc.deleteCertFile();
                } catch (IOException se) {
                    respData.add(GetJSON("Error", "Description", se.toString()));
                }
            }
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
