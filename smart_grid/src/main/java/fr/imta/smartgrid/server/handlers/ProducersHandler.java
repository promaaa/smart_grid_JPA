package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Producer;
import fr.imta.smartgrid.model.SolarPanel;
import fr.imta.smartgrid.model.WindTurbine;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

import java.util.List;

public class ProducersHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public ProducersHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            // Query for all producers
            List<Producer> producers = db.createQuery("SELECT p FROM Producer p", Producer.class).getResultList();
            
            // Create JSON response
            JsonArray response = new JsonArray();
            
            for (Producer producer : producers) {
                JsonObject producerJson = new JsonObject()
                        .put("id", producer.getId())
                        .put("name", producer.getName())
                        .put("description", producer.getDescription())
                        .put("power_source", producer.getPowerSource());
                
                // Add grid ID if present
                if (producer.getGrid() != null) {
                    producerJson.put("grid", producer.getGrid().getId());
                }
                
                // Add kind based on class
                String kind;
                if (producer instanceof SolarPanel) {
                    kind = "SolarPanel";
                } else if (producer instanceof WindTurbine) {
                    kind = "WindTurbine";
                } else {
                    kind = "Producer";
                }
                producerJson.put("kind", kind);
                
                // Add available measurements
                JsonArray measurements = new JsonArray();
                producer.getMeasurements().forEach(measurement -> measurements.add(measurement.getId()));
                producerJson.put("available_measurements", measurements);
                
                // Add owners
                JsonArray owners = new JsonArray();
                producer.getOwners().forEach(owner -> owners.add(owner.getId()));
                producerJson.put("owners", owners);
                
                // Add specific SolarPanel fields
                if (producer instanceof SolarPanel) {
                    SolarPanel solarPanel = (SolarPanel) producer;
                    producerJson.put("efficiency", solarPanel.getEfficiency());
                }
                
                // Add specific WindTurbine fields
                else if (producer instanceof WindTurbine) {
                    WindTurbine windTurbine = (WindTurbine) producer;
                    producerJson.put("height", windTurbine.getHeight());
                    producerJson.put("blade_length", windTurbine.getBladeLength());
                }
                
                response.add(producerJson);
            }
            
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
