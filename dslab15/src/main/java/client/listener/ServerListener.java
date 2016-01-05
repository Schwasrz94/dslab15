package client.listener;

import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;

import channel.Channel;
import cli.Shell;
import client.Client;

/**
 * Listens for any incoming Client connections. Whenever a Client connections to the CloudController, the ClientListener spawns a new
 * ClientHandler in its own thread and adds it to the pool.
 *
 */
public class ServerListener implements Runnable {

	private Channel tcp;
	private boolean waitForResponse;
	private Shell shell;
	private String lastMsg;
	private Client client;

	public ServerListener(Channel tcp, Shell s, Client c) {
		this.tcp = tcp;
		this.shell = s;
		this.client = c;
		this.waitForResponse = false;
	}

	public void run() {
		String message;
		try {

			while(((message = new String(tcp.read(),"UTF-8")) != null)) {

				if(message.startsWith("[message]")){

					message = message.replace("[message]", "");
					lastMsg = message;
					shell.writeLine(message);

				} else if (waitForResponse){

					client.setSresp(message);
					waitForResponse = false;

				} else shell.writeLine(message);
			}

		} catch(ClosedChannelException e){
			//Stops Thread if tcpChannel gets closed	
		} catch (SocketException e) {
			//Stops Thread if tcpChannel gets closed	
		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	public String getLastMsg() {
		return lastMsg;
	}

	public void setWaitForResponse(boolean b){
		this.waitForResponse = b;
	}
}
