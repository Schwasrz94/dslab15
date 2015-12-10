package client.listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;

import channel.Channel;
import channel.TcpChannel;
import cli.Shell;

public class PrivateServerListener implements Runnable{

	private ServerSocket serverSocket;
	private Shell shell;

	public PrivateServerListener(ServerSocket serverSocket, Shell s) {
		this.serverSocket = serverSocket;
		this.shell = s;
	}

	public void run() {
		try {
			while(true){				
				Channel tcp = new TcpChannel(serverSocket.accept());
				shell.writeLine(tcp.read());
				tcp.write("!ack");
			}		
		} catch (SocketException e) {
			
		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
}
