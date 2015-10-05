/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

import irc.Irc;
import irc.Sentence;

import java.io.Serializable;


public class JvnCoordImpl 	
              extends UnicastRemoteObject 
							implements JvnRemoteCoord{
	int nbObjectCreate;
	HashMap<String,JvnObject> listNameJvnObject;
	ArrayList<JvnControlePartageObject> listControlePartageObject;

	public static void main(String argv[]) 
	{
		
		try 
		{
			LocateRegistry.createRegistry(1099);
			JvnCoordImpl coord = new JvnCoordImpl();
			java.rmi.Naming.bind("coordinateur", coord);
			System.out.println("Serveur pret ! ");
		   
		} catch (Exception e) {
			System.out.println("coord problem : " + e.getMessage());
		}
			
			
	}
	
  /**
  * Default constructor
  * @throws JvnException
  **/
	private JvnCoordImpl() throws Exception {
		nbObjectCreate = 0;
		listNameJvnObject = new HashMap<String,JvnObject>();
		listControlePartageObject = new ArrayList<JvnControlePartageObject>();
	}

  /**
  *  Allocate a NEW JVN object id (usually allocated to a 
  *  newly created JVN object)
  * @throws java.rmi.RemoteException,JvnException
  **/
  public int jvnGetObjectId()
  throws java.rmi.RemoteException,jvn.JvnException {
	int tmp = nbObjectCreate;
	nbObjectCreate++;
    return tmp;
  }
  
  /**
  * Associate a symbolic name with a JVN object
  * @param jon : the JVN object name
  * @param jo  : the JVN object 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
	  listNameJvnObject.put(jon, jo);
	  JvnControlePartageObject ctrl = new JvnControlePartageObject(jo.jvnGetObjectId(),jo.jvnGetObjectState(),js);
	  listControlePartageObject.add(ctrl);
  }
  
  /**
  * Get the reference of a JVN object managed by a given JVN server 
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
    JvnObject jo = listNameJvnObject.get(jon);
    if(jo!=null)
    {
	    JvnControlePartageObject ctrl = listControlePartageObject.get(jo.jvnGetObjectId());
		ctrl.addServer(js);
    }
    return jo;
  }
  
  /**
  * Get a Read lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public Serializable jvnLockRead(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
	   JvnControlePartageObject ctrl = listControlePartageObject.get(joi);
	   ctrl.newReader(js);
	   return ctrl.getObject();
   }

  /**
  * Get a Write lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public Serializable jvnLockWrite(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
	   JvnControlePartageObject ctrl = listControlePartageObject.get(joi);
	   ctrl.newWriter(js);
	   return ctrl.getObject();
   }

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
    public void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, JvnException {
	 // to be completed
    }
}

 
