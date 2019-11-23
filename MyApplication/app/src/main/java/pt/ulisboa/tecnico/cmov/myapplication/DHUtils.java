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
        KeyPairGenerator clientKpairGen = KeyPairGenerator.getInstance("DH");
        clientKpairGen.initialize(2048);
        clientKpair = clientKpairGen.generateKeyPair();

        // client creates and initializes her DH KeyAgreement object
        System.out.println("CLIENT: Initialization ...");
        clientKeyAgree = KeyAgreement.getInstance("DH");
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
        KeyFactory clientKeyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(serverPubKeyEnc);
        PublicKey serverPubKey = clientKeyFac.generatePublic(x509KeySpec);
        System.out.println("CLIENT : Execute PHASE1 ...");
        clientKeyAgree.doPhase(serverPubKey, true);
    }

    public byte[] generateSharedSecret(){
        /*
         * At this stage, both client and server have completed the DH key
         * agreement protocol.
         * Both generate the (same) shared secret.
         */
        byte[] sharedSecret = clientKeyAgree.generateSecret();
        clientAesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");
        return sharedSecret;
    }

    public byte[] encript(byte[] sharedSecret) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {

        /*
         * server encrypts, using AES in CBC mode
         */
        if (clientCipher == null) {
            clientCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        }

        IvParameterSpec ivParams = getIvParameterSpec();

        clientCipher.init(Cipher.ENCRYPT_MODE, clientAesKey,ivParams);
        byte[] cleartext = "This is just an example".getBytes();
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

        IvParameterSpec ivParams = getIvParameterSpec();

        clientCipher.init(Cipher.DECRYPT_MODE, clientAesKey,ivParams);
        return clientCipher.doFinal(ciphertext);
    }


}
