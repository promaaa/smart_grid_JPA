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

public class HandlerConsumers implements Handler<RoutingContext> {
    private final EntityManager db;

    public HandlerConsumers(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        String method = context.request().method().name();
        String path = context.request().path();
        String id = context.pathParam("id");
        
        try {
            // Gestion des routes selon la méthode HTTP et le chemin
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
                        .end("Requête incorrecte");
            }
        } catch (NumberFormatException e) {
            context.response()
                    .setStatusCode(400)
                    .end("Format d'ID invalide");
        } catch (Exception e) {
            context.response()
                    .setStatusCode(500)
                    .end("Erreur interne du serveur : " + e.getMessage());
        }
    }
    
    private void getAllConsumers(RoutingContext context) {
        // Récupérer tous les consommateurs depuis la base de données
        List<Consumer> consumers = db.createQuery("SELECT c FROM Consumer c", Consumer.class).getResultList();
        
        JsonArray response = new JsonArray();
        
        for (Consumer consumer : consumers) {
            JsonObject consumerJson = new JsonObject()
                    .put("id", consumer.getId())
                    .put("name", consumer.getName())
                    .put("description", consumer.getDescription())
                    .put("max_power", consumer.getMaxPower());
            
            // Ajouter l'identifiant du réseau électrique s'il existe
            if (consumer.getGrid() != null) {
                consumerJson.put("grid", consumer.getGrid().getId());
            }
            
            // Déterminer le type de consommateur
            String kind = consumer instanceof EVCharger ? "EVCharger" : "Consumer";
            consumerJson.put("kind", kind);
            
            // Ajouter les mesures disponibles
            JsonArray measurements = new JsonArray();
            consumer.getMeasurements().forEach(measurement -> measurements.add(measurement.getId()));
            consumerJson.put("available_measurements", measurements);
            
            // Ajouter les propriétaires
            JsonArray owners = new JsonArray();
            consumer.getOwners().forEach(owner -> owners.add(owner.getId()));
            consumerJson.put("owners", owners);
            
            // Ajouter les champs spécifiques à EVCharger
            if (consumer instanceof EVCharger) {
                EVCharger evCharger = (EVCharger) consumer;
                consumerJson.put("voltage", evCharger.getVoltage());
                consumerJson.put("maxAmp", evCharger.getMaxAmp());
                consumerJson.put("type", evCharger.getType());
            }
            
            response.add(consumerJson);
        }
        
        // Envoyer la réponse JSON
        context.response()
                .putHeader("content-type", "application/json")
                .end(response.encode());
    }
    
    private void getConsumer(RoutingContext context, long id) {
        // Rechercher un consommateur par ID
        Consumer consumer = db.find(Consumer.class, id);
        
        if (consumer == null) {
            context.response()
                    .setStatusCode(404)
                    .end("Consommateur non trouvé");
            return;
        }
        
        JsonObject consumerJson = new JsonObject()
                .put("id", consumer.getId())
                .put("name", consumer.getName())
                .put("description", consumer.getDescription())
                .put("max_power", consumer.getMaxPower());
        
        if (consumer.getGrid() != null) {
            consumerJson.put("grid", consumer.getGrid().getId());
        }
        
        String kind = consumer instanceof EVCharger ? "EVCharger" : "Consumer";
        consumerJson.put("kind", kind);
        
        JsonArray measurements = new JsonArray();
        consumer.getMeasurements().forEach(measurement -> measurements.add(measurement.getId()));
        consumerJson.put("available_measurements", measurements);
        
        JsonArray owners = new JsonArray();
        consumer.getOwners().forEach(owner -> owners.add(owner.getId()));
        consumerJson.put("owners", owners);
        
        if (consumer instanceof EVCharger) {
            EVCharger evCharger = (EVCharger) consumer;
            consumerJson.put("voltage", evCharger.getVoltage());
            consumerJson.put("maxAmp", evCharger.getMaxAmp());
            consumerJson.put("type", evCharger.getType());
        }
        
        context.response()
                .putHeader("content-type", "application/json")
                .end(consumerJson.encode());
    }
    
    private void createConsumer(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            
            Consumer consumer;
            String kind = body.getString("kind", "Consumer");
            
            // Créer l'objet approprié selon le type
            if ("EVCharger".equals(kind)) {
                consumer = new EVCharger();
                EVCharger evCharger = (EVCharger) consumer;
                evCharger.setVoltage(body.getDouble("voltage", 0.0).intValue());
                evCharger.setMaxAmp(body.getDouble("maxAmp", 0.0).intValue());
                evCharger.setType(body.getString("type", ""));
            } else {
                // Création d'une sous-classe anonyme de Consumer (Consumer est abstraite)
                consumer = new Consumer() {};
            }
            
            // Remplir les champs communs
            consumer.setName(body.getString("name", ""));
            consumer.setDescription(body.getString("description", ""));
            consumer.setMaxPower(body.getDouble("max_power", 0.0));
            
            // Associer à un réseau électrique si précisé
            if (body.containsKey("grid")) {
                Long gridId = body.getLong("grid");
                Grid grid = db.find(Grid.class, gridId);
                if (grid != null) {
                    consumer.setGrid(grid);
                }
            }
            
            // Enregistrement dans la base de données
            EntityTransaction transaction = db.getTransaction();
            transaction.begin();
            db.persist(consumer);
            transaction.commit();
            
            // Répondre avec l'ID du consommateur créé
            context.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("id", consumer.getId()).encode());
        } catch (Exception e) {
            context.response()
                    .setStatusCode(400)
                    .end("Données invalides pour le consommateur : " + e.getMessage());
        }
    }
    
    private void updateConsumer(RoutingContext context, long id) {
        try {
            Consumer consumer = db.find(Consumer.class, id);
            
            if (consumer == null) {
                context.response()
                        .setStatusCode(404)
                        .end("Consommateur non trouvé");
                return;
            }
            
            JsonObject body = context.getBodyAsJson();
            
            EntityTransaction transaction = db.getTransaction();
            transaction.begin();
            
            // Mise à jour des champs communs
            if (body.containsKey("name")) {
                consumer.setName(body.getString("name"));
            }
            
            if (body.containsKey("description")) {
                consumer.setDescription(body.getString("description"));
            }
            
            if (body.containsKey("max_power")) {
                consumer.setMaxPower(body.getDouble("max_power"));
            }
            
            if (body.containsKey("grid")) {
                Long gridId = body.getLong("grid");
                Grid grid = db.find(Grid.class, gridId);
                if (grid != null) {
                    consumer.setGrid(grid);
                }
            }
            
            // Mise à jour spécifique pour EVCharger
            if (consumer instanceof EVCharger && body.containsKey("voltage")) {
                ((EVCharger) consumer).setVoltage(body.getDouble("voltage").intValue());
            }
            
            if (consumer instanceof EVCharger && body.containsKey("maxAmp")) {
                ((EVCharger) consumer).setMaxAmp(body.getDouble("maxAmp").intValue());
            }
            
            if (consumer instanceof EVCharger && body.containsKey("type")) {
                ((EVCharger) consumer).setType(body.getString("type"));
            }
            
            transaction.commit();
            
            context.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("id", consumer.getId()).encode());
        } catch (Exception e) {
            context.response()
                    .setStatusCode(400)
                    .end("Erreur lors de la mise à jour : " + e.getMessage());
        }
    }
    
    private void deleteConsumer(RoutingContext context, long id) {
        try {
            Consumer consumer = db.find(Consumer.class, id);
            
            if (consumer == null) {
                context.response()
                        .setStatusCode(404)
                        .end("Consommateur non trouvé");
                return;
            }
            
            EntityTransaction transaction = db.getTransaction();
            transaction.begin();
            db.remove(consumer);
            transaction.commit();
            
            context.response()
                    .setStatusCode(204)
                    .end();
        } catch (Exception e) {
            context.response()
                    .setStatusCode(500)
                    .end("Erreur lors de la suppression : " + e.getMessage());
        }
    }
}
