package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.DataPoint;
import fr.imta.smartgrid.model.Measurement;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class MeasurementValuesHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public MeasurementValuesHandler(EntityManager db) {
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
            
            // Get optional query parameters for time range
            String fromParam = context.request().getParam("from");
            String toParam = context.request().getParam("to");
            
            long fromTimestamp = fromParam != null ? Long.parseLong(fromParam) : 0;
            long toTimestamp = toParam != null ? Long.parseLong(toParam) : Integer.MAX_VALUE;
            
            // Query datapoints in the specified time range
            TypedQuery<DataPoint> query = db.createQuery(
                    "SELECT dp FROM DataPoint dp WHERE dp.measurement = :measurement " +
                    "AND dp.timestamp >= :fromTime AND dp.timestamp <= :toTime " +
                    "ORDER BY dp.timestamp", DataPoint.class)
                    .setParameter("measurement", measurement)
                    .setParameter("fromTime", fromTimestamp)
                    .setParameter("toTime", toTimestamp);
            
            List<DataPoint> datapoints = query.getResultList();
            
            // Create JSON response
            JsonObject response = new JsonObject()
                    .put("sensor_id", measurement.getSensor().getId())
                    .put("measurement_id", measurement.getId());
            
            JsonArray values = new JsonArray();
            for (DataPoint datapoint : datapoints) {
                values.add(new JsonObject()
                        .put("timestamp", datapoint.getTimestamp())
                        .put("value", datapoint.getValue()));
            }
            
            response.put("values", values);
            
            // Return response
            context.response()
                    .putHeader("content-type", "application/json")
                    .end(response.encode());
        } catch (NumberFormatException e) {
            context.response()
                    .setStatusCode(400)
                    .end(new JsonObject().put("error", "Invalid parameter").encode());
        } catch (Exception e) {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject().put("error", "Internal server error: " + e.getMessage()).encode());
        }
    }
}
