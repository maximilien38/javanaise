package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {
	
	enum Verrou {NL, RC, WC, R, W, RWC};
	
	int id;
	Verrou state;
	Serializable objetPartage;
	
	public JvnObjectImpl(Serializable o, int id)
	{
		objetPartage = o;
		state = Verrou.NL;
		this.id = id;
	}

	@Override
	public void jvnLockRead() throws JvnException {
		
		if(state == Verrou.NL || state == Verrou.W)
		{
			//entry consistency : on récupère les objets à l'entrée dans la prise du lock
			objetPartage = JvnServerImpl.jvnGetServer().jvnLockRead(id);
			state = Verrou.R;
		}
		else if(state == Verrou.RC)
		{
			state = Verrou.R;
		}
		else if(state == Verrou.WC)
		{
			state = Verrou.RWC;
		}
		else if(state == Verrou.R)
		{
			//on a déjà le verrou en lecture on ne fait donc rien
		}
		else
		{
			//RWC
			//on a déjà le verrou en écriture donc on peut aussi lire
		}
		
	}

	@Override
	public void jvnLockWrite() throws JvnException {
		if(state == Verrou.NL || state == Verrou.RC || state == Verrou.R)
		{
			//entry consistency : on récupère les objets à l'entrée dans la prise du lock
			objetPartage = JvnServerImpl.jvnGetServer().jvnLockWrite(id);
			state = Verrou.W;
		}
		else if(state == Verrou.WC)
		{
			state = Verrou.W;
		}
		else if(state == Verrou.W)
		{
			//on a déjà le verrou en écriture on ne fait donc rien
		}
		else
		{
			//RWC
			//on a déjà le verrou en écriture donc on peut le passé en écriture
			state = Verrou.W;
		}
		
	}

	@Override
	public synchronized void jvnUnLock() throws JvnException {
		if(state == Verrou.NL || state == Verrou.RC || state == Verrou.WC)
		{
			//erreur car on à pas de verrou et on utilise UnLock
			throw new JvnException("UnLock but not lock ! (verrou : "+state+")");
		}
		else if(state == Verrou.R)
		{
			state = Verrou.RC;
			notify();
		}
		else
		{
			//verrou en W OU RWC
			state = Verrou.WC;
			notify();
		}
		
	}

	@Override
	public int jvnGetObjectId() throws JvnException {
		return id;
	}

	@Override
	public Serializable jvnGetObjectState() throws JvnException {
		return objetPartage;
	}

	@Override
	public synchronized void jvnInvalidateReader() throws JvnException {
		if(state == Verrou.NL || state == Verrou.WC || state == Verrou.W)
		{
			//on est sensé être entrain de lire donc si ce n'est pas le cas c'est une erreur
			throw new JvnException("invalidateReader but not Read ! (verrou : "+state+")");
		}
		else if(state == Verrou.RC)
		{
			//si on avait déjà laché le vérrou en lecture
			state = Verrou.NL;
		}
		else
		{
			//Verrou.R	ou Verrou.RWC
			try {
				wait();
				state = Verrou.NL;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}

	@Override
	public synchronized Serializable jvnInvalidateWriter() throws JvnException {
		if(state == Verrou.NL || state == Verrou.RC || state == Verrou.R)
		{
			//on est sensé être entrain d'écrire donc si ce n'est pas le cas c'est une erreur
			throw new JvnException("invalidateWriter but not Write ! (verrou : "+state+")");
		}
		else if(state == Verrou.WC)
		{
			//si on avait déjà laché le verrou en écriture
			state = Verrou.NL;
		}
		else
		{
			//Verrou.W ou Verrou.RWC (car la personne qui lit n'a pas fini)
			//Il faut attendre que l'ecriture se finisse.
			try {
				wait();
				state = Verrou.NL;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			state = Verrou.NL;
		}
		return objetPartage;
	}

	@Override
	public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
		if(state == Verrou.NL || state == Verrou.RC || state == Verrou.R)
		{
			//on est sensé être entrain d'écrire donc si ce n'est pas le cas c'est une erreur
			throw new JvnException("invalidateWriter but not Write ! (verrou : "+state+")");
		}
		else if(state == Verrou.WC || state == Verrou.RWC)
		{
			//si on avait déjà laché le verrou en écriture
			state = Verrou.RC;
		}
		else
		{
			//Verrou.W
			//Il faut attendre que l'ecriture se finisse.
			try {
				wait();
				state = Verrou.NL;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			state = Verrou.NL;
		}
		return objetPartage;
	}

}
