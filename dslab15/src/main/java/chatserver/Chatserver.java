package chatserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;
import chatserver.listener.TcpListener;
import chatserver.listener.UdpListener;
import chatserver.persistence.User;
import cli.Command;
import cli.Shell;

public class Chatserver implements IChatserverCli, Runnable {

	@SuppressWarnings("unused")
	private String componentName;
	@SuppressWarnings("unused")
	private Config config;
	@SuppressWarnings("unused")
	private InputStream userRequestStream;
	@SuppressWarnings("unused")
	private PrintStream userResponseStream;
	private Shell shell;
	private Map<String,User> userMap;
	private ServerSocket serverSocket;
	private DatagramSocket datagramSocket;
	private final ExecutorService pool;
	
	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Chatserver(String componentName, Config config, InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		this.userMap = new HashMap<String,User>();
		
		Config userConfig = new Config("user");
		Set<String> userKeys = userConfig.listKeys();
		for (String key : userKeys) {
			String userName = key.replace(".password", "");
			User user = new User(userName,userConfig.getString(key),false);
			userMap.put(key.replace(".password", ""), user);
		}
		
		try {
			serverSocket = new ServerSocket(config.getInt("tcp.port"));
			datagramSocket = new DatagramSocket(config.getInt("udp.port"));
		} catch (SocketException e) {
			System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		}

		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
		
		pool = Executors.newCachedThreadPool();
	}

	@Override
	public void run() {
		
		TcpListener tcp = new TcpListener(serverSocket,userMap,pool);
		pool.execute(tcp);
		
		UdpListener udp = new UdpListener(datagramSocket,userMap);
		pool.execute(udp);
		
		pool.execute(shell);
		System.out.println(getClass().getName()+ " up and waiting for commands!");
		
	}

	@Override
	@Command
	public String users() throws IOException {
		String result = "";
		List<String> usernames = new ArrayList<String>(userMap.keySet());
		Collections.sort(usernames);
		for(int i = 0;i<usernames.size();i++){
			String user = usernames.get(i);
			result += (i+1)+". "+user+" "+(userMap.get(user).isOnline()?"online":"offline")+"\n";
		}
		return result;
	}

	@Override
	@Command
	public String exit() throws IOException {
		if(serverSocket != null && !serverSocket.isClosed())
			serverSocket.close();
		if(datagramSocket != null && !datagramSocket.isClosed())
			datagramSocket.close();
		pool.shutdown();
		shell.close();
		return "Shutting down server";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) {
		Chatserver chatserver = new Chatserver(args[0],
				new Config("chatserver"), System.in, System.out);
		new Thread((Runnable) chatserver).start();
	}

}
