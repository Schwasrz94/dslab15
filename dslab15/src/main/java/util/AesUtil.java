package util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Saturn on 04.01.2016.
 */
public class AesUtil {
    public SecretKey generateSecretKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            return generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("ERROR creating KeyGenerator: " + e.getMessage());
        }
        return null;
    }
    public SecretKey getSecretKeyFromBytes(byte[] aesKey) {
        return new SecretKeySpec(aesKey, 0, aesKey.length, "AES");
    }

    public IvParameterSpec getIvParameterSpecFromBytes(byte[] aesIvParameter) {
        return new IvParameterSpec(aesIvParameter);
    }

    public Cipher getCipher(String s) {
        try {
            return Cipher.getInstance(s);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
