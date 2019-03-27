package br.com.digitalRepublic.util.db;


import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class BusinessObjectFactory {
    /* FIXME gerenciar conex�es perdidas usando a ajuda do garbage collector */

    private static final Log LOG = LogFactory.getLog(BusinessObjectFactory.class);
    private static final ThreadLocal<EntityManager> localEntityManager = new ThreadLocal<EntityManager>();
    private static final EntityManagerFactory factory;

    static {
	try {
	    factory = Persistence.createEntityManagerFactory("default");
	} catch (Throwable ex) {
	    LOG.error("Initial SessionFactory creation failed.", ex);
	    throw new ExceptionInInitializerError(ex);
	}
    }

    private BusinessObjectFactory() {
    }

    public static <T extends BusinessObject> T getInstance(Class<T> type) {
	T instance;
	try {
	    instance = type.newInstance();
	} catch (RuntimeException e) {
	    throw e;
	} catch (Exception e) {
	    throw new IllegalStateException(
		    "Exception thrown by business object constructor", e);
	}
	EntityManager em = localEntityManager.get();
	if (em == null) {
	    // devemos iniciar uma transa��o? acho que n�o: quem fecha?
	    beginTransaction();
	    em = localEntityManager.get();
	    throw new IllegalStateException("No active transaction");
	}
	instance.setEntityManager(em);
	return instance;
    }

    public static void beginTransaction() {
	EntityManager em = factory.createEntityManager();
	//em.setFlushMode(FlushModeType.COMMIT);
	localEntityManager.set(em);
	EntityTransaction transaction = em.getTransaction();
	transaction.begin();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Began transaction " + transaction);
        }
    }

    public static void commitTransaction() {
	EntityManager em = localEntityManager.get();
	if (em == null)
	    throw new IllegalStateException("No active transaction");
	EntityTransaction transaction = em.getTransaction();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Commit transaction " + transaction);
        }
	transaction.commit();
	localEntityManager.set(null);
    }

    public static void rollbackTransaction() {
	setRollbackOnly();
	endTransaction();
    }

    public static void setRollbackOnly() {
	EntityManager em = localEntityManager.get();
	if (em == null) {
	    throw new IllegalStateException("No active transaction");
	}
	EntityTransaction transaction = em.getTransaction();
	transaction.setRollbackOnly();
    }

    public static void endTransaction() {
	EntityManager em = localEntityManager.get();
	if (em == null) {
	    return;
	}
	EntityTransaction transaction = em.getTransaction();
	if (transaction == null || !transaction.isActive()) {
	    return;
	}
	if (transaction.getRollbackOnly()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Rollback transaction " + transaction);
            }
	    transaction.rollback();
	}
	else {
	    if (LOG.isDebugEnabled()) {
	        LOG.debug("Commit transaction " + transaction);
	    }
	    transaction.commit();
	}
	localEntityManager.set(null);
    }

    public static synchronized EntityManager getEntityManager() {
	return localEntityManager.get();
    }

}
