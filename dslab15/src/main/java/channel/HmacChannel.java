package channel;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.ClosedChannelException;
import java.security.Key;

import util.SecurityUtils;

public class HmacChannel extends ChannelDecorator {

	private Key hmacKey;

	public HmacChannel(Channel tcpChannel, Key hmacKey) {
		super(tcpChannel);
		this.hmacKey = hmacKey;
	}

	@Override
	public byte[] read() throws ClosedChannelException, IOException {
		return SecurityUtils.decryptHmac(new String(super.read(),"UTF-8"), hmacKey).getBytes();
	}

	@Override
	public void write(byte[] m) throws ClosedChannelException {
		try {
			super.write((SecurityUtils.encryptHmac(new String(m,"UTF-8"), hmacKey)).getBytes());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

}
