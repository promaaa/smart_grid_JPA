package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Person;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

public class PersonHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public PersonHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            // Get person ID from path parameter
            String idParam = context.pathParam("id");
            int personId = Integer.parseInt(idParam);
            
            // Find the person
            Person person = db.find(Person.class, personId);
            if (person == null) {
                context.response()
                        .setStatusCode(404)
                        .end(new JsonObject().put("error", "Person not found").encode());
                return;
            }
            
            // Create JSON response
            JsonObject response = new JsonObject()
                    .put("id", person.getId())
                    .put("first_name", person.getFirstName())
                    .put("last_name", person.getLastName());
            
            // Add grid ID if present
            if (person.getGrid() != null) {
                response.put("grid", person.getGrid().getId());
            }
            
            // Add owned sensors
            JsonArray ownedSensors = new JsonArray();
            person.getSensors().forEach(sensor -> ownedSensors.add(sensor.getId()));
            response.put("owned_sensors", ownedSensors);
            
            // Return response
            context.response()
                    .putHeader("content-type", "application/json")
                    .end(response.encode());
        } catch (NumberFormatException e) {
            context.response()
                    .setStatusCode(400)
                    .end(new JsonObject().put("error", "Invalid person ID").encode());
        } catch (Exception e) {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject().put("error", "Internal server error: " + e.getMessage()).encode());
        }
    }
}
