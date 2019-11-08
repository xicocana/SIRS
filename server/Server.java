import java.io.*;
import java.net.*;

public class Server {
    public static void main(String args[]) throws Exception {

		System.out.println("Started DriveKeeper server");
		System.out.println("Waiting for client connection");

        String clientRequest;
        ServerSocket welcomeSocket = new ServerSocket(9090);

        while(true) {
			Socket connectionSocket = welcomeSocket.accept();
			System.out.println("Client connected");
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			clientRequest = inFromClient.readLine();
			ExecuteRequest(clientRequest , outToClient);
        }
	}
	
	public static void ExecuteRequest(String command, DataOutputStream client){
		System.out.println("Executing command : " + command);
		try{
			client.writeBytes("Executed command " + command + " successfully \n");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}