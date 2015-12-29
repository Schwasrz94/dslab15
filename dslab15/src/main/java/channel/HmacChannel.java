package channel;

import java.io.IOException;
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
	public String read() throws ClosedChannelException, IOException {
		return SecurityUtils.decryptHmac(super.read(), hmacKey);
	}

	@Override
	public void write(String m) throws ClosedChannelException {
		super.write(SecurityUtils.encryptHmac(m, hmacKey));
	}

}
