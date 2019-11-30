package pt.ulisboa.tecnico.cmov.myapplication;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Enumeration;
import java.util.Optional;



public class RSAGenerator {

    private static final String KEYSTORE_ALIAS = "clientKS";

    public RSAGenerator() {

    }

    @TargetApi(Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Optional<PublicKey> getPublicKey(String keyFile) {
        PublicKey pub = null;
        try {

            String ExternalStorageDirectoryPath = Environment
                    .getExternalStorageDirectory()
                    .getAbsolutePath();
            String targetPath = ExternalStorageDirectoryPath + "/SIRS/server.pub";

            byte[] keyBytes = Files.readAllBytes(Paths.get(targetPath));

            X509EncodedKeySpec spec =
                    new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("EC");
            pub = kf.generatePublic(spec);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(pub);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Optional<PrivateKey> getPrivateKey(String keyFile) {
        PrivateKey pvt = null;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            Enumeration<String> aliases = ks.aliases();

            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);


            KeyStore.Entry entry = keyStore.getEntry(KEYSTORE_ALIAS, null);
            pvt = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
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
            sign = Signature.getInstance("SHA256withECDSA");

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
            sign = Signature.getInstance("SHA256withECDSA");

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
