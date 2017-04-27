package resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import flight.latam.Latam;
import flight.model.Flight;
import flight.voegol.VoeGol;
import flight.voegol.exception.SearchException;

@Path("/flight")
public class FlightResource {

	@GET
	@Path("/gol")
	@Produces(MediaType.APPLICATION_JSON)
	public Response voeGol(
			@QueryParam("from") String from,
			@QueryParam("to") String to,
			@QueryParam("dep") String dep,
			@QueryParam("ret") String ret,
			@QueryParam("adult") int adult,
			@QueryParam("child") int child) {

		//TODO Validação: as datas devem estar no formato dd/mm/yyyy
		
		Flight findFlight = new Flight(from, to, dep, ret, adult, child);
		VoeGol voegol = new VoeGol(findFlight);
		try {
			Flight flight = voegol.search();

			// Converter POJO em JSON string
			GsonBuilder builder = new GsonBuilder();
			Gson gson = builder.create();
			String json = gson.toJson(flight);

			return Response.status(Response.Status.OK).entity(json).build();
		} catch (SearchException e) {
			System.err.println("Erro: " + e.getMessage());
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("/latam")
	@Produces(MediaType.APPLICATION_JSON)
	public Response latam(
			@QueryParam("from") String from,
			@QueryParam("to") String to,
			@QueryParam("dep") String dep,
			@QueryParam("ret") String ret,
			@QueryParam("adult") int adult,
			@QueryParam("child") int child) {

		//TODO Validação: as datas devem estar no formato dd/mm/yyyy
		
		Flight findFlight = new Flight(from, to, dep, ret, adult, child);
		Latam latam = new Latam(findFlight);
		try {
			Flight flight = latam.search();

			// Converter POJO em JSON string
			GsonBuilder builder = new GsonBuilder();
			Gson gson = builder.create();
			String json = gson.toJson(flight);

			return Response.status(Response.Status.OK).entity(json).build();
		} catch (SearchException e) {
			System.err.println("Erro: " + e.getMessage());
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}
}
