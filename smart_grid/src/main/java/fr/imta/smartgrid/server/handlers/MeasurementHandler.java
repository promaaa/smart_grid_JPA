package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Measurement;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

public class MeasurementHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public MeasurementHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            // Get measurement ID from path parameter
            String idParam = context.pathParam("id");
            int measurementId = Integer.parseInt(idParam);
            
            // Find the measurement
            Measurement measurement = db.find(Measurement.class, measurementId);
            if (measurement == null) {
                context.response()
                        .setStatusCode(404)
                        .end(new JsonObject().put("error", "Measurement not found").encode());
                return;
            }
            
            // Create JSON response
            JsonObject response = new JsonObject()
                    .put("id", measurement.getId())
                    .put("name", measurement.getName())
                    .put("unit", measurement.getUnit());
            
            // Add sensor ID if present
            if (measurement.getSensor() != null) {
                response.put("sensor", measurement.getSensor().getId());
            }
            
            // Return response
            context.response()
                    .putHeader("content-type", "application/json")
                    .end(response.encode());
        } catch (NumberFormatException e) {
            context.response()
                    .setStatusCode(400)
                    .end(new JsonObject().put("error", "Invalid measurement ID").encode());
        } catch (Exception e) {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject().put("error", "Internal server error: " + e.getMessage()).encode());
        }
    }
}
