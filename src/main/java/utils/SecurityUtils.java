package utils;



import java.util.Base64;

public class SecurityUtils {

    public static String encPass64(String pass){
        return Base64.getEncoder().encodeToString(pass.getBytes());
    }

    public static String encByte64(byte[] data){ return Base64.getEncoder().encodeToString(data);}

    public static String decPass64(String pass){ return new String(Base64.getDecoder().decode(pass)); }

    public static String encPass228(String pass){ return Encode228(pass); }

    public static String decPass228(String pass){ return Decode228(pass); }

    private static String Encode228(String str) {
        StringBuilder result = new StringBuilder(str);
        StringBuilder tmpsB = new StringBuilder();

        int i, j;
        for (i = 1; i < result.length(); i++){

            for(j = 0;j < result.length();j=j+i+1){
                if(result.length() > j+i+1) {
                    tmpsB.append(new StringBuilder(result.substring(j, i + j + 1)).reverse());
                }else {
                    tmpsB.append(new StringBuilder(result.substring(j, j + (result.length() - j))).reverse());
                }
            }
            result.delete(0,result.length());
            result.append(tmpsB);
            tmpsB.delete(0,tmpsB.length());
        }
        return result.toString();
    }

    private static String Decode228(String str) {
        StringBuilder result = new StringBuilder(str);
        StringBuilder tmpsB = new StringBuilder();

        int i, j;
        for (i=result.length()-1;i > 0;i--){

            for(j = 0;j < result.length();j=j+i+1){
                if(result.length() > j+i+1) {
                    tmpsB.append(new StringBuilder(result.substring(j, i + j + 1)).reverse());
                }else {
                    tmpsB.append(new StringBuilder(result.substring(j, j + (result.length() - j))).reverse());
                }
            }
            result.delete(0,result.length());
            result.append(tmpsB);
            tmpsB.delete(0,tmpsB.length());
        }
        return result.toString();
    }
}
