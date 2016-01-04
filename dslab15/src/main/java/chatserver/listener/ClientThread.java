package chatserver.listener;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import channel.Channel;
import chatserver.persistence.User;
import nameserver.INameserver;
import nameserver.INameserverForChatserver;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;

public class ClientThread implements Runnable{

	private Channel tcp;
	private String currentUser;
	private Map<String,User> userMap;
	private TcpListener listener;
	private INameserver root;
	
	public ClientThread(Channel tcp, Map<String,User> userMap, TcpListener listener, INameserver root) {
		this.tcp = tcp;
		this.userMap = userMap;
		this.currentUser = null;
		this.listener = listener;
		this.root=root;
	}

	@Override
	public void run() {
		String request;
		try {
			while ((request = tcp.read()) != null) {
				String[] parts = request.split("-");
				
				switch (parts[0]){
				case "!login":
					tcp.write(login(parts));
					break;
				case "!logout":
					tcp.write(logout());
					tcp.close();
					break;
				case "!send":
					send(request);
					break;
				case "!register":
					tcp.write("");
					tcp.write(register(parts));
					break;
				case "!lookup":
					tcp.write(lookup(parts));
					break;
				default:
					tcp.write("Unknown command!");
				}
			}
		} catch(ClosedChannelException e){
			//Stops Thread if tcpChannel gets closed	
		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		} 
	}
	
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
				c.write(message);
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
		/*if(!userMap.containsKey(arguments[1])) return "User "+arguments[1]+" not found";
		if(!userMap.get(arguments[1]).isOnline()) return "User "+arguments[1]+" not online";
		if(userMap.get(arguments[1]).getAddr() == null) return "User "+arguments[1]+" not registered";*/



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
