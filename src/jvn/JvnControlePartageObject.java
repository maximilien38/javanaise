package jvn;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;

import irc.Sentence;

public class JvnControlePartageObject {
	
	Serializable object;
	int id;
	ArrayList<JvnRemoteServer> serverContainObject;
	ArrayList<JvnRemoteServer> serverReadObject;
	JvnRemoteServer serverWriteObject;
	
	
	public JvnControlePartageObject()
	{
		serverReadObject = new ArrayList<JvnRemoteServer>();
		serverWriteObject = null;
		
		serverContainObject = new ArrayList<JvnRemoteServer>();
		object = null;
	}
	
	public JvnControlePartageObject(int id,Serializable object, JvnRemoteServer js) {
		serverReadObject = new ArrayList<JvnRemoteServer>();
		serverWriteObject = null;
		
		this.id = id;
		serverContainObject = new ArrayList<JvnRemoteServer>();
		serverContainObject.add(js);
		this.object = object;
	}

	public void addServer(JvnRemoteServer js) {
		if(!serverContainObject.contains(js))
			serverContainObject.add(js);
	}

	public void setObject(Serializable object) {
		this.object = object;
	}

	public Serializable getObject() {
		return object;
	}

	public void newReader(JvnRemoteServer js) throws RemoteException, JvnException {
		if(serverWriteObject != null)
		{
			object = serverWriteObject.jvnInvalidateWriterForReader(id);
			serverWriteObject = null;
		}
		if(!serverReadObject.contains(js))
		{
			serverReadObject.add(js);
		}
		
	}

	public void newWriter(JvnRemoteServer js) throws RemoteException, JvnException {
		if(serverWriteObject != null)
		{
			object = serverWriteObject.jvnInvalidateWriter(id);
			serverWriteObject = null;
		}
		
		if(!serverReadObject.isEmpty())
		{
			Iterator i = serverReadObject.iterator();
			while(i.hasNext())
			{
				((JvnRemoteServer)(i.next())).jvnInvalidateReader(id);
			}
		}
		serverWriteObject = js;
	}

}
