package channel;

import java.io.*;
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
		System.out.println("ADDRESS: "+addr.toString());
		if(!open) throw new ClosedChannelException();
		socket = new Socket(addr.getAddress(),addr.getPort());
		reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
		writer = new PrintWriter(this.socket.getOutputStream(), true);
	}

	@Override
	public byte[] read() throws IOException, ClosedChannelException {
		if(!open) throw new ClosedChannelException();
		return reader.readLine().getBytes();
	}

	@Override
	public void write(byte[] m) throws ClosedChannelException {
		if(!open) throw new ClosedChannelException();
		try {
			writer.println(new String(m,"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		writer.flush();
	}

	@Override
	public boolean isOpen() {
		return open;
	}

}
