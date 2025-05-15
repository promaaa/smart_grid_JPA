package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Grid;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

import java.util.List;

public class GridsHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public GridsHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            // Get all grid IDs
            List<Integer> gridIds = db.createQuery("SELECT g.id FROM Grid g", Integer.class)
                    .getResultList();
            
            // Create JSON response
            JsonArray response = new JsonArray();
            gridIds.forEach(response::add);
            
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
