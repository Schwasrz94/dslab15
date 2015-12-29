package client.listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.security.Key;

import channel.Channel;
import channel.HmacChannel;
import channel.TcpChannel;
import cli.Shell;

public class PrivateServerListener implements Runnable{

	private ServerSocket serverSocket;
	private Shell shell;
	private Key hmacKey;

	public PrivateServerListener(ServerSocket serverSocket, Shell s, Key hmacKey) {
		this.serverSocket = serverSocket;
		this.shell = s;
		this.hmacKey = hmacKey;
	}

	public void run() {
		try {
			while(true){				
				Channel tcp = new TcpChannel(serverSocket.accept());
				tcp = new HmacChannel(tcp,hmacKey);
				String m = tcp.read();
				if(m.startsWith("!tampered")){
					System.out.println("Tampered message: " + m.substring(10));
					tcp.write(m);
				}
				else{
					shell.writeLine(m);
					tcp.write("!ack");
				}
			}		
		} catch (SocketException e) {
			
		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
}
