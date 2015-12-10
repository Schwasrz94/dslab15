package channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;

public class ChannelDecorator implements Channel{

	public Channel channelToBeDecorated;
	
	public ChannelDecorator(Channel channelToBeDecorated) {
		this.channelToBeDecorated = channelToBeDecorated;
	}

	@Override
	public void close() throws IOException {
		channelToBeDecorated.close();
	}

	@Override
	public void bind(InetSocketAddress addr) throws IOException,ClosedChannelException {
		channelToBeDecorated.bind(addr);
	}

	@Override
	public String read() throws IOException, ClosedChannelException {
		return channelToBeDecorated.read();
	}

	@Override
	public void write(String m) throws ClosedChannelException {
		channelToBeDecorated.write(m);
	}

	@Override
	public boolean isOpen() {
		return channelToBeDecorated.isOpen();
	}
}
