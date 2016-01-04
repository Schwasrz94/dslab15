package client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.Key;
import java.security.Security;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;
import util.Keys;
import channel.Channel;
import channel.HmacChannel;
import channel.TcpChannel;
import cli.Command;
import cli.Shell;
import client.listener.PrivateServerListener;
import client.listener.ServerListener;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private Shell shell;
	private Channel tcp;
	private DatagramSocket udp;
	private ServerSocket privateServerSocket;
	private boolean loggedIn;
	private String user;
	private ServerListener serverListener;
	private final ExecutorService pool;
	private String sresp;
	private Key hmacKey;
	
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
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		this.loggedIn = false;
		this.user = null;
		this.serverListener = null;
		this.sresp = null;
		
		try {
			this.hmacKey = Keys.readSecretKey(new File(config.getString("hmac.key")));
		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		
		try {
			udp = new DatagramSocket();
		} catch (SocketException e) {
			System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		
		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
		
		pool = Executors.newCachedThreadPool();
	}

	public String getSresp() {
		if(sresp == null) return null;
		String s = sresp;
		sresp = null;
		return s;
	}

	public void setSresp(String sresp) {
		this.sresp = sresp;
	}

	@Override
	public void run() {
		pool.execute(shell);
		System.out.println(getClass().getName() + " up and waiting for commands!");
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException {
		if(loggedIn) return "You are already logged in";
		
		tcp = new TcpChannel();
		tcp.bind(new InetSocketAddress(config.getString("chatserver.host"), config.getInt("chatserver.tcp.port")));	
		
		serverListener = new ServerListener(tcp,shell,this);
		tcp.write("!login-"+username+"-"+password);
		
		String response = tcp.read();
		if (response.equals("Successfully logged in.")) {
			loggedIn = true;
			user = username;
			pool.execute(serverListener);
		}
		else tcp.close();
		return response;
	}

	@Override
	@Command
	public String logout() throws IOException {
		if(!loggedIn) return "You are not logged in";
		
		serverListener.setWaitForResponse(true);
		tcp.write("!logout");
	
		String response;
		while((response = getSresp()) == null);
		if(response.equals("Successfully logged out.")){
			if (tcp != null && tcp.isOpen())
				tcp.close();
			if (privateServerSocket != null && !privateServerSocket.isClosed())
				privateServerSocket.close();
			loggedIn = false;
			user = null;
		}		
		
		return response;
	}

	@Override
	@Command
	public String send(String message) throws IOException {
		if(!loggedIn) return "You are not logged in";
		tcp.write("!send-"+message);
		return "";
	}

	@Override
	@Command
	public String list() throws IOException {
		byte[] buffer = (new String("!list")).getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
				InetAddress.getByName(config.getString("chatserver.host")),
				config.getInt("chatserver.udp.port"));
		
		udp.setSoTimeout(2000);
		
		udp.send(packet);

		buffer = new byte[1024];
		packet = new DatagramPacket(buffer, buffer.length);
		try{
			udp.receive(packet);
		} catch(SocketTimeoutException e) {
            System.out.println("Server not reachable. Try again later.");
        }
		
		return new String(packet.getData());
	}

	@Override
	@Command
	public String msg(String username, String message) throws IOException {
		if(!loggedIn) return "You are not logged in";
		serverListener.setWaitForResponse(true);
		tcp.write("!lookup-"+username);
	
		String response;
		while((response = getSresp()) == null);
		
		if(response.contains(":")){
			
			String[] parts = response.split(":");
			System.out.println(parts[0]+" "+Integer.parseInt(parts[1]));
			System.out.println(parts[0].length());
			
			Channel privateTcp = new TcpChannel();
			privateTcp.bind(new InetSocketAddress(InetAddress.getByName(parts[0]),Integer.parseInt(parts[1])));	
			privateTcp = new HmacChannel(privateTcp,hmacKey);
			
			privateTcp.write("["+user+"]: "+message);
			String m = privateTcp.read();
			if(m.trim().equals("!ack")){
				response = username+" replied with ack";
			}
			else {
				response = "A message has been modified!";
			}
			privateTcp.close();
		}
		return response;
	}

	@Override
	@Command
	public String lookup(String username) throws IOException {
		if(!loggedIn) return "You are not logged in";
		
		serverListener.setWaitForResponse(true);
		tcp.write("!lookup-"+username);
		
		String response;
		while((response = getSresp()) == null);
		return response;
	}

	@Override
	@Command
	public String register(String privateAddress) throws IOException {
		if(!loggedIn) return "You are not logged in";
		String[] parts = privateAddress.split(":");
		if(parts.length<2) return "Wrong format of address.";
		
		serverListener.setWaitForResponse(true);
		tcp.write("!register-"+privateAddress);
		
		String response;
		while((response = getSresp()) == null);
		sresp = null;
		
		
			System.out.println("starting private connection");
				if (privateServerSocket != null && !privateServerSocket.isClosed())
					privateServerSocket.close();
				privateServerSocket = new ServerSocket(Integer.parseInt(parts[1]));
				PrivateServerListener psListener = new PrivateServerListener(privateServerSocket,shell,hmacKey);
				pool.execute(psListener);

		return response;
	}
	
	@Override
	@Command
	public String lastMsg() throws IOException {
		if(!loggedIn) return "You are not logged in";
		return serverListener.getLastMsg();
	}

	@Override
	@Command
	public String exit() throws IOException {
		if(loggedIn){
			logout();
		}
		if (udp != null && !udp.isClosed())
			udp.close();
		if (tcp != null && tcp.isOpen())
			tcp.close();
		if (privateServerSocket != null && !privateServerSocket.isClosed())
			privateServerSocket.close();
		pool.shutdown();
		shell.close();
		return "Bye";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in,
				System.out);
		new Thread((Runnable) client).start();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
