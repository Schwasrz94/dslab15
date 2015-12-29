package util;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import javax.crypto.Mac;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

/**
 * Please note that this class is not needed for Lab 1, but can later be
 * used in Lab 2.
 * 
 * Provides security provider related utility methods.
 */
public final class SecurityUtils {

	private SecurityUtils() {
	}

	/**
	 * Registers the {@link BouncyCastleProvider} as the primary security
	 * provider if necessary.
	 */
	public static synchronized void registerBouncyCastle() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.insertProviderAt(new BouncyCastleProvider(), 0);
		}
	}
	
	public static String encryptHmac(String input, Key hmacKey){
			byte[] hmacValue;
			try {
				hmacValue = createHmacBase64Hash(input.getBytes(), hmacKey);
			} catch (InvalidKeyException | NoSuchAlgorithmException e) {
				e.printStackTrace();
				return input;
			}
			return new String(hmacValue) + " " + input;
	}

	private static byte[] createHmacBase64Hash(byte[] input, Key hmacKey) throws InvalidKeyException, NoSuchAlgorithmException {
		Mac hMac = Mac.getInstance("HmacSHA256");
		hMac.init(hmacKey);
		byte[] hashedMessage = hMac.doFinal(input);
		final String B64 = "a - zA - Z0 -9/+ " ;
		assert new String(hashedMessage) . matches ( " [ " + B64 + " ]{43}= [\\ s [^\\ s ]]+ " ) ;
		return Base64.encode(hashedMessage);
	}
	
	public static String decryptHmac(String input, Key hmacKey) {
		String[] m = input.split("\\s+");
		String hash = m[0];
		String text = "";
		for(int i=1;i<m.length;i++){
			text+=m[i]+" ";
		}
		try {
			byte[] cHash = createHmacBase64Hash(text.trim().getBytes(), hmacKey);
			boolean equal = MessageDigest.isEqual(cHash, hash.trim().getBytes());
			if (equal) {
				return text;
			} else { 
				return "!tampered "+text;
			}
		} catch (InvalidKeyException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
}
