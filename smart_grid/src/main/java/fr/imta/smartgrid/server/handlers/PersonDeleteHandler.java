package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Person;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

public class PersonDeleteHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public PersonDeleteHandler(EntityManager db) {
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
            
            // Start transaction
            db.getTransaction().begin();
            
            // Remove person
            db.remove(person);
            
            // Commit transaction
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
