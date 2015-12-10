package channel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;

public class TcpChannel implements Channel {

	private boolean open;
	private BufferedReader reader;
	private PrintWriter writer;
	private Socket socket;
	
	public TcpChannel(Socket socket) throws IOException{
		this.open = true;
		this.socket = socket;
		reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
		writer = new PrintWriter(this.socket.getOutputStream(), true);
	}
	
	public TcpChannel(){
		this.open = true;
	}
	
	@Override
	public void close(){
		if (socket != null && !socket.isClosed())
			try {
				socket.close();
			} catch (Exception e) {
				// Ignored because we cannot handle it
			}
		this.open = false;
	}

	@Override
	public void bind(InetSocketAddress addr) throws IOException, ClosedChannelException {
		if(!open) throw new ClosedChannelException();
		socket = new Socket(addr.getAddress(),addr.getPort());
		reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
		writer = new PrintWriter(this.socket.getOutputStream(), true);
	}

	@Override
	public String read() throws IOException, ClosedChannelException {
		if(!open) throw new ClosedChannelException();
		return reader.readLine();
	}

	@Override
	public void write(String m) throws ClosedChannelException {
		if(!open) throw new ClosedChannelException();
		writer.println(m);
		writer.flush();
	}

	@Override
	public boolean isOpen() {
		return open;
	}

}
