package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.*;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

public class SensorHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public SensorHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            // Get sensor ID from path parameter
            String idParam = context.pathParam("id");
            int sensorId = Integer.parseInt(idParam);
            
            // Find the sensor
            Sensor sensor = db.find(Sensor.class, sensorId);
            if (sensor == null) {
                context.response()
                        .setStatusCode(404)
                        .end(new JsonObject().put("error", "Sensor not found").encode());
                return;
            }
            
            // Create JSON response with base sensor data
            JsonObject response = new JsonObject()
                    .put("id", sensor.getId())
                    .put("name", sensor.getName())
                    .put("description", sensor.getDescription());
            
            // Determine the sensor kind
            String kind = getSensorKind(sensor);
            response.put("kind", kind);
            
            // Add grid ID if present
            if (sensor.getGrid() != null) {
                response.put("grid", sensor.getGrid().getId());
            }
            
            // Add available measurements
            JsonArray measurements = new JsonArray();
            sensor.getMeasurements().forEach(measurement -> measurements.add(measurement.getId()));
            response.put("available_measurements", measurements);
            
            // Add owners
            JsonArray owners = new JsonArray();
            sensor.getOwners().forEach(owner -> owners.add(owner.getId()));
            response.put("owners", owners);
            
            // Add specific fields based on sensor type
            addSpecificFields(sensor, response);
            
            // Return response
            context.response()
                    .putHeader("content-type", "application/json")
                    .end(response.encode());
        } catch (NumberFormatException e) {
            context.response()
                    .setStatusCode(400)
                    .end(new JsonObject().put("error", "Invalid sensor ID").encode());
        } catch (Exception e) {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject().put("error", "Internal server error: " + e.getMessage()).encode());
        }
    }

    private String getSensorKind(Sensor sensor) {
        if (sensor instanceof SolarPanel) {
            return "SolarPanel";
        } else if (sensor instanceof WindTurbine) {
            return "WindTurbine";
        } else if (sensor instanceof EVCharger) {
            return "EVCharger";
        } else if (sensor instanceof Producer) {
            return "Producer";
        } else if (sensor instanceof Consumer) {
            return "Consumer";
        } else {
            return "Sensor";
        }
    }

    private void addSpecificFields(Sensor sensor, JsonObject response) {
        // For Producer
        if (sensor instanceof Producer) {
            Producer producer = (Producer) sensor;
            response.put("power_source", producer.getPowerSource());
            
            // For SolarPanel
            if (sensor instanceof SolarPanel) {
                SolarPanel solarPanel = (SolarPanel) sensor;
                response.put("efficiency", solarPanel.getEfficiency());
            }
            
            // For WindTurbine
            else if (sensor instanceof WindTurbine) {
                WindTurbine windTurbine = (WindTurbine) sensor;
                response.put("height", windTurbine.getHeight());
                response.put("blade_length", windTurbine.getBladeLength());
            }
        }
        
        // For Consumer
        else if (sensor instanceof Consumer) {
            Consumer consumer = (Consumer) sensor;
            response.put("max_power", consumer.getMaxPower());
            
            // For EVCharger
            if (sensor instanceof EVCharger) {
                EVCharger evCharger = (EVCharger) sensor;
                response.put("voltage", evCharger.getVoltage());
                response.put("maxAmp", evCharger.getMaxAmp());
                response.put("type", evCharger.getType());
            }
        }
    }
}
