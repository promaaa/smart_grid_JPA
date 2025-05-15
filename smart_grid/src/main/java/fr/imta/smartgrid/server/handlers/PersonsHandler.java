package fr.imta.smartgrid.server.handlers;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

import java.util.List;

public class PersonsHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public PersonsHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            // Get all person IDs
            List<Integer> personIds = db.createQuery("SELECT p.id FROM Person p", Integer.class)
                    .getResultList();
            
            // Create JSON response
            JsonArray response = new JsonArray();
            personIds.forEach(response::add);
            
            // Return response
            context.response()
                    .putHeader("content-type", "application/json")
                    .end(response.encode());
        } catch (Exception e) {
            context.response()
                    .setStatusCode(500)
                    .end("Internal server error: " + e.getMessage());
        }
    }
}
