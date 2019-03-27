package br.com.digitalRepublic.entity;

import java.io.Serializable;
import javax.persistence.*;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;
import java.util.List;


/**
 * The persistent class for the Cliente database table.
 * 
 */
@Entity
@Table(name = "pdvs", catalog = "testeDigitalRepublic")
public class pdvs implements Serializable {

	private static final long serialVersionUID = 5973030089748250567L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private int id;

	private String tradingName;

	private String ownerName;
	
	private String document;
		
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTradingName() {
		return tradingName;
	}

	public void setTradingName(String tradingName) {
		this.tradingName = tradingName;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public String getDocument() {
		return document;
	}

	public void setDocument(String document) {
		this.document = document;
	}

	@Override
	public String toString() {
		return "pdvs [id=" + id + ", tradingName=" + tradingName + ", ownerName=" + ownerName + ", document=" + document
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		pdvs other = (pdvs) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
}