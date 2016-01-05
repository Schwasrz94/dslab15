package channel;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

/**
 * Created by Saturn on 04.01.2016.
 */
public class AESChannel extends ChannelDecorator {
    private SecretKey secretKey;
    private IvParameterSpec ivParameterSpec;
    private Cipher cipher;

    public AESChannel(Channel channelToBeDecorated, SecretKey secretKey, IvParameterSpec ivParameterSpec, Cipher cipher) {
        super(channelToBeDecorated);
        this.secretKey = secretKey;
        this.ivParameterSpec = ivParameterSpec;
        this.cipher = cipher;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public void bind(InetSocketAddress addr) throws IOException, ClosedChannelException {
        super.bind(addr);
    }

    @Override
    public byte[] read() throws IOException, ClosedChannelException {
        try {
            return decrypt(super.read());
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            return null;
        } catch (BadPaddingException e) {
            e.printStackTrace();
            return null;
        }

    }

    private byte[] decrypt(byte[] message) throws IllegalBlockSizeException, BadPaddingException {
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            System.out.println("ERROR in AesTcpChannel decrypt: " + e.getMessage());
        }
        return cipher.doFinal(message);
    }

    @Override
    public void write(byte[] m) throws ClosedChannelException {
        try {
            super.write(encrypt(m));
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();

        }
    }

    private byte[] encrypt(byte[] message) throws IllegalBlockSizeException, BadPaddingException {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            System.out.println("ERROR in AesTcpChannel encrypt: " + e.getMessage());
        }
        return cipher.doFinal(message);
    }

    @Override
    public boolean isOpen() {
        return super.isOpen();
    }
}
