package br.com.digitalRepublic.apirest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.json.JSONObject;

import com.google.gson.Gson;
import antlr.ASdebug.IASDebugStream;
import br.com.digitalRepublic.dao.PdvsDAO;
import br.com.digitalRepublic.entity.pdvs;

@Path("/backend")
public class ApiRest {

	private static Logger logger = Logger.getLogger(ApiRest.class);
	
	private PdvsDAO createPDVDAO() {
		PdvsDAO servicedao = new PdvsDAO();
		return servicedao;
	}

	@POST
	@Path("/createPDV")
	@Consumes("application/json")
    @Produces("application/json")
	public Response createPDV(pdvs pdvs) {
		logger.info("Projeto digital republic - Classe ApiRest - metodo createPDV - ");

		if(createPDVDAO().buscaJaExisteCNPJ(pdvs.getDocument())) 
		{
			
		}else 
		{
			createPDVDAO().salvarDados(pdvs);
		}
		
		return Response.status(200).entity("ok").build();
	}
	
	
	@GET
	@Path("/getPDV/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public pdvs getPDV(@PathParam("id") String id) {
		logger.info("Projeto digital republic - "
				+ "Classe ApiRest - "
				+ "metodo checkLoginCliente - paramentros: [id]:"
				+ id);
		pdvs pdvs = createPDVDAO().buscaPDVPorID(id);
		return pdvs;
	}
	
	@GET
	@Path("/searchPDV/{lnt}/{lat}")
	@Produces(MediaType.APPLICATION_JSON)
	public pdvs searchPDV(@PathParam("lnt") String lnt,@PathParam("lat") String lat) {
		logger.info("Projeto digital republic - Classe ApiRest - "
				+ "metodo checkLoginCliente - paramentros: [lnt]:"
				+ lnt+" [lat]: "+lat);
		return null;

	}
}
