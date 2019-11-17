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
        System.out.println("SERVER: Generate DH keypair ...");
        KeyPairGenerator serverKpairGen = KeyPairGenerator.getInstance("DH");
        serverKpairGen.initialize(2048);
        serverKpair = serverKpairGen.generateKeyPair();

        // server creates and initializes her DH KeyAgreement object
        System.out.println("SERVER: Initialization ...");
        serverKeyAgree = KeyAgreement.getInstance("DH");
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
        KeyFactory serverKeyFac = KeyFactory.getInstance("DH");
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
        serverAesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");
        return sharedSecret;
    }

    public byte[] encript(byte[] sharedSecret) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {

        /*
         * client encrypts, using AES in CBC mode
         */
        if (serverCipher == null) {
            serverCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        }

        IvParameterSpec ivParams = getIvParameterSpec();

        serverCipher.init(Cipher.ENCRYPT_MODE, serverAesKey,ivParams);
        byte[] cleartext = "This is just an example".getBytes();
        return serverCipher.doFinal(cleartext);
    }

    private IvParameterSpec getIvParameterSpec() {
        SecureRandom rnd = new SecureRandom();
        byte[] iv = new byte[serverCipher.getBlockSize()];
        // rnd.nextBytes(iv);
        return new IvParameterSpec(iv);
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

        IvParameterSpec ivParams = getIvParameterSpec();

        serverCipher.init(Cipher.DECRYPT_MODE, serverAesKey,ivParams);
        return serverCipher.doFinal(ciphertext);
    }


}
