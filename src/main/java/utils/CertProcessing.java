package utils;

import DataBase.Dbconn;

import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CertProcessing {

    private final Props props;
    private Part FilePart;
    private File CertFile;
    private WeakHashMap<String, String> certContent;
    private final Dbconn dbf;

    public CertProcessing() throws IOException, ClassNotFoundException {
        props = Props.getInstance();
        dbf = Dbconn.getInstance();
    }

    public void setFilePart(Part filePart){
        FilePart = filePart;
    }

    public String getFileName() {
        if (CertFile == null) {
            return "Сертификат не сохранен";
        }
        return CertFile.getName();
    }

    public void deleteCertFile() throws IOException {
        Files.delete(Paths.get(CertFile.getAbsolutePath()));
        ReferenceQueue<File> queue = new ReferenceQueue<>();
        WeakReference<File> weakRef = new WeakReference<>(CertFile, queue);
        CertFile = null;
        FilePart = null;
        weakRef.clear();
    }

    public void saveCert () throws IOException {
        String fileName = Paths.get(FilePart.getSubmittedFileName()).getFileName().toString();
        InputStream fileContent = FilePart.getInputStream();

        CertFile = new File(props.getTempFolder() + fileName);
        FileOutputStream out = new FileOutputStream(CertFile);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = fileContent.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.close();
    }

    public int setCertContent() throws IOException {
        if (CertFile.exists()) {
            String contentCert = readCert(CertFile.getAbsolutePath());

            if (!contentCert.equals("Некорректный пароль")) {
                certContent = new WeakHashMap<>();
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
                return 1;
            }
            return -1;
        }
        return -2;
    }

    public void storeCertContent() throws IOException, SQLException {

        try (FileInputStream bincontentCert = new FileInputStream(CertFile.getAbsolutePath())) {
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
        }

    }
    
    public String getCertAttribute (String AtrName){
        if (certContent.containsKey(AtrName)) {
            return certContent.get(AtrName);
        }
        return "Атрибут " + AtrName + " отсутствует";
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
        if (tmpsB.indexOf("ERROR_INVALID_PASSWORD") > -1) {
            return "Некорректный пароль";
        }

        return tmpsB.substring(tmpsB.lastIndexOf("Серийный"), tmpsB.lastIndexOf("--"));

    }

    private String getCertContentData(String regex, String text, boolean isDate, boolean spaceReplace) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        String res = null;
        if (matcher.find()) {
            res = matcher.group(1);
        }
        if (res == null) {
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

}
