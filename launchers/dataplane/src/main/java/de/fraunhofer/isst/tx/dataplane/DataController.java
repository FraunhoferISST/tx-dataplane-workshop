package de.fraunhofer.isst.tx.dataplane;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import static java.lang.String.format;

@Path("/")
public class DataController {

    @GET
    @Path("/download")
    public Response downloadData() {
        return Response.ok("this is the dummy data :-)").build();
    }

    @GET
    @Path("/download/{id}")
    public Response downloadData(@PathParam("id") String id) {
        return Response.ok(format("You requested data with ID: %s", id)).build();
    }
}
