package Crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * A utility class that encrypts or decrypts a file.
 *
 * @author www.codejava.net
 */
public class EncrDecrFilesUtil {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    public static final String ENCRYPT = "encrypt";
    public static final String DECRYPT = "decrypt";


    public static void encrypt(String key, File inputFile, File outputFile)
            throws CryptoException {
        doCrypto(Cipher.ENCRYPT_MODE, key, inputFile, outputFile);
    }

    public static void decrypt(String key, File inputFile, File outputFile)
            throws CryptoException {
        doCrypto(Cipher.DECRYPT_MODE, key, inputFile, outputFile);
    }

    public static void doSomething(String folderName, String type, String key) {
        String dir = System.getProperty("user.dir") + "/" + folderName;
        File folder = new File(dir);


        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    try {
                        String fileDir = dir + "/" + file.getName();
                        if (EncrDecrFilesUtil.ENCRYPT.equals(type)) {
                            File encryptedFile = new File(fileDir + ".encrypted");
                            EncrDecrFilesUtil.encrypt(key, file, encryptedFile);
                            file.delete();
                            System.out.println("SERVER : Encrypted file " + file.getName());
                        } else if (EncrDecrFilesUtil.DECRYPT.equals(type)) {
                            File decryptedFile = new File(fileDir.replaceFirst(".encrypted", ""));
                            EncrDecrFilesUtil.decrypt(key, file, decryptedFile);
                            file.delete();
                            System.out.println("SERVER : Decrypted file " + file.getName());
                        }
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        }

    }

    private static void doCrypto(int cipherMode, String key, File inputFile,
                                 File outputFile) throws CryptoException {
        try {
            Key secretKey = new SecretKeySpec(key.getBytes("UTF-8"), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(cipherMode, secretKey);

            FileInputStream inputStream = new FileInputStream(inputFile);
            byte[] inputBytes = new byte[(int) inputFile.length()];
            inputStream.read(inputBytes);

            byte[] outputBytes = cipher.doFinal(inputBytes);

            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(outputBytes);

            inputStream.close();
            outputStream.close();

        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                | InvalidKeyException | BadPaddingException
                | IllegalBlockSizeException | IOException ex) {
            throw new CryptoException("Error encrypting/decrypting file", ex);
        }
    }
}

class CryptoException extends Exception {

    public CryptoException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
