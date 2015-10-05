/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;



public class JvnServerImpl 	
extends UnicastRemoteObject 
implements JvnLocalServer, JvnRemoteServer{

	// A JVN server is managed as a singleton 
	private static JvnServerImpl js = null;
	
	// coordinateur distant
	JvnRemoteCoord coord;
	
	//liste des JvnObject disponible sur ce server
	ArrayList<JvnObject> cache;

	/**
	 * Default constructor
	 * @throws JvnException
	 **/
	private JvnServerImpl() throws Exception {
		super();
		coord = (JvnRemoteCoord) java.rmi.Naming.lookup("coordinateur");
		System.out.println("Coordinateur récupéré");
		cache = new ArrayList<JvnObject>();
	}

	/**
	 * Static method allowing an application to get a reference to 
	 * a JVN server instance
	 * @throws JvnException
	 **/
	public static JvnServerImpl jvnGetServer() {
		if (js == null){
			try {
				js = new JvnServerImpl();
			} catch (Exception e) {
				return null;
			}
		}
		return js;
	}

	/**
	 * The JVN service is not used anymore
	 * @throws JvnException
	 **/
	public  void jvnTerminate()
			throws jvn.JvnException {
		try {
			coord.jvnTerminate(this);
		} catch (RemoteException e) {
			System.out.println("problem server terminate : " + e.getMessage());
		}
	} 

	/**
	 * creation of a JVN object
	 * @param o : the JVN object state
	 * @throws JvnException
	 **/
	public  JvnObject jvnCreateObject(Serializable o)
			throws jvn.JvnException {
		JvnObject jo = null;
		try {
			//création d'un JvnObjet : son ID lui est fourni par le coordinateur
			//qui a juste a coté le nombre d'objet qui lui est fourni
			jo = new JvnObjectImpl(o, coord.jvnGetObjectId());
		} catch (RemoteException e) {
			System.out.println("problem server createObjetc : " + e.getMessage());
		}
		return jo;
	}

	/**
	 *  Associate a symbolic name with a JVN object
	 * @param jon : the JVN object name
	 * @param jo : the JVN object 
	 * @throws JvnException
	 **/
	public  void jvnRegisterObject(String jon, JvnObject jo)
			throws jvn.JvnException {
		try
		{
			//on enregistre le fait que le serveur utilise l'objet de nom jon dans le coordinateur
			coord.jvnRegisterObject(jon, jo, (JvnRemoteServer)this);
			
			//et on enregistre que l'objet sur le serveur
			cache.add(jo);
		} catch (Exception e) {
			System.out.println("problem serverImpl : jvnRegisterObject : " + e.getMessage());
		}
	}

	/**
	 * Provide the reference of a JVN object beeing given its symbolic name
	 * @param jon : the JVN object name
	 * @return the JVN object 
	 * @throws JvnException
	 **/
	public  JvnObject jvnLookupObject(String jon)
			throws jvn.JvnException {
		JvnObject jo = null;
		try
		{
			//on récupère l'objet jon sur le coordinateur
			jo = coord.jvnLookupObject(jon,(JvnRemoteServer)this);
			
			//si il était effectivement présent on l'ajoute au objet present sur le serveur
			if(jo != null)
			{
				cache.add(jo);
			}
		} catch (Exception e) {
			System.out.println("problem jvnLookupObject : " + e.getMessage());
		}
		return jo;
	}	

	/**
	 * Get a Read lock on a JVN object 
	 * @param joi : the JVN object identification
	 * @return the current JVN object state
	 * @throws  JvnException
	 **/
	public Serializable jvnLockRead(int joi)
			throws JvnException {
		Serializable objectPartage = null;
		try {
			//on signal que l'objet joi veut prendre le verrou en lecture
			objectPartage = coord.jvnLockRead(joi, this);
		} catch (RemoteException e) {
			System.out.println("problem server jvnLockRead : " + e.getMessage());
		}
		return objectPartage;

	}
	
	/**
	 * Get a Write lock on a JVN object 
	 * @param joi : the JVN object identification
	 * @return the current JVN object state
	 * @throws  JvnException
	 **/
	public Serializable jvnLockWrite(int joi) throws JvnException {
		Serializable objectPartage = null;
		try {
			//on signal que l'objet joi veut prendre le verrou en écriture
			objectPartage = coord.jvnLockWrite(joi, this);
		} catch (RemoteException e) {
			System.out.println("problem server jvnLockWrite : " + e.getMessage());
		}
		return objectPartage;
	}

	/**
	 * Invalidate the Read lock of the JVN object identified by id 
	 * called by the JvnCoord
	 * @param joi : the JVN object id
	 * @return void
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public void jvnInvalidateReader(int joi)
			throws java.rmi.RemoteException,jvn.JvnException {
		Iterator<JvnObject> i = cache.iterator();
		boolean find = false;
		
		// on parcourt le cache pour trouver l'objet à invalider
		while(i.hasNext() && !find)
		{
			JvnObject jo = (JvnObject)(i.next());
			if(jo.jvnGetObjectId() == joi)
			{
				jo.jvnInvalidateReader();
				find = true;
			}
		}
	};

	/**
	 * Invalidate the Write lock of the JVN object identified by id 
	 * @param joi : the JVN object id
	 * @return the current JVN object state
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public Serializable jvnInvalidateWriter(int joi)
			throws java.rmi.RemoteException,jvn.JvnException { 
		Serializable o = null;
		Iterator<JvnObject> i = cache.iterator();
		boolean find = false;
		
		// on parcourt le cache pour trouver l'objet à invalider
		while(i.hasNext() && !find)
		{
			JvnObject jo = (JvnObject)(i.next());
			if(jo.jvnGetObjectId() == joi)
			{
				o = jo.jvnInvalidateWriter();
				find = true;
			}
		}
		return o;
	};

	/**
	 * Reduce the Write lock of the JVN object identified by id 
	 * @param joi : the JVN object id
	 * @return the current JVN object state
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public Serializable jvnInvalidateWriterForReader(int joi)
			throws java.rmi.RemoteException,jvn.JvnException { 
		Serializable o = null;
		Iterator<JvnObject> i = cache.iterator();
		boolean find = false;
		
		// on parcourt le cache pour trouver l'objet à invalider
		while(i.hasNext() && !find)
		{
			JvnObject jo = (JvnObject)(i.next());
			if(jo.jvnGetObjectId() == joi)
			{
				o = jo.jvnInvalidateWriterForReader();
				find = true;
			}
		}
		return o;
	};

}


