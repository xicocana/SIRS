package meic.sirs.drivekeeper;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;

public class UserOptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_options);

        Button access_device = (Button) findViewById(R.id.access_drive_btn);
        access_device.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ServerConnect().execute();
            }
        });
    }
}

class ServerConnect extends AsyncTask<String, Void, Void> {

    private Exception exception;

    protected Void doInBackground(String... urls) {
        try {
            String request;
            String response;
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            InetAddress addr = InetAddress.getByName("192.168.43.80");
            Socket clientSocket = new Socket(addr, 9090);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            request = "accessDrive()";
            outToServer.writeBytes(request + '\n');
            response = inFromServer.readLine();
            System.out.println(response);
            clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        return null;
    }
}
