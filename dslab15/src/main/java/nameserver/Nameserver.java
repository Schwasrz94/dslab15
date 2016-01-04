package nameserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cli.Command;
import cli.Shell;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import nameserver.remoteObjects.NameserverRemote;
import util.Config;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class Nameserver implements INameserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;

	private Shell shell;

	private String registryBindingname;
	private String registryHost;
	private String registryPort;
	private String domain;

	private Registry registry;

	private INameserver root;

	private INameserver remoteObject;
	private final INameserver exported;

	private HashMap<String,String> addresses;
	private List<String> nameservers;
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
	public Nameserver(String componentName, Config config,
					  InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		this.registryBindingname = config.getString("root_id");
		this.registryHost=config.getString("registry.host");
		this.registryPort=config.getString("registry.port");
		this.exported = new NameserverRemote(this);
		this.addresses = new HashMap<String, String>();
		this.nameservers = new ArrayList<String>();
		try {
			remoteObject = (INameserver) UnicastRemoteObject.exportObject(exported,0);
		} catch (RemoteException e) {
			e.printStackTrace();
			// TODO ignore?
		}
		if(config.listKeys().contains("domain")){
			this.domain=config.getString("domain");
			try {
				registry = LocateRegistry.getRegistry(registryHost,Integer.parseInt(registryPort));
				try {
					root = (INameserver) registry.lookup(registryBindingname);
					try {
						root.registerNameserver(domain,remoteObject,remoteObject);
					} catch (AlreadyRegisteredException e) {
						e.printStackTrace();
					} catch (InvalidDomainException e) {
						e.printStackTrace();
					}
				} catch (NotBoundException e) {
					e.printStackTrace();
					// TODO exception handling
				}
			} catch (RemoteException e) {
				e.printStackTrace();
				// TODO exception handling
			}
		} else {
			try {
				root = remoteObject;
				registry= LocateRegistry.createRegistry(Integer.parseInt(registryPort));
				registry.bind(registryBindingname,remoteObject);
			} catch (RemoteException e) {
				e.printStackTrace();
				// TODO ignore?
			} catch (AlreadyBoundException e) {
				e.printStackTrace();
				// TODO ignore?
			}
		}
		// TODO
	}

	@Override
	public void run() {
		// TODO
		this.shell = new Shell(componentName,userRequestStream,userResponseStream);
		shell.register(this);
		new Thread(shell).start();
	}

	@Command
	@Override
	public String nameservers() throws IOException {
		// TODO Auto-generated method stub
		for(String s:nameservers){
			System.out.println(s);
		}
		return null;
	}

	@Command
	@Override
	public String addresses() throws IOException {
		// TODO Auto-generated method stub
		for(String s : addresses.keySet()){
			System.out.println((s+" "+addresses.get(s)));
		}
		return null;
	}

	@Command
	@Override
	public String exit() throws IOException {
		// TODO Auto-generated method stub
		System.out.println("exit method called");
		shell.close();
		try {
			UnicastRemoteObject.unexportObject(exported, true);
		} catch (NoSuchObjectException e){
			//TODO exception handling
			e.printStackTrace();
		}
		if(domain==null) {
			try {
				registry.unbind(registryBindingname);
			} catch (NotBoundException e) {
				e.printStackTrace();
				// TODO exception handling
			}
		}

		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Nameserver}
	 *            component
	 */
	public static void main(String[] args) {
		Nameserver nameserver = new Nameserver(args[0], new Config(args[0]),
				System.in, System.out);
		// TODO: start the nameserver
		new Thread(nameserver).start();
	}

	@Override
	public String toString() {
		return "Nameserver{" +
				"componentName='" + componentName + '\'' +
				", registryBindingname='" + registryBindingname + '\'' +
				", registryHost='" + registryHost + '\'' +
				", registryPort='" + registryPort + '\'' +
				", domain='" + domain + '\'' +
				'}';
	}

	public String getComponentName() {
		return componentName;
	}

	public Config getConfig() {
		return config;
	}

	public InputStream getUserRequestStream() {
		return userRequestStream;
	}

	public PrintStream getUserResponseStream() {
		return userResponseStream;
	}

	public Shell getShell() {
		return shell;
	}

	public String getRegistryBindingname() {
		return registryBindingname;
	}

	public String getRegistryHost() {
		return registryHost;
	}

	public String getRegistryPort() {
		return registryPort;
	}

	public String getDomain() {
		return domain;
	}

	public Registry getRegistry() {
		return registry;
	}

	public void addAdress(String user,String address){
		addresses.put(user,address);
	}

	public void addNameserver(String s){
		nameservers.add(s);
	}
}
