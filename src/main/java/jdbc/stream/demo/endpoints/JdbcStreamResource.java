package jdbc.stream.demo.endpoints;

import com.fasterxml.jackson.core.JsonGenerator;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jdbc.stream.demo.entity.User;
import jdbc.stream.demo.service.JdbcStreamService;
import jdbc.stream.demo.util.JPAUtil;
import lombok.extern.java.Log;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;

import java.io.IOException;
import java.io.OutputStream;

@RequestScoped
@Path("/jdbc-stream")
@Log
public class JdbcStreamResource {

    @Inject
    private JdbcStreamService jdbcStreamService;

    /**
     * curl http://localhost:8080/jdbc-stream/oom
     * @return
     */
    @GET
    @Path("/oom")
    @Produces(MediaType.APPLICATION_JSON)
    public Response streamJdbcResultToResponseOOM() {
        return Response.ok(jdbcStreamService.streamJdbcResultToResponseOOM()).build();
    }

    /**
     * curl http://localhost:8080/jdbc-stream/file?fileName=users.json&mode=jpa
     * @param fileName
     * @param mode
     * @return
     */
    @GET
    @Path("/file")
    @Produces(MediaType.APPLICATION_JSON)
    public Response streamJdbcResultToFile(@QueryParam("fileName") String fileName, @QueryParam("mode") String mode) {
        String userHomeDirectory = System.getProperty("user.home");
        jdbcStreamService.streamJdbcResultToFile(userHomeDirectory +"/" + fileName, mode);
        return Response.ok().build();
    }

    /**
     * curl http://localhost:8080/jdbc-stream/response
     * @return
     */
    @GET
    @Path("/response")
    @Produces(MediaType.APPLICATION_JSON)
    public Response streamJdbcResultToResponse() {
        return Response.ok(jdbcStreamService.streamJdbcResultToResponse()).build();
    }
}
