package nameserver.remoteObjects;

import nameserver.INameserver;
import nameserver.INameserverForChatserver;
import nameserver.Nameserver;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import org.omg.CORBA.DynAnyPackage.Invalid;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Martin on 22.12.2015.
 */
public class NameserverRemote implements INameserver,Remote{

    private Nameserver ns;
    private HashMap<String,INameserver> subdomains;
    private HashMap<String,String> users;

    public NameserverRemote(Nameserver nameserver){
        this.ns=nameserver;
        subdomains=new HashMap<String, INameserver>();
        users=new HashMap<String, String>();
    }

    @Override
    public void registerNameserver(String domain, INameserver nameserver, INameserverForChatserver nameserverForChatserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        System.out.println("Request: registerNameserver");
        System.out.println("domain: "+domain);
        List<String> dSplit = Arrays.asList(domain.split("\\."));
        if(dSplit.size()==1){
            if (subdomains.containsKey(dSplit.get(0))) {
                throw new AlreadyRegisteredException("Domain is already registered!");
            }
            else {
                subdomains.put(dSplit.get(0),nameserver);
                ns.addNameserver(dSplit.get(0));
            }
        } else if (dSplit.size()>1){
            if (subdomains.containsKey(dSplit.get(dSplit.size()-1))) {
                String next = "";
                String request = dSplit.get((dSplit.size()-1));
                for(int i = 0;i<dSplit.size()-1;i++){
                    next+=dSplit.get(i);
                    if(i<dSplit.size()-2)next+=".";
                }
                subdomains.get(request).registerNameserver(next, nameserver, nameserverForChatserver);
            } else {
                throw new InvalidDomainException("Domain uses unknown subdomain");
            }
        } else {
            throw new InvalidDomainException("Invalid Domain!");
        }
    }

    @Override
    public void registerUser(String username, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        System.out.println("Request: registerUser");
        System.out.println("Username: "+username);
        System.out.println("Address: "+address);
        /*try {
            ns.getShell().writeLine("start registering for user: "+username);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        List<String> uSplit = Arrays.asList(username.split("\\."));
        String user= "";
        if (uSplit.size()>0) {
            user=uSplit.get(0);
        } else {
            throw new InvalidDomainException("username/domain empty");
        }
        if(uSplit.size()==1){
            if(users.containsKey(user)){
                throw new AlreadyRegisteredException("User is already registered!");
            } else {
                users.put(user, address);
                ns.addAdress(user, address);
                /*try {
                    ns.getShell().writeLine("user registered: "+username);
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
            }
        } else {
            String sub = uSplit.get(uSplit.size()-1);
            if (subdomains.containsKey(sub)){
                String next="";
                for(int i = 0;i<uSplit.size()-1;i++){
                    next+=uSplit.get(i);
                    if(i<uSplit.size()-2)next+=".";
                }
                subdomains.get(sub).registerUser(next,address);
                /*try {
                    ns.getShell().writeLine("user passed \nnext: " + next);
                } catch (IOException e) {
                    e.printStackTrace();
                }*/

            } else throw new InvalidDomainException("Invalid subdomain!");
        }
        //System.out.println("registeruser method end");
    }

    @Override
    public INameserverForChatserver getNameserver(String zone) throws RemoteException {
        System.out.println("Requested: getNameserver");
        System.out.println("zone: "+zone);
        INameserverForChatserver result = null;
        if(subdomains.containsKey(zone)){
            result=subdomains.get(zone);
        }
        return result;
    }

    @Override
    public String lookup(String username) throws RemoteException {
        System.out.println("Requested: lookup");
        System.out.println("username: "+username);
        String result = null;
        if (users.containsKey(username)){
            result=users.get(username);
        }
        return result;
    }

}
