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
import java.security.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import channel.*;

import org.bouncycastle.util.encoders.Base64;

import util.AesUtil;
import util.Config;
import util.Keys;
import cli.Command;
import cli.Shell;
import client.listener.PrivateServerListener;
import client.listener.ServerListener;
import util.RsaUtil;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;

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
	
/*
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
*/
	
	@Override
	@Command
	public String logout() throws IOException {
		if(!loggedIn) return "You are not logged in";

		serverListener.setWaitForResponse(true);
		tcp.write("!logout".getBytes());
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
		tcp.write(("!send-"+message).getBytes());
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
		tcp.write(("!lookup-"+username).getBytes());

		String response;
		while((response = getSresp()) == null);

		if(response.contains(":")){

			String[] parts = response.split(":");
			System.out.println(parts[0]+" "+Integer.parseInt(parts[1]));
			System.out.println(parts[0].length());

			Channel privateTcp = new TcpChannel();
			privateTcp.bind(new InetSocketAddress(InetAddress.getByName(parts[0]),Integer.parseInt(parts[1])));
			privateTcp = new HmacChannel(privateTcp,hmacKey);

			privateTcp.write(("["+user+"]: "+message).getBytes());
			String m = new String(privateTcp.read(),"UTF-8");
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
		tcp.write(("!lookup-"+username).getBytes());

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
		tcp.write(("!register-"+privateAddress).getBytes());

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
	@Command
	public String authenticate(String username) throws IOException {
		if(!loggedIn){
			
			tcp = new Base64Channel(new TcpChannel());
			try {
				tcp.bind(new InetSocketAddress(config.getString("chatserver.host"), config.getInt("chatserver.tcp.port")));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			PrivateKey privateKey;
			try {
				privateKey = Keys.readPrivatePEM(new File("keys/client/" + username + ".pem"));
			} catch (Exception e) {
				return "User doesn�t exist.";
			}
			PublicKey serverPublicKey = Keys.readPublicPEM(new File("keys/client/chatserver.pub.pem"));


			byte[] clientChallenge = new byte[32];
			SecureRandom secureRandom = new SecureRandom();
			secureRandom.nextBytes(clientChallenge);
			clientChallenge = Base64.encode(clientChallenge);

			byte[] serverChallange = new byte[32];

			byte[] aesKey = null;

			byte[] aesIvParameter = null;

			//First message
			try {
				byte[] rsaString = (new RsaUtil()).encrypt("!authenticate-" + username + "-" + new String(clientChallenge, "UTF-8"), serverPublicKey);
				tcp.write(rsaString);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (BadPaddingException e) {
				e.printStackTrace();
			}

			byte[] controllerResponseBytes = tcp.read();
			String serverResponse = null;
			try {
				serverResponse = (new RsaUtil()).decrypt(controllerResponseBytes, privateKey);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (BadPaddingException e) {
				e.printStackTrace();
			}

			if (serverResponse.startsWith("!ok")) {
				String parts[] = serverResponse.split("_");
				serverChallange = Base64.decode(parts[2]);
				if(Arrays.equals(Base64.decode(clientChallenge),Base64.decode(parts[1]))){
					aesKey = Base64.decode(parts[3]);
					aesIvParameter = Base64.decode(parts[4]);
				} else {
					return "authentication failed";
				}
			} else {
				return "authentication failed";
			}
			SecretKey secretKey = (new AesUtil()).getSecretKeyFromBytes(aesKey);
			if (secretKey == null)
				return "authentication failed";

			IvParameterSpec ivParameterSpec = (new AesUtil()).getIvParameterSpecFromBytes(aesIvParameter);

			Cipher cipher = (new AesUtil()).getCipher("AES/CTR/NoPadding");
			if (cipher == null)
				return "authentication failed";

			tcp = new AESChannel(tcp,secretKey ,ivParameterSpec ,cipher );

			//third massage
			tcp.write(serverChallange);

			this.loggedIn = true;
			this.user = username;
			serverListener = new ServerListener(tcp,shell,this);
			pool.execute(serverListener);
		} else {
			return "You are already logged in!";
		}
		return username + " successfully authenticated!";
	}

}
