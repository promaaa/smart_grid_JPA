package fr.imta.smartgrid.server.handlers;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

import java.util.List;

public class SensorsKindHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public SensorsKindHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            // Get sensor kind from path parameter
            String kind = context.pathParam("kind");
            
            // Query for sensor IDs based on kind
            List<Integer> sensorIds;
            
            if ("SolarPanel".equals(kind)) {
                sensorIds = db.createQuery("SELECT s.id FROM SolarPanel s", Integer.class).getResultList();
            } else if ("WindTurbine".equals(kind)) {
                sensorIds = db.createQuery("SELECT w.id FROM WindTurbine w", Integer.class).getResultList();
            } else if ("EVCharger".equals(kind)) {
                sensorIds = db.createQuery("SELECT e.id FROM EVCharger e", Integer.class).getResultList();
            } else {
                // Return empty array for invalid kind
                sensorIds = List.of();
            }
            
            // Create JSON response
            JsonArray response = new JsonArray();
            sensorIds.forEach(response::add);
            
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
