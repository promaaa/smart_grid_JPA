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

public class PersonUpdateHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public PersonUpdateHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            // Get person ID from path parameter
            String idParam = context.pathParam("id");
            int personId = Integer.parseInt(idParam);
            
            // Get request body
            JsonObject body = context.getBodyAsJson();
            if (body == null) {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject().put("error", "Invalid JSON body").encode());
                return;
            }
            
            // Find the person
            Person person = db.find(Person.class, personId);
            if (person == null) {
                context.response()
                        .setStatusCode(404)
                        .end(new JsonObject().put("error", "Person not found").encode());
                return;
            }
            
            // Start transaction
            db.getTransaction().begin();
            
            // Update first name if present
            if (body.containsKey("first_name")) {
                person.setFirstName(body.getString("first_name"));
            }
            
            // Update last name if present
            if (body.containsKey("last_name")) {
                person.setLastName(body.getString("last_name"));
            }
            
            // Update grid if present
            if (body.containsKey("grid")) {
                Integer gridId = body.getInteger("grid");
                if (gridId != null) {
                    Grid grid = db.find(Grid.class, gridId);
                    if (grid != null) {
                        person.setGrid(grid);
                    }
                }
            }
            
            // Update owned sensors if present
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
            
            // Save changes
            db.persist(person);
            db.getTransaction().commit();
            
            // Return success response
            context.response()
                    .setStatusCode(200)
                    .end(new JsonObject().put("status", "success").encode());
        } catch (NumberFormatException e) {
            context.response()
                    .setStatusCode(400)
                    .end(new JsonObject().put("error", "Invalid person ID").encode());
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
