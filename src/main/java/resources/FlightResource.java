package resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import flight.model.Flight;
import flight.voegol.VoeGol;
import flight.voegol.exception.SearchException;

@Path("/flight")
public class FlightResource {

	@GET
	@Path("/voegol")
	@Produces(MediaType.APPLICATION_JSON)
	public Response voeGol(
			@QueryParam("from") String from, 
			@QueryParam("to") String to,
			@QueryParam("dayDep") int dayDep, 
			@QueryParam("monthDep") int monthDep, 
			@QueryParam("yearDep") int yearDep,
			@QueryParam("dayArr") int dayArr, 
			@QueryParam("monthArr") int monthArr, 
			@QueryParam("yearArr") int yearArr,
			@QueryParam("adult") int adult, 
			@QueryParam("child") int child) {

		// TODO tratar os parâmetros

		VoeGol voegol = new VoeGol(from, to, dayDep, monthDep, yearDep, dayArr, monthArr, yearArr, adult, child);
		try {
			Flight flight = voegol.search();
			
			//Converter POJO em JSON string
			GsonBuilder builder = new GsonBuilder();
			Gson gson = builder.create();
			String json = gson.toJson(flight);
			
			return Response.status(Response.Status.OK).entity(json).build();
		} catch (SearchException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

	}

}
