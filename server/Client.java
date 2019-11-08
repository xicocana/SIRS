import java.io.*;
import java.net.*;

public class Client {
    public static void main(String argv[]) throws Exception {
        String request;
        String response;
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        Socket clientSocket = new Socket("localhost", 9090);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        request = inFromUser.readLine();
        outToServer.writeBytes(request + '\n');
        response = inFromServer.readLine();
        System.out.println(response);
        clientSocket.close();
    }
}