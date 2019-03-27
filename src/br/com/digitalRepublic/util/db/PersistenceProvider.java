package br.com.digitalRepublic.util.db;


import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Centraliza a configura��o do mecanismo de persist�ncia usado.
 */
public final class PersistenceProvider {
    /*
     * hibernate.transaction.factory_class =
     * org.hibernate.transaction.JDBCTransactionFactory
     * hibernate.current_session_context_class = thread
     */
    private static final Log LOG = LogFactory.getLog(PersistenceProvider.class);
    private static final EntityManagerFactory factory;

    static {
	try {
	    factory = Persistence.createEntityManagerFactory("default");
	} catch (Throwable ex) {
	    LOG.error("Initial SessionFactory creation failed.", ex);
	    throw new ExceptionInInitializerError(ex);
	}
    }

    private PersistenceProvider() {
    }

    /**
     * Retorna a <em>factory</em> de gerenciadores de persist�ncia em uso.
     * @return a <em>factory</em> de gerenciadores de persist�ncia em uso.
     */
    public static EntityManagerFactory getEntityManagerFactory() {
	return factory;
    }

}
