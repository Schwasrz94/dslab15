package chatserver.listener;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.ClosedChannelException;
import java.rmi.RemoteException;
import java.security.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import channel.AESChannel;
import channel.Channel;
import chatserver.persistence.User;
import nameserver.INameserver;
import nameserver.INameserverForChatserver;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import org.bouncycastle.util.encoders.Base64;
import util.AesUtil;
import util.Keys;
import util.RsaUtil;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;

public class ClientThread implements Runnable{

	private Channel tcp;
	private String currentUser;
	private Map<String,User> userMap;
	private TcpListener listener;
	private INameserver root;
	private boolean authenticated;
	private PrivateKey serverPrivateKey;
	private PublicKey userPublicKey;
	
	public ClientThread(Channel tcp, Map<String,User> userMap, TcpListener listener, INameserver root) {
		this.tcp = tcp;
		this.userMap = userMap;
		this.currentUser = null;
		this.listener = listener;
		this.root=root;
		this.authenticated = false;
		try {
			this.serverPrivateKey =  Keys.readPrivatePEM(new File("keys/chatserver/chatserver.pem"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		byte[] request;
		try {
			while ((request = tcp.read()) != null) {
				String req = new String(request,"UTF-8");
				if(!authenticated) {
					try {
						req = (new RsaUtil()).decrypt(request,serverPrivateKey);
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
				}
				String[] parts = req.split("-");
				switch (parts[0]){
				case "!logout":
					tcp.write(logout().getBytes());
					tcp.close();
					break;
				case "!send":
					send(new String(request,"UTF-8"));
					break;
				case "!register":
					tcp.write(register(parts).getBytes());
					break;
				case "!lookup":
					tcp.write(lookup(parts).getBytes());
					break;
				case "!authenticate":
					authenticate(parts);
					break;
				default:
					tcp.write("Unknown command!".getBytes());
				}
			}
		} catch(ClosedChannelException e){
			//Stops Thread if tcpChannel gets closed	
		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		} 
	}
	private void authenticate(String[] parts) {
		String user = parts[1];
		String clientChallange = parts[2];

		try {
			this.userPublicKey = Keys.readPublicPEM(new File("keys/chatserver/" + user + ".pub.pem"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		SecureRandom secureRandom = new SecureRandom();
		byte[] serverChallenge = new byte[32];
		secureRandom.nextBytes(serverChallenge);

		byte[] encodedServerChallenge = Base64.encode(serverChallenge);


		SecretKey secretKey = (new AesUtil()).generateSecretKey();
		byte[] ivBytes = new byte[16];
		secureRandom.nextBytes(ivBytes);

		byte[] encodedSecretKey = Base64.encode(secretKey.getEncoded());
		byte[] encodedIvBytes = Base64.encode(ivBytes);

		//second message
		try {
			try {
				tcp.write((new RsaUtil()).encrypt("!ok_" + clientChallange + "_" + new String(encodedServerChallenge, "UTF-8") + "_" + new String(encodedSecretKey, "UTF-8") + "_" + new String(encodedIvBytes, "UTF-8"),this.userPublicKey));
			} catch (ClosedChannelException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (BadPaddingException e) {
				e.printStackTrace();
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		IvParameterSpec ivParameterSpec = (new AesUtil()).getIvParameterSpecFromBytes(ivBytes);

		Cipher cipher = (new AesUtil()).getCipher("AES/CTR/NoPadding");

		tcp = new AESChannel(tcp, secretKey, ivParameterSpec, cipher);

		byte[] returnedControllerChallenge = new byte[0];
		try {
			returnedControllerChallenge = tcp.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (Arrays.equals(serverChallenge, returnedControllerChallenge)) {
			currentUser = user;
			authenticated = true;
			synchronized(userMap) {
				userMap.get(user).setOnline(true);
			}
			listener.addChanneltoTcpChannels(tcp);
		}
	}
	
	/*
	public String login(String[] arguments){
		if(arguments.length < 3) return "Too few arguments.";
		if(currentUser != null) return "Already logged in.";
		if(userMap.containsKey(arguments[1]) && userMap.get(arguments[1]).getPassword().equals(arguments[2])){
			synchronized(userMap) {
				userMap.get(arguments[1]).setOnline(true);
			}
			currentUser = arguments[1];
			return "Successfully logged in.";
		}
		return "Wrong username or password.";
	}
	*/
	
	public String logout(){
		if(currentUser == null) return "Not logged in.";
		synchronized(userMap) {
			userMap.get(currentUser).setOnline(false);
			userMap.get(currentUser).setAddr(null);
		}
		currentUser = null;
		listener.getTcpChannels().remove(tcp);
		return "Successfully logged out.";
	}
	
	public void send(String request) throws ClosedChannelException{
		if(currentUser == null) return;
		String message = "[message]"+currentUser+": "+request.replace("!send-", "");
		for(Channel c :listener.getTcpChannels()){
			if(c != tcp){
				c.write(message.getBytes());
			}
		}
	}

	// Old methods
	/*
	public String register(String[] arguments){
		if(currentUser == null ) return "Not logged in.";
		if(arguments.length < 2) return "Too few arguments.";
		synchronized(userMap) {
			userMap.get(currentUser).setAddr(arguments[1]);
		}
		return "Successfully registered address for "+currentUser+".";
	}



	public String lookup(String[] arguments){
		if(currentUser == null ) return "Not logged in.";
		if(arguments.length < 2) return "Too few arguments.";
		if(!userMap.containsKey(arguments[1])) return "User "+arguments[1]+" not found";
		if(!userMap.get(arguments[1]).isOnline()) return "User "+arguments[1]+" not online";
		if(userMap.get(arguments[1]).getAddr() == null) return "User "+arguments[1]+" not registered";
		return userMap.get(arguments[1]).getAddr();
	}*/

	public String register(String[] arguments){
		if(currentUser == null ) return "Not logged in.";
		if(arguments.length < 2) return "Too few arguments.";
		/*synchronized(userMap) {
			userMap.get(currentUser).setAddr(arguments[1]);
		}*/
		try {
			root.registerUser(currentUser,arguments[1]);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (AlreadyRegisteredException e) {
			e.printStackTrace();
			return "user already registered";
		} catch (InvalidDomainException e) {
			e.printStackTrace();
			return "invalid domain";
		}
		return "Successfully registered address for "+currentUser+".";
	}

	public String lookup(String[] arguments){
		if(currentUser == null ) return "Not logged in.";
		if(arguments.length < 2) return "Too few arguments.";
		return reverseLookup(arguments[1]);
	}
	
	private String reverseLookup(String currentUser) {
		String result=null;
		List<String> uSplit	= Arrays.asList(currentUser.split("\\."));
		INameserverForChatserver nameserver = root;
		for(int i = uSplit.size()-1;i>0;i--){
			if (nameserver==null) {
				return "User not found! Unknown Subdomain!";
			}
			try {
				nameserver=nameserver.getNameserver(uSplit.get(i));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		if (nameserver==null) {
			return "User not found! Unknown Subdomain!";
		} else {
			try {
				result = nameserver.lookup(uSplit.get(0));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		if (result==null) {
			return "User "+currentUser+" not registered";
		}
		return result;
	}

}
