package server;




import Crypto.DHUtils;
import Crypto.Crypto;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class RemoteBluetoothServer{

    public static void main(String[] args) {
        Thread waitThread = new Thread(new WaitThread());
        waitThread.start();
    }
}

class WaitThread implements Runnable {

    /** Constructor */
    public WaitThread() {
    }

    public void run() {
        waitForConnection();
    }

    /** Waiting for connection from devices */
    private void waitForConnection() {
        // retrieve the local Bluetooth device object
        LocalDevice local;

        StreamConnectionNotifier notifier;
        StreamConnection connection;

        // setup the server to listen for connection
        try {
            local = LocalDevice.getLocalDevice();
            local.setDiscoverable(DiscoveryAgent.GIAC);

            UUID uuid = new UUID(80087355); // "04c6093b-0000-1000-8000-00805f9b34fb"
            String url = "btspp://localhost:" + uuid.toString() + ";name=RemoteBluetooth";
            notifier = (StreamConnectionNotifier) Connector.open(url);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        // waiting for connection
        while(true) {
            try {
                System.out.println("waiting for connection...");
                connection = notifier.acceptAndOpen();

                Thread processThread = new Thread(new ProcessConnectionThread(connection));
                processThread.start();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }
}

class ProcessConnectionThread implements Runnable {

    private StreamConnection mConnection;

    // Constant that indicate command from devices
    private static final int EXIT_CMD = -1;
    private static final int KEY_RIGHT = 1;
    private static final int KEY_LEFT = 2;

    public ProcessConnectionThread(StreamConnection connection) {
        mConnection = connection;
    }

    public void run() {
        try {

            //PREPARE DH
            DHUtils dhUtils = new DHUtils();
            OutputStream outputStream =  mConnection.openOutputStream();
            InputStream inputStream = mConnection.openInputStream();

            byte[] buffer = new byte[1024];

            // Server encodes her public key, and sends it over
            byte[] serverPubKeyEnc =  dhUtils.generateServerPublicKey();
            outputStream.write(serverPubKeyEnc);

            //Server receives YB
            inputStream.read(buffer);
            dhUtils.initPhase1(buffer);

            //Generate Shared Secret
            dhUtils.generateSharedSecret();

            // prepare to receive data
            System.out.println("waiting for input ...");
            while (true) {
                buffer = new byte[1024];
                int numberOfB = inputStream.read(buffer);
                byte[] result = Arrays.copyOfRange(buffer,0,numberOfB);
                String msg = new String(dhUtils.decript(result));

                int command = Integer.parseInt(msg);

                if (command == EXIT_CMD) {
                    System.out.println("finish process");
                    break;
                }
                processCommand(command);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Process the command from client
     *
     * @param command the command code
     */
    private void processCommand(int command) {
        try {
            Robot robot = new Robot();
            switch (command) {
                case KEY_RIGHT:
                    robot.keyPress(KeyEvent.VK_RIGHT);
                    robot.keyRelease(KeyEvent.VK_RIGHT);

                    Crypto.doSomething("FolderToEncrypt", Crypto.ENCRYPT,"1234567891111111");

                    System.out.println("Right");
                    break;
                case KEY_LEFT:
                    robot.keyPress(KeyEvent.VK_LEFT);
                    robot.keyRelease(KeyEvent.VK_LEFT);

                    Crypto.doSomething("FolderToEncrypt", Crypto.DECRYPT,"1234567891111111");

                    System.out.println("Left");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}