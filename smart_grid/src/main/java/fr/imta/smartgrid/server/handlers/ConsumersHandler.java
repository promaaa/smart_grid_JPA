package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.Consumer;
import fr.imta.smartgrid.model.EVCharger;
import fr.imta.smartgrid.model.Grid;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

public class ConsumersHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public ConsumersHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        String method = context.request().method().name();
        String path = context.request().path();
        String id = context.pathParam("id");
        
        try {
            // Route based on HTTP method and path
            if ("GET".equals(method)) {
                if (id != null) {
                    getConsumer(context, Long.parseLong(id));
                } else {
                    getAllConsumers(context);
                }
            } else if ("POST".equals(method)) {
                createConsumer(context);
            } else if ("PUT".equals(method) && id != null) {
                updateConsumer(context, Long.parseLong(id));
            } else if ("DELETE".equals(method) && id != null) {
                deleteConsumer(context, Long.parseLong(id));
            } else {
                context.response()
                        .setStatusCode(400)
                        .end("Bad request");
            }
        } catch (NumberFormatException e) {
            context.response()
                    .setStatusCode(400)
                    .end("Invalid ID format");
        } catch (Exception e) {
            context.response()
                    .setStatusCode(500)
                    .end("Internal server error: " + e.getMessage());
        }
    }
    
    private void getAllConsumers(RoutingContext context) {
        // Query for all consumers
        List<Consumer> consumers = db.createQuery("SELECT c FROM Consumer c", Consumer.class).getResultList();
        
        // Create JSON response
        JsonArray response = new JsonArray();
        
        for (Consumer consumer : consumers) {
            JsonObject consumerJson = new JsonObject()
                    .put("id", consumer.getId())
                    .put("name", consumer.getName())
                    .put("description", consumer.getDescription())
                    .put("max_power", consumer.getMaxPower());
            
            // Add grid ID if present
            if (consumer.getGrid() != null) {
                consumerJson.put("grid", consumer.getGrid().getId());
            }
            
            // Add kind based on class
            String kind = consumer instanceof EVCharger ? "EVCharger" : "Consumer";
            consumerJson.put("kind", kind);
            
            // Add available measurements
            JsonArray measurements = new JsonArray();
            consumer.getMeasurements().forEach(measurement -> measurements.add(measurement.getId()));
            consumerJson.put("available_measurements", measurements);
            
            // Add owners
            JsonArray owners = new JsonArray();
            consumer.getOwners().forEach(owner -> owners.add(owner.getId()));
            consumerJson.put("owners", owners);
            
            // Add specific EVCharger fields
            if (consumer instanceof EVCharger) {
                EVCharger evCharger = (EVCharger) consumer;
                consumerJson.put("voltage", evCharger.getVoltage());
                consumerJson.put("maxAmp", evCharger.getMaxAmp());
                consumerJson.put("type", evCharger.getType());
            }
            
            response.add(consumerJson);
        }
        
        // Return response
        context.response()
                .putHeader("content-type", "application/json")
                .end(response.encode());
    }
    
    private void getConsumer(RoutingContext context, long id) {
        // Find consumer by ID
        Consumer consumer = db.find(Consumer.class, id);
        
        if (consumer == null) {
            context.response()
                    .setStatusCode(404)
                    .end("Consumer not found");
            return;
        }
        
        // Create JSON response
        JsonObject consumerJson = new JsonObject()
                .put("id", consumer.getId())
                .put("name", consumer.getName())
                .put("description", consumer.getDescription())
                .put("max_power", consumer.getMaxPower());
        
        // Add grid ID if present
        if (consumer.getGrid() != null) {
            consumerJson.put("grid", consumer.getGrid().getId());
        }
        
        // Add kind based on class
        String kind = consumer instanceof EVCharger ? "EVCharger" : "Consumer";
        consumerJson.put("kind", kind);
        
        // Add available measurements
        JsonArray measurements = new JsonArray();
        consumer.getMeasurements().forEach(measurement -> measurements.add(measurement.getId()));
        consumerJson.put("available_measurements", measurements);
        
        // Add owners
        JsonArray owners = new JsonArray();
        consumer.getOwners().forEach(owner -> owners.add(owner.getId()));
        consumerJson.put("owners", owners);
        
        // Add specific EVCharger fields
        if (consumer instanceof EVCharger) {
            EVCharger evCharger = (EVCharger) consumer;
            consumerJson.put("voltage", evCharger.getVoltage());
            consumerJson.put("maxAmp", evCharger.getMaxAmp());
            consumerJson.put("type", evCharger.getType());
        }
        
        // Return response
        context.response()
                .putHeader("content-type", "application/json")
                .end(consumerJson.encode());
    }
    
    private void createConsumer(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            
            Consumer consumer;
            String kind = body.getString("kind", "Consumer");
            
            // Create the appropriate consumer type
            if ("EVCharger".equals(kind)) {
                consumer = new EVCharger();
                EVCharger evCharger = (EVCharger) consumer;
                evCharger.setVoltage(body.getDouble("voltage", 0.0).intValue());
                evCharger.setMaxAmp(body.getDouble("maxAmp", 0.0).intValue());
                evCharger.setType(body.getString("type", ""));
            } else {
                // Cannot instantiate abstract Consumer class directly
                // Create a concrete implementation instead
                consumer = new Consumer() {}; // Anonymous subclass of Consumer
            }
            
            // Set common fields
            consumer.setName(body.getString("name", ""));
            consumer.setDescription(body.getString("description", ""));
            consumer.setMaxPower(body.getDouble("max_power", 0.0));
            
            // Set grid if provided
            if (body.containsKey("grid")) {
                Long gridId = body.getLong("grid");
                Grid grid = db.find(Grid.class, gridId);
                if (grid != null) {
                    consumer.setGrid(grid);
                }
            }
            
            // Persist to database
            EntityTransaction transaction = db.getTransaction();
            transaction.begin();
            db.persist(consumer);
            transaction.commit();
            
            // Return success response with the created ID
            context.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("id", consumer.getId()).encode());
        } catch (Exception e) {
            context.response()
                    .setStatusCode(400)
                    .end("Invalid consumer data: " + e.getMessage());
        }
    }
    
    private void updateConsumer(RoutingContext context, long id) {
        try {
            Consumer consumer = db.find(Consumer.class, id);
            
            if (consumer == null) {
                context.response()
                        .setStatusCode(404)
                        .end("Consumer not found");
                return;
            }
            
            JsonObject body = context.getBodyAsJson();
            
            // Start transaction
            EntityTransaction transaction = db.getTransaction();
            transaction.begin();
            
            // Update common fields if provided
            if (body.containsKey("name")) {
                consumer.setName(body.getString("name"));
            }
            
            if (body.containsKey("description")) {
                consumer.setDescription(body.getString("description"));
            }
            
            if (body.containsKey("max_power")) {
                consumer.setMaxPower(body.getDouble("max_power"));
            }
            
            // Update grid if provided
            if (body.containsKey("grid")) {
                Long gridId = body.getLong("grid");
                Grid grid = db.find(Grid.class, gridId);
                if (grid != null) {
                    consumer.setGrid(grid);
                }
            }
            
            // Update EVCharger specific fields
            if (consumer instanceof EVCharger && body.containsKey("voltage")) {
                ((EVCharger) consumer).setVoltage(body.getDouble("voltage").intValue());
            }
            
            if (consumer instanceof EVCharger && body.containsKey("maxAmp")) {
                ((EVCharger) consumer).setMaxAmp(body.getDouble("maxAmp").intValue());
            }
            
            if (consumer instanceof EVCharger && body.containsKey("type")) {
                ((EVCharger) consumer).setType(body.getString("type"));
            }
            
            // Commit changes
            transaction.commit();
            
            // Return success response
            context.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("id", consumer.getId()).encode());
        } catch (Exception e) {
            context.response()
                    .setStatusCode(400)
                    .end("Error updating consumer: " + e.getMessage());
        }
    }
    
    private void deleteConsumer(RoutingContext context, long id) {
        try {
            Consumer consumer = db.find(Consumer.class, id);
            
            if (consumer == null) {
                context.response()
                        .setStatusCode(404)
                        .end("Consumer not found");
                return;
            }
            
            // Start transaction
            EntityTransaction transaction = db.getTransaction();
            transaction.begin();
            
            // Remove consumer
            db.remove(consumer);
            
            // Commit changes
            transaction.commit();
            
            // Return success response
            context.response()
                    .setStatusCode(204)
                    .end();
        } catch (Exception e) {
            context.response()
                    .setStatusCode(500)
                    .end("Error deleting consumer: " + e.getMessage());
        }
    }
}
