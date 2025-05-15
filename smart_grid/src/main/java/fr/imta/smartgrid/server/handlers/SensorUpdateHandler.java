package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.*;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;

public class SensorUpdateHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public SensorUpdateHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            // Get sensor ID from path parameter
            String idParam = context.pathParam("id");
            int sensorId = Integer.parseInt(idParam);
            
            // Get request body
            JsonObject body = context.getBodyAsJson();
            if (body == null) {
                context.response()
                        .setStatusCode(400)
                        .end(new JsonObject().put("error", "Invalid JSON body").encode());
                return;
            }
            
            // Find the sensor
            Sensor sensor = db.find(Sensor.class, sensorId);
            if (sensor == null) {
                context.response()
                        .setStatusCode(404)
                        .end(new JsonObject().put("error", "Sensor not found").encode());
                return;
            }
            
            // Start transaction
            db.getTransaction().begin();
            
            // Update common sensor fields if present in JSON
            if (body.containsKey("name")) {
                sensor.setName(body.getString("name"));
            }
            
            if (body.containsKey("description")) {
                sensor.setDescription(body.getString("description"));
            }
            
            // Update owners if present
            if (body.containsKey("owners")) {
                JsonArray ownerIds = body.getJsonArray("owners");
                List<Person> owners = new ArrayList<>();
                
                for (int i = 0; i < ownerIds.size(); i++) {
                    Integer ownerId = ownerIds.getInteger(i);
                    Person owner = db.find(Person.class, ownerId);
                    if (owner != null) {
                        owners.add(owner);
                    }
                }
                
                sensor.setOwners(owners);
            }
            
            // Update producer-specific fields
            if (sensor instanceof Producer) {
                Producer producer = (Producer) sensor;
                
                if (body.containsKey("power_source")) {
                    producer.setPowerSource(body.getString("power_source"));
                }
                
                // Update solar panel-specific fields
                if (producer instanceof SolarPanel && body.containsKey("efficiency")) {
                    ((SolarPanel) producer).setEfficiency(body.getDouble("efficiency").floatValue());
                }
                
                // Update wind turbine-specific fields
                if (producer instanceof WindTurbine) {
                    if (body.containsKey("height")) {
                        ((WindTurbine) producer).setHeight(body.getDouble("height"));
                    }
                    
                    if (body.containsKey("blade_length")) {
                        ((WindTurbine) producer).setBladeLength(body.getDouble("blade_length"));
                    }
                }
            }
            
            // Update consumer-specific fields
            if (sensor instanceof Consumer) {
                Consumer consumer = (Consumer) sensor;
                
                if (body.containsKey("max_power")) {
                    consumer.setMaxPower(body.getDouble("max_power"));
                }
                
                // Update EV charger-specific fields
                if (consumer instanceof EVCharger) {
                    EVCharger evCharger = (EVCharger) consumer;
                    
                    if (body.containsKey("type")) {
                        evCharger.setType(body.getString("type"));
                    }
                    
                    if (body.containsKey("voltage")) {
                        evCharger.setVoltage(body.getInteger("voltage"));
                    }
                    
                    if (body.containsKey("maxAmp")) {
                        evCharger.setMaxAmp(body.getInteger("maxAmp"));
                    }
                }
            }
            
            // Save changes
            db.persist(sensor);
            db.getTransaction().commit();
            
            // Return success response
            context.response()
                    .setStatusCode(200)
                    .end(new JsonObject().put("status", "success").encode());
        } catch (NumberFormatException e) {
            context.response()
                    .setStatusCode(400)
                    .end(new JsonObject().put("error", "Invalid sensor ID").encode());
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
