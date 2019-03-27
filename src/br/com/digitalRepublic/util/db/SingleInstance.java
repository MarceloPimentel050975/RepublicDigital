package br.com.digitalRepublic.util.db;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Garante que somente uma inst�ncia da aplica��o pode rodar num 
 * diret�rio. Invocando o m�todo {@link #manageApplication()}, um arquivo
 * chamado ".lock" � criado no diret�rio atual. Se este arquivo n�o puder ser criado,
 * entende-se que j� h� aplica��o rodando no diret�rio atual (n�o necessariamente
 * a mesma). Ao finalizar a aplica��o o arquivo � removido.
 */
public final class SingleInstance {

    private static final Log LOG = LogFactory.getLog(SingleInstance.class);
    
    private static boolean managing;
    private static FileOutputStream lockStream;
    private File lockFile;
    private FileLock lock;
    private FileChannel lockChannel;
    
    public static void manageApplication() throws Exception {
	if (!managing) {
	    new SingleInstance();
	    managing = true;
	}
    }
    
    private SingleInstance() throws Exception {
	lockFile = new File(".lock");
	acquireLock();
	Runtime.getRuntime().addShutdownHook(new ShutdownHook());
	new ExitChecker().start();
    }

    private void acquireLock() throws FileNotFoundException, IOException,
	    Exception {
	if (lockStream != null) {
	    try {
		lockStream.close();
	    } catch (Exception e) { }
	    lockStream = null;
	    lockChannel = null;
	}
	lockStream = new FileOutputStream(lockFile);
	lockChannel = lockStream.getChannel();
	lock = lockChannel.tryLock();
	if (lock == null) {
	    throw new Exception("Unable to acquire lock on " + lockFile.getAbsolutePath());
	}
    }
    
    private class ExitChecker extends Thread {
	public ExitChecker() {
	    super(ExitChecker.class.getName());
	    setDaemon(true);
	}
	
	public void run() {
	    while (true) {
		try {
		    if (!lockFile.isFile()) {
			acquireLock();
		    }
		    
		    File killer = new File(".kill");
		    if (killer.isFile()) {
			killer.delete();
			System.exit(0);
		    }
		} catch (Exception e) {
		    LOG.warn(".kill is locked for writing, we cannot exit " +
		    		"until it can be removed.", e);
		}
		synchronized (this) {
		    try {
			wait(1000);
		    } catch (InterruptedException interrupted) {
			if (LOG.isDebugEnabled()) {
			    LOG.debug("Interrupt signal received", interrupted);
			}
		    }
		}
	    }
	}
    }
    
    private class ShutdownHook extends Thread {
	public ShutdownHook() {
	    super(ShutdownHook.class.getName());
	    setDaemon(false);
	}
	public void run() {
	    try {
		lock.release();
		lockStream.close();
		lockFile.delete();
	    } catch (Exception e) {
		LOG.warn("Cannot remove lock file", e);
	    }
	}
    }
    
}
