package pt.ulisboa.tecnico.cmov.myapplication;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Enumeration;

public class AssymetricUtils {

    private KeyStore ks;
    private static final String KEYSTORE_ALIAS = "clientKS";

    @RequiresApi(api = Build.VERSION_CODES.M)
    public AssymetricUtils(Context context) {

        try {
            /*
             * Generate a new EC key pair entry in the Android Keystore by
             * using the KeyPairGenerator API. The private key can only be
             * used for signing or verification and only with SHA-256 or
             * SHA-512 as the message digest.
             */
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            kpg.initialize(new KeyGenParameterSpec.Builder(  KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256,  KeyProperties.DIGEST_SHA512).build());

            KeyPair kp = kpg.generateKeyPair();

            /*
             * Load the Android KeyStore instance using the
             * "AndroidKeyStore" provider to list out what entries are
             * currently stored.
             */
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            Enumeration<String> aliases = ks.aliases();

            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);


            KeyStore.Entry entry = keyStore.getEntry(KEYSTORE_ALIAS, null);
            PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
            PublicKey publicKey = keyStore.getCertificate(KEYSTORE_ALIAS).getPublicKey();

            //create local folder
            File f = new File(Environment.getExternalStorageDirectory()+"/SIRS");
            if (!f.exists()) {
                if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    boolean t = f.mkdirs();
                    
                }
            }

            f = new File(Environment.getExternalStorageDirectory()+"/SIRS/public.key");
            f.createNewFile();

            //write the bytes in file
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(publicKey.getEncoded());
            fos.flush();
            fos.close();



        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException |
                KeyStoreException | CertificateException | IOException | UnrecoverableEntryException e) {
            e.printStackTrace();
        }
    }


}
