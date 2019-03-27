package br.com.digitalRepublic.dao;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.LogFactory;

import br.com.digitalRepublic.util.db.ReflectionHelper;


/**
 * Classe Abstrata respons�vel por criar um Singleton de
 * EntityManagerFactory, e prover a criacao de EntityManager,
 * assim como simplificar o tamanho da codifica��o da chamada de
 * alguns m�todos b�sicos que ser�o sempre utilizados pelo EntityManager. 
 * @author vntfaga
 *
 */
public abstract class AbstractDAO {

private static EntityManagerFactory emf = null;

    /**
     * M�todo protegido que prove um Singleton de EntityManagerFactory e
     * retorna instancias de EntityManager
     * @return <b>EntityManager</b> emf.createEntityManager
     */
    protected static EntityManager getEntityManager() {
	if (emf == null) {
	    emf = Persistence.createEntityManagerFactory("default");
	}
	return emf.createEntityManager();
    }

    /**
     * M�todo protegido que recebe um EntityManager como parametro
     * e faz com que ele inicie uma transa��o
     * @param entityManager
     */
    protected void beginTransaction(EntityManager entityManager) {
	entityManager.getTransaction().begin();
    }

    /**
     * M�todo protegido que recebe um EntityManager como parametro
     * e faz com que ele encerre uma transa��o
     * @param entityManager
     */
    protected void commitTransaction(EntityManager entityManager) {
	entityManager.getTransaction().commit();
    }

    /**
     * M�todo protegido que recebe um EntityManager como parametro
     * e faz com que ele fa�a rollback de uma transa��o falha
     * @param entityManager
     */
    protected void rollBack(EntityManager entityManager) {
	entityManager.getTransaction().rollback();
    }

    /**
     * M�todo protegido que recebe um EntityManager como parametro
     * e faz com que ele se feche
     * @param entityManager
     */
    protected void flush(EntityManager entityManager) {
	entityManager.close();
    }

    
    @SuppressWarnings("unchecked")
    public <T> T refresh(T instance) {
        ReflectionHelper<T> helper = new ReflectionHelper<T>(instance);
        Serializable id = helper.getId();

        ReflectionHelper<Serializable> pkHelper = new ReflectionHelper<Serializable>(id);
        if (pkHelper.isEmbeddable()) {
            try {
            id = refreshPK(id, pkHelper);
            } catch (Exception e) {
                LogFactory.getLog(getClass()).error("Unable to refresh " + id, e);
            }
        }
        EntityManager entityManager = getEntityManager();
        beginTransaction(entityManager);
        T managed = (T)entityManager.getReference(helper.getTargetClass(), id);
        if (managed == null)
            managed = entityManager.find((Class<T>)instance.getClass(), id);
//        commitTransaction(entityManager);
//        flush(entityManager);
        return (managed == null ? instance : managed);
    }

    private Serializable refreshPK(Serializable pk,
            ReflectionHelper<Serializable> pkHelper) throws Exception {
        List<String> refreshProperties = pkHelper.toOneRelationships();
        for (String property : refreshProperties) {
            Object value = PropertyUtils.getProperty(pk, property);
            value = refresh(value);
            PropertyUtils.setProperty(pk, property, value);
        }
        return pk;
    }

}