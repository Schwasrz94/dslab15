package chatserver.listener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import chatserver.persistence.User;

/**
 * Listens for any incoming Client connections. Whenever a Client connections to the CloudController, the ClientListener spawns a new
 * ClientHandler in its own thread and adds it to the pool.
 *
 */
public class UdpListener implements Runnable {
	private DatagramSocket datagramSocket;
	private Map<String,User> userMap;

	public UdpListener(DatagramSocket datagramSocket, Map<String,User> userMap ) {
		this.datagramSocket = datagramSocket;
		this.userMap = userMap;
	}


	public void run() {
		byte[] buffer;
		DatagramPacket packet;
		String response;

		try{
			while (true) {
				response = "";
				buffer = new byte[1024];
				packet = new DatagramPacket(buffer, buffer.length);
				
				datagramSocket.receive(packet);

				String s = new String(packet.getData(),0,packet.getLength());
				String t = "!list";
				
				if (t.equals(s)) {
					List<String> usernames = new ArrayList<String>(userMap.keySet());
					Collections.sort(usernames);
					response = "Online users:\n";
					for(String user : usernames){
						if(userMap.get(user).isOnline())
							response += "* "+user+"\n";
					}
				}
				else response = "Unknown command! lol";
				
				datagramSocket.send(new DatagramPacket(response.getBytes(), response.getBytes().length, packet.getSocketAddress()));
			}
		} catch (SocketException e) {
			//Stop Thread if datagramSocket gets closed
		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

}
