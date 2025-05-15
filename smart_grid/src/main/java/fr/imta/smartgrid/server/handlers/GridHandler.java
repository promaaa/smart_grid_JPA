package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Grid;
import fr.imta.smartgrid.model.Person;
import fr.imta.smartgrid.model.Sensor;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

import java.util.stream.Collectors;

public class GridHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public GridHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            // Get grid ID from path parameter
            String idParam = context.pathParam("id");
            int gridId = Integer.parseInt(idParam);
            
            // Find the grid
            Grid grid = db.find(Grid.class, gridId);
            if (grid == null) {
                context.response()
                        .setStatusCode(404)
                        .end(new JsonObject().put("error", "Grid not found").encode());
                return;
            }
            
            // Create JSON response
            JsonObject response = new JsonObject()
                    .put("id", grid.getId())
                    .put("name", grid.getName())
                    .put("description", grid.getDescription());
            
            // Add user IDs
            JsonArray users = new JsonArray();
            grid.getPersons().forEach(person -> users.add(person.getId()));
            response.put("users", users);
            
            // Add sensor IDs
            JsonArray sensors = new JsonArray();
            grid.getSensors().forEach(sensor -> sensors.add(sensor.getId()));
            response.put("sensors", sensors);
            
            // Return response
            context.response()
                    .putHeader("content-type", "application/json")
                    .end(response.encode());
        } catch (NumberFormatException e) {
            context.response()
                    .setStatusCode(400)
                    .end(new JsonObject().put("error", "Invalid grid ID").encode());
        } catch (Exception e) {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject().put("error", "Internal server error: " + e.getMessage()).encode());
        }
    }
}
