package Crypto;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;

public class RSAGenerator {

    public static void main(String[] args) {
        try {

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");

            kpg.initialize(256);
            KeyPair kp = kpg.generateKeyPair();

            Key pub = kp.getPublic();
            Key pvt = kp.getPrivate();

            String outFile = System.getProperty("user.dir") + "/" + "resources/server";

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

    public Optional<PublicKey> getPublicKey(String keyFile) {
        PublicKey pub = null;
        try {
            String dir = System.getProperty("user.dir") + "/" + "resources/";
            /* Read all the public key bytes */
            Path path = Paths.get(dir + keyFile);
            byte[] bytes = Files.readAllBytes(path);

            /* Generate public key. */
            X509EncodedKeySpec ks = new X509EncodedKeySpec(bytes);
            KeyFactory kf = KeyFactory.getInstance("EC");
            pub = kf.generatePublic(ks);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(pub);
    }

    public Optional<PrivateKey> getPrivateKey(String keyFile) {
        PrivateKey pvt = null;
        try {
            String dir = System.getProperty("user.dir") + "/" + "resources/";
            /* Read all bytes from the private key file */
            Path path = Paths.get(dir + keyFile);
            byte[] bytes = Files.readAllBytes(path);

            /* Generate private key. */
            PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(bytes);
            KeyFactory kf = KeyFactory.getInstance("EC");
            pvt = kf.generatePrivate(ks);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(pvt);
    }

    public Optional<byte[]> generateSign(byte[] dataFile, String privateKeyName) {
        Signature sign;

        try {
            sign = Signature.getInstance("SHA256withEC");

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

    public boolean validateSign(byte[] dataFile,byte[]dataSignedFile, String pubKeyName) {
        Signature sign;
        try {
            sign = Signature.getInstance("SHA256withEC");

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
