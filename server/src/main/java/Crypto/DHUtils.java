package Crypto;

import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class DHUtils {

    private KeyPair serverKpair;
    private KeyAgreement serverKeyAgree;
    private Cipher serverCipher;
    private SecretKeySpec serverAesKey;

    public DHUtils() throws InvalidKeyException, NoSuchAlgorithmException {
        /*
         * server creates her own DH key pair with 2048-bit key size
         */
        System.out.println("SERVER : Generate DH keypair ...");
        KeyPairGenerator serverKpairGen = KeyPairGenerator.getInstance("EC");
        serverKpairGen.initialize(256);
        serverKpair = serverKpairGen.generateKeyPair();

        // server creates and initializes her DH KeyAgreement object
        System.out.println("SERVER : Initialization ...");
        serverKeyAgree = KeyAgreement.getInstance("ECDH");
        serverKeyAgree.init(serverKpair.getPrivate());
    }

    public byte[] generateServerPublicKey() {
        // server encodes her public key
        byte[] serverPubKeyEnc = serverKpair.getPublic().getEncoded();
        return serverPubKeyEnc;
    }


    public void initPhase1(byte[] clientPubKeyEnc) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        /*
         * server uses client's public key for the first (and only) phase
         * of her version of the DH
         * protocol.
         * Before she can do so, she has to instantiate a DH public key
         * from client's encoded key material.
         */
        KeyFactory serverKeyFac = KeyFactory.getInstance("EC");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(clientPubKeyEnc);
        PublicKey clientPubKey = serverKeyFac.generatePublic(x509KeySpec);
        System.out.println("SERVER : Execute PHASE1 ...");
        serverKeyAgree.doPhase(clientPubKey, true);
    }

    public byte[] generateSharedSecret(){
        /*
         * At this stage, both server and client have completed the DH key
         * agreement protocol.
         * Both generate the (same) shared secret.
         */
        byte[] sharedSecret = serverKeyAgree.generateSecret();
        serverAesKey = new SecretKeySpec(sharedSecret, "AES");
        return sharedSecret;
    }

    public byte[] encript( int msgi) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {

        /*
         * client encrypts, using AES in CBC mode
         */
        if (serverCipher == null) {
            serverCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        }

        byte[] iv = new byte[16];
        // rnd.nextBytes(iv);
        IvParameterSpec ivParams = new IvParameterSpec(iv);

        String msg = ""+msgi;
        serverCipher.init(Cipher.ENCRYPT_MODE, serverAesKey,ivParams);
        byte[] cleartext = msg.getBytes();
        return serverCipher.doFinal(cleartext);
    }

    public byte[] decript(byte[] ciphertext) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        /*
         * server decrypts, using AES in CBC mode
         */

        // Instantiate AlgorithmParameters object from parameter encoding
        // obtained from client
        if (serverCipher == null) {
            serverCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        }

        byte[] iv = new byte[16];
        // rnd.nextBytes(iv);
        IvParameterSpec ivParams = new IvParameterSpec(iv);

        serverCipher.init(Cipher.DECRYPT_MODE, serverAesKey,ivParams);
        return serverCipher.doFinal(ciphertext);
    }



    /*
     * Converts a byte to hex digit and writes to the supplied buffer
     */
    private static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    /*
     * Converts a byte array to hex string
     */
    public static String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
            if (i < len-1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }


}
