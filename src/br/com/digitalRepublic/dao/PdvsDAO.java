package br.com.digitalRepublic.dao;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import br.com.digitalRepublic.entity.pdvs;


/**
 * Classe responsável pelo Direct Acess Object para manipulação de dados no repositório.
 * @author marcelo
 *
 */
public class PdvsDAO extends AbstractDAO {

	private EntityManager em = getEntityManager();
	private static final Log LOG = LogFactory.getLog(PdvsDAO.class);
	
	/**
	 * Metodo para buscar o PDV por id
	 * @param id - Tipo String - id de identificação do objeto
	 * @return Retorna um objeto do tipo PDVS.
	 */
	 public pdvs buscaPDVPorID(String id) 
	 {
			try {
				beginTransaction(em);
				pdvs pdvs = em.find(pdvs.class, Integer.valueOf(id));
				return pdvs;
			} catch(NoResultException e)  { 
				return null;
			} finally {
				flush(em);
			}
	  }
	
	
	
	/**
	 * Metodo para buscar se já existe um cnpj cadastrado no repositório.
	 * @param cnpj - Tipo String - parãmetro de busca para pesquisa.
	 * @return boolean - já existe um cnpj já cadastrado
	 */
	 public boolean buscaJaExisteCNPJ(String cnpj) 
	 {
			try {
				beginTransaction(em);
				Query query = em.createQuery("select p FROM pdvs p where p.document='"+cnpj+"'");
				if(query.getMaxResults()>0)
				{
					return true;
				}
				else
				{
					return false;	
				}
			} catch(NoResultException e)  { 
				return false;
			} finally {
				flush(em);
			}
	  }
	 

	/**
	 * Metodo salvarDados para salvar o objeto de dados.
	 * @param pdvs Tipo entidade pdvs para salvar na tabela no repositório.
	 */
	public void salvarDados(pdvs pdvs) {
	try {
	    	beginTransaction(em);
	    	if (pdvs.getId() != 0) 
		    {
	    		em.merge(pdvs);
		    }
		    else
		    {
		      em.persist(pdvs);
		    }
		    commitTransaction(em);
		} catch (RuntimeException ex) {
		    rollBack(em);
		    throw ex;
		}finally 
		{
			flush(em);
		}
    }
}
