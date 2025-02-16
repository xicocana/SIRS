package server;


import Crypto.DHUtils;
import Crypto.EncrDecrFilesUtil;
import Crypto.RSAGenerator;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import java.awt.*;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import java.io.*;
import org.apache.commons.io.FileUtils;

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
    private static final int ENCRYPT = 1;
    private static final int DECRYPT = 2;

    ProcessConnectionThread(StreamConnection connection) {
        mConnection = connection;
    }

    public void run() {
        try {
            OutputStream outputStream = mConnection.openOutputStream();
            InputStream inputStream = mConnection.openInputStream();

            DHUtils dhUtils = new DHUtils();
            byte[] buffer;
            RSAGenerator rsaGenerator = new RSAGenerator();
            boolean v;

            //PREPARE DH
            // Server encodes its public key, and sends it over
            byte[] serverPubKeyEnc = dhUtils.generateServerPublicKey();

            Optional<byte[]> signedServerY = rsaGenerator.generateSign(serverPubKeyEnc,"server.key");

            try{
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                if (signedServerY.isPresent()){
                    List<byte[]> listBytesY = new ArrayList<>(Arrays.asList(serverPubKeyEnc));
                    listBytesY.addAll(Arrays.asList(signedServerY.get()));
                    objectOutputStream.writeObject(listBytesY);
                    //Server receives YB
                    ArrayList<byte[]> listServerY = (ArrayList<byte[]>) objectInputStream.readObject();
                    v = rsaGenerator.validateSign(listServerY.get(0),listServerY.get(1),"client.pub");
                    if (!v){
                        System.out.println("Error validating key");
                        return ;
                    }
                    dhUtils.initPhase1(listServerY.get(0));
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            //Generate Shared Secret
            System.out.println("Session key Created");
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


            final String[] arraymsgPass = receiveValuesFromClient(inputStream, dhUtils);
            final String pass = arraymsgPass[0];

            // prepare to receive data
            System.out.println("SERVER : Waiting for input ...");
            while (true) {
                final String[] arraymsg = receiveValuesFromClient(inputStream, dhUtils);

                int command = Integer.parseInt(arraymsg[0]);

                boolean errorSession ;
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
                    processCommand(command,pass);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] receiveValuesFromClient(InputStream inputStream, DHUtils dhUtils) throws IOException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        byte[] buffer;
        buffer = new byte[1024];
        int numberOfB = inputStream.read(buffer);
        byte[] result = Arrays.copyOfRange(buffer, 0, numberOfB);
        String msg = new String(dhUtils.decript(result));
        return msg.split(":");
    }

    /**
     * Process the command from client
     *
     * @param command the command code
     */

    //bora usar folder, /tmp/DriveKeeper/
    //verificar sempre se pasta

    private void processCommand(int command,String pass) {
        try {
            //Robot robot = new Robot();
            String username = System.getProperty("user.name");
            String dir_pen = "/media/"+username+"/DriveKeeper/SecretFiles";
            String dir = "/tmp/DriveKeeper";

            File destDir ;
            File srcDir ;

            switch (command) {
                case ENCRYPT:
                    //robot.keyPress(KeyEvent.VK_RIGHT);
                    //robot.keyRelease(KeyEvent.VK_RIGHT);

                    //encriptar ficheiros da pasta local
                    EncrDecrFilesUtil.doSomething(dir, EncrDecrFilesUtil.ENCRYPT, pass);
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
                case DECRYPT:
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