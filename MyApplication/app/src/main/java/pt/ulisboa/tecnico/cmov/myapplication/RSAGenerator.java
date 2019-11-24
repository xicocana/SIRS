package pt.ulisboa.tecnico.cmov.myapplication;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;

public class RSAGenerator {

    public RSAGenerator() {
        try {

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");

            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();

            Key pub = kp.getPublic();
            Key pvt = kp.getPrivate();

            System.out.print(System.getProperty("user.dir") );
            String outFile = System.getProperty("user.dir") + "/" + "resources/client";

            FileOutputStream out = new FileOutputStream(outFile + ".key");
            out.write(pvt.getEncoded());
            out.close();

            out = new FileOutputStream(outFile + ".pub");
            out.write(pvt.getEncoded());
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Optional<PublicKey> getPublicKey(String keyFile) {
        PublicKey pub = null;
        try {

            KeyStore keyStore =  KeyStore.getInstance("AndroidKeyStore");
            keyStore.getKey("server.pub","1234".toCharArray());

            String dir = System.getProperty("user.dir") + "/" + "resources/";
            /* Read all the public key bytes */
            Path path = Paths.get(dir + keyFile);
            byte[] bytes = Files.readAllBytes(path);

            /* Generate public key. */
            X509EncodedKeySpec ks = new X509EncodedKeySpec(bytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            pub = kf.generatePublic(ks);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(pub);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Optional<PrivateKey> getPrivateKey(String keyFile) {
        PrivateKey pvt = null;
        try {
            String dir = System.getProperty("user.dir") + "/" + "resources/";
            /* Read all the public key bytes */
            Path path = Paths.get(dir + keyFile);
            byte[] bytes = Files.readAllBytes(path);

            /* Generate private key. */
            PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(bytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            pvt = kf.generatePrivate(ks);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(pvt);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Optional<byte[]> generateSign(byte[] dataFile, String privateKeyName) {
        Signature sign;

        try {
            sign = Signature.getInstance("SHA256withRSA");

            Optional<PrivateKey> privateKey = getPrivateKey(privateKeyName);
            privateKey.ifPresent(privateKey1 -> {
                try {
                    sign.initSign(privateKey1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            sign.update(dataFile);
            return Optional.of(sign.sign());

        } catch (NoSuchAlgorithmException | SignatureException e) {
            e.printStackTrace();
        }

        return Optional.empty();

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean validateSign(byte[] dataFile, byte[]dataSignedFile, String pubKeyName) {
        Signature sign;
        try {
            sign = Signature.getInstance("SHA256withRSA");

            Optional<PublicKey> publicKey = getPublicKey(pubKeyName);
            publicKey.ifPresent(publicKey1 -> {
                try {
                    sign.initVerify(publicKey1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            sign.update(dataFile);
            return sign.verify(dataSignedFile);

        } catch (NoSuchAlgorithmException | SignatureException e) {
            e.printStackTrace();
        }
        return false;
    }
}
