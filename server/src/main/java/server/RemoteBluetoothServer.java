package server;


import Crypto.DHUtils;
import Crypto.EncrDecrFilesUtil;
import Crypto.RSAGenerator;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import java.awt.*;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import java.io.*;
import org.apache.commons.io.FileUtils;
	
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class RemoteBluetoothServer {

    public static void main(String[] args) {
        Thread waitThread = new Thread(new WaitThread());
        waitThread.start();
    }
}

class WaitThread implements Runnable {

    /**
     * Constructor
     */
    WaitThread() {
    }

    public void run() {
        waitForConnection();
    }

    /**
     * Waiting for connection from devices
     */
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
        System.out.println("SERVER : Waiting for connection...");

        while (true) {
            try {
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

    ProcessConnectionThread(StreamConnection connection) {
        mConnection = connection;
    }

    public void run() {
        try {

            OutputStream outputStream = mConnection.openOutputStream();

            InputStream inputStream = mConnection.openInputStream();
            DHUtils dhUtils = new DHUtils();
            byte[] buffer = new byte[1024];
            RSAGenerator rsaGenerator = new RSAGenerator();

            //PREPARE DH
            // Server encodes its public key, and sends it over
            byte[] serverPubKeyEnc = dhUtils.generateServerPublicKey();
            if(false){//TODO Alterar só enquanto nao funciona

                Optional<byte[]> signedServerY = rsaGenerator.generateSign(serverPubKeyEnc,"server.key");
                if (signedServerY.isPresent()){
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                    List<byte[]> listBytesY = new ArrayList<>(Arrays.asList(serverPubKeyEnc));
                    listBytesY.addAll(Arrays.asList(signedServerY.get()));
                    objectOutputStream.writeObject(listBytesY);
                }else {
                    throw new Exception("Error signing message");
                }
            }else{
                outputStream.write(serverPubKeyEnc);
            }


            //Server receives YB
            inputStream.read(buffer);
            dhUtils.initPhase1(buffer);

            //Generate Shared Secret
            dhUtils.generateSharedSecret();

            //new session starts here
            String dir_sesh = System.getProperty("user.dir") + "/session.txt" ;
            File file = new File(dir_sesh);
            //Create the file
            if (file.createNewFile()) {
                PrintWriter writer = new PrintWriter(file);
                writer.print("0");
                writer.close();
                System.out.println("Session file is created!");
            } else {
                System.out.println("Session file already exists. Deleting content");
                PrintWriter writer = new PrintWriter(file);
                writer.print("0");
                writer.close();
            }

            // prepare to receive data
            System.out.println("SERVER : Waiting for input ...");
            while (true) {
                buffer = new byte[1024];
                int numberOfB = inputStream.read(buffer);
                byte[] result = Arrays.copyOfRange(buffer, 0, numberOfB);
                String msg = new String(dhUtils.decript(result));
                String[] arraymsg = msg.split(":");

                int command = Integer.parseInt(arraymsg[0]);

                boolean errorSession = false;
                try (Stream<String> stream = Files.lines(Paths.get(dir_sesh))) {
                    errorSession = stream.map(Integer::valueOf).allMatch(x -> x.compareTo(Integer.parseInt(arraymsg[1])) > 0);
                }

                if(!errorSession){
                    PrintWriter writer = new PrintWriter(file);
                    writer.print(arraymsg[1]);
                    writer.close();
                    if (command == EXIT_CMD) {
                        System.out.println("SERVER : Finish process");
                        break;
                    }
                    processCommand(command);
                }
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

    //bora usar folder, /tmp/DriveKeeper/
    //verificar sempre se pasta ja exist e o crlh

    private void processCommand(int command) {
        try {
            //Robot robot = new Robot();
            String username = System.getProperty("user.name");
            String dir_pen = "/media/"+username+"/DriveKeeper/SecretFiles";
            String dir = "/tmp/DriveKeeper";

            File destDir = null;
            File srcDir = null;

            switch (command) {
                case KEY_RIGHT:
                    //robot.keyPress(KeyEvent.VK_RIGHT);
                    //robot.keyRelease(KeyEvent.VK_RIGHT);

                    //encriptar ficheiros da pasta local
                    EncrDecrFilesUtil.doSomething(dir, EncrDecrFilesUtil.ENCRYPT, "1234567891111111");
                    //copiar encryptados pa pen
                    destDir = new File(dir_pen);
                    srcDir = new File(dir);
                    try {
                        FileUtils.copyDirectory(srcDir, destDir);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //delete folder
                    FileUtils.deleteDirectory(srcDir);
                
                    break;
                case KEY_LEFT:
                    //robot.keyPress(KeyEvent.VK_LEFT);
                    //robot.keyRelease(KeyEvent.VK_LEFT);

                    //criar pasta no pc local
                    File file = new File(dir);
                    if (!file.exists()) {
                        if (file.mkdir()) {
                            System.out.println("Temporary directory created");
                        } else {
                            System.out.println("Failed to create directory");
                        }
                    }
                    //copiar ficheiros pra la
                    srcDir = new File(dir_pen);
                    destDir = new File(dir);
                    try {
                        FileUtils.copyDirectory(srcDir, destDir);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //desencriptar
                    EncrDecrFilesUtil.doSomething(dir, EncrDecrFilesUtil.DECRYPT, "1234567891111111");
                    //abrir pasta no explorer
                    Desktop.getDesktop().open(new File(dir));
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}