package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Grid;
import fr.imta.smartgrid.model.Person;
import fr.imta.smartgrid.model.Sensor;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;

public class PersonCreateHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public PersonCreateHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            // Get request body
            JsonObject body = context.getBodyAsJson();
            if (body == null) {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject().put("error", "Invalid JSON body").encode());
                return;
            }
            
            // Check for mandatory fields
            if (!body.containsKey("first_name") || !body.containsKey("last_name") || !body.containsKey("grid")) {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject().put("error", "Missing required fields: first_name, last_name, grid").encode());
                return;
            }
            
            // Get grid
            Integer gridId = body.getInteger("grid");
            Grid grid = db.find(Grid.class, gridId);
            if (grid == null) {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject().put("error", "Grid not found").encode());
                return;
            }
            
            // Create new person
            Person person = new Person();
            person.setFirstName(body.getString("first_name"));
            person.setLastName(body.getString("last_name"));
            person.setGrid(grid);
            
            // Add owned sensors if present
            if (body.containsKey("owned_sensors")) {
                JsonArray sensorIds = body.getJsonArray("owned_sensors");
                List<Sensor> sensors = new ArrayList<>();
                
                for (int i = 0; i < sensorIds.size(); i++) {
                    Integer sensorId = sensorIds.getInteger(i);
                    Sensor sensor = db.find(Sensor.class, sensorId);
                    if (sensor != null) {
                        sensors.add(sensor);
                    }
                }
                
                person.setSensors(sensors);
            }
            
            // Start transaction
            db.getTransaction().begin();
            
            // Save person
            db.persist(person);
            
            // Commit transaction
            db.getTransaction().commit();
            
            // Return success response with new person ID
            context.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("id", person.getId()).encode());
        } catch (Exception e) {
            // Rollback transaction if active
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject().put("error", "Internal server error: " + e.getMessage()).encode());
        }
    }
}
