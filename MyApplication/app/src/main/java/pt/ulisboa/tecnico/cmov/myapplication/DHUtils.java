package pt.ulisboa.tecnico.cmov.myapplication;

import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class DHUtils {

    private KeyPair clientKpair;
    private KeyAgreement clientKeyAgree;
    private Cipher clientCipher;
    private SecretKeySpec clientAesKey;

    public DHUtils() throws InvalidKeyException, NoSuchAlgorithmException {
        /*
         * client creates her own DH key pair with 2048-bit key size
         */
        System.out.println("CLIENT: Generate DH keypair ...");
        KeyPairGenerator clientKpairGen = KeyPairGenerator.getInstance("EC");
        clientKpairGen.initialize(256);
        clientKpair = clientKpairGen.generateKeyPair();

        // client creates and initializes her DH KeyAgreement object
        System.out.println("CLIENT: Initialization ...");
        clientKeyAgree = KeyAgreement.getInstance("ECDH");
        clientKeyAgree.init(clientKpair.getPrivate());
    }

    public byte[] generateServerPublicKey() {
        // client encodes her public key
        byte[] clientPubKeyEnc = clientKpair.getPublic().getEncoded();
        return clientPubKeyEnc;
    }


    public void initPhase1(byte[] serverPubKeyEnc) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        /*
         * client uses server's public key for the first (and only) phase
         * of her version of the DH
         * protocol.
         * Before she can do so, she has to instantiate a DH public key
         * from server's encoded key material.
         */
        KeyFactory clientKeyFac = KeyFactory.getInstance("EC");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(serverPubKeyEnc);
        PublicKey serverPubKey = clientKeyFac.generatePublic(x509KeySpec);
        System.out.println("CLIENT : Execute PHASE1 ...");
        clientKeyAgree.doPhase(serverPubKey, true);
    }

    private byte[] sharedSecret = null;

    public byte[] generateSharedSecret(){
        /*
         * At this stage, both client and server have completed the DH key
         * agreement protocol.
         * Both generate the (same) shared secret.
         */
        sharedSecret = clientKeyAgree.generateSecret();
        clientAesKey = new SecretKeySpec(sharedSecret, "AES");
        return sharedSecret;
    }

    public byte[] getSharedSecret(){
        return sharedSecret;
    }

    public byte[] encript(int msgi) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {

        /*
         * server encrypts, using AES in CBC mode
         */
        if (clientCipher == null) {
            clientCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        }

        byte[] iv = new byte[16];
        // rnd.nextBytes(iv);
        IvParameterSpec ivParams = new IvParameterSpec(iv);

        String msg = "" + msgi;
        clientCipher.init(Cipher.ENCRYPT_MODE, clientAesKey, ivParams);
        byte[] cleartext = msg.getBytes();
        return clientCipher.doFinal(cleartext);
    }

    private IvParameterSpec getIvParameterSpec() {
        SecureRandom rnd = new SecureRandom();
        byte[] iv = new byte[clientCipher.getBlockSize()];
        // rnd.nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    public byte[] decript(byte[] ciphertext) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        /*
         * client decrypts, using AES in CBC mode
         */



        // Instantiate AlgorithmParameters object from parameter encoding
        // obtained from server
        if (clientCipher == null) {
            clientCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        }

        byte[] iv = new byte[16];
        // rnd.nextBytes(iv);
        IvParameterSpec ivParams = new IvParameterSpec(iv);

        clientCipher.init(Cipher.DECRYPT_MODE, clientAesKey, ivParams);
        return clientCipher.doFinal(ciphertext);
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
