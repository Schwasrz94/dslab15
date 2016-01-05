package channel;

import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetBoundException;

/**
 * Created by Saturn on 04.01.2016.
 */
public class Base64Channel extends ChannelDecorator {
    public Base64Channel(Channel channel) {
        super(channel);
    }
    @Override
    public void bind(InetSocketAddress addr) throws IOException,ClosedChannelException {
        channelToBeDecorated.bind(addr);
    }
    @Override
    public byte[] read() throws ClosedChannelException, NotYetBoundException, IOException {
        return decode(super.read());
    }

    private byte[] decode(byte[] mess) {
        return Base64.decode(mess);
    }

    @Override
    public void write(byte[] mess) throws ClosedChannelException {
        super.write(encode(mess));

    }

    private byte[] encode(byte[] mess) {
        return Base64.encode(mess);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public boolean isOpen() {
        return super.isOpen();
    }
}
