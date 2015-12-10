package chatserver.persistence;

public class User {

	private String name;
	private String password;
	private boolean online;
	private String addr;
	
	public User(String name, String password, boolean online) {
		this.name = name;
		this.password = password;
		this.online = online;
		this.addr = null;
	}

	public String getAddr() {
		return addr;
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public String getName() {
		return name;
	}

}
