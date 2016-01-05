package channel;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;

public interface Channel extends Closeable{

	public void bind(InetSocketAddress addr) throws IOException, ClosedChannelException;

	public byte[] read() throws IOException, ClosedChannelException;

	public void write(byte[] m) throws ClosedChannelException;

	public boolean isOpen();

}
