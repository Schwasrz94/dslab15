package chatserver.listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import channel.Base64Channel;
import channel.Channel;
import channel.TcpChannel;
import chatserver.persistence.User;
import nameserver.INameserver;

/**
 * Listens for any incoming Client connections. Whenever a Client connections to the CloudController, the ClientListener spawns a new
 * ClientHandler in its own thread and adds it to the pool.
 *
 */
public class TcpListener implements Runnable {
	private ServerSocket serverSocket;
	private final ExecutorService pool;
	private Map<String,User> userMap;
	private List<Channel> tcpChannels;
	private INameserver root;

	public TcpListener(ServerSocket serverSocket, Map<String,User> userMap, ExecutorService pool,INameserver root) {
		this.serverSocket = serverSocket;
		this.userMap = userMap;
		this.pool = pool;
		this.tcpChannels = new ArrayList<Channel>();
		this.root=root;
	}

	public void run() {
		try {
			while(true){
				
				Channel tcp = new Base64Channel(new TcpChannel(serverSocket.accept()));
				ClientThread client = new ClientThread(tcp, userMap, this,root);
				pool.execute(client);
				
			}		
		} catch (SocketException e) {
			//Stops Thread if datagramSocket gets closed	
			for(Channel c : tcpChannels) {
				if (c != null && c.isOpen()){
					try {
						c.close();
					} catch (IOException e1) {
						// Ignored because we cannot handle it
					}
				}
			}
		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	public List<Channel> getTcpChannels() {
		return tcpChannels;
	}
	public synchronized void addChanneltoTcpChannels(Channel channel) {
		tcpChannels.add(channel);
	}

}
