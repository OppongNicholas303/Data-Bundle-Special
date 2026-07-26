import java.net.URL;
import java.net.HttpURLConnection;

public class Test {
    public static void main(String[] args) {
        try {
            URL url = new URL("https://api.paystack.co/transaction/initialize");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            System.out.println("Response code: " + con.getResponseCode());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
