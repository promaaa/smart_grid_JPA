package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Grid;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

public class GridProductionHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public GridProductionHandler(EntityManager db) {
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
            
            // Calculate total production by querying the database
            // Get the latest energy produced values from all producers in this grid
            Double totalProduction = (Double) db.createNativeQuery(
                    "SELECT COALESCE(SUM(d.value), 0) " +
                    "FROM datapoint d " +
                    "JOIN measurement m ON d.measurement = m.id " +
                    "JOIN sensor s ON m.sensor = s.id " +
                    "JOIN producer p ON p.id = s.id " +
                    "WHERE s.grid = ?1 " +
                    "AND m.name = 'total_energy_produced' " +
                    "AND d.timestamp = (SELECT MAX(d2.timestamp) FROM datapoint d2 WHERE d2.measurement = d.measurement)")
                    .setParameter(1, gridId)
                    .getSingleResult();
            
            // Return response
            context.response()
                    .putHeader("content-type", "application/json")
                    .end(String.valueOf(totalProduction));
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
