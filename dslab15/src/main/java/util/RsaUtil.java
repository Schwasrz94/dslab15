package util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.*;

public class RsaUtil {

    private Cipher cipher;

    public RsaUtil() throws NoSuchAlgorithmException, NoSuchPaddingException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
    }

    public byte[] encrypt(String message, PublicKey key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(message.getBytes("UTF-8"));
    }

    public String decrypt(byte[] message, PrivateKey key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(message));
    }

}
