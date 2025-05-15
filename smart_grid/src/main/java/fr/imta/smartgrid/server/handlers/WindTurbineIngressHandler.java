package fr.imta.smartgrid.server.handlers;

import fr.imta.smartgrid.model.DataPoint;
import fr.imta.smartgrid.model.Measurement;
import fr.imta.smartgrid.model.WindTurbine;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

import java.util.List;

public class WindTurbineIngressHandler implements Handler<RoutingContext> {
    private final EntityManager db;

    public WindTurbineIngressHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            // Parse request body
            JsonObject body = context.getBodyAsJson();
            if (body == null) {
                context.response().setStatusCode(500).end(new JsonObject().put("error", "Invalid JSON payload").encode());
                return;
            }

            // Extract data
            int turbineId = body.getInteger("windturbine");
            long timestamp = body.getLong("timestamp");
            JsonObject data = body.getJsonObject("data");

            if (data == null) {
                context.response().setStatusCode(500).end(new JsonObject().put("error", "Missing data field").encode());
                return;
            }

            double speed = data.getDouble("speed");
            double power = data.getDouble("power");

            // Find the wind turbine
            WindTurbine windTurbine = db.find(WindTurbine.class, turbineId);
            if (windTurbine == null) {
                context.response().setStatusCode(404).end(new JsonObject().put("error", "Wind turbine not found").encode());
                return;
            }

            // Find measurements for the wind turbine
            Measurement speedMeasurement = null;
            Measurement powerMeasurement = null;
            Measurement energyMeasurement = null;

            for (Measurement m : windTurbine.getMeasurements()) {
                if ("speed".equals(m.getName())) {
                    speedMeasurement = m;
                } else if ("power".equals(m.getName())) {
                    powerMeasurement = m;
                } else if ("total_energy_produced".equals(m.getName())) {
                    energyMeasurement = m;
                }
            }

            // Get latest energy value
            double prevEnergy = 0;
            if (energyMeasurement != null) {
                // Get the latest energy datapoint
                List<DataPoint> latestPoints = db.createQuery(
                        "SELECT dp FROM DataPoint dp WHERE dp.measurement = :measurement " +
                                "ORDER BY dp.timestamp DESC", DataPoint.class)
                        .setParameter("measurement", energyMeasurement)
                        .setMaxResults(1)
                        .getResultList();

                if (!latestPoints.isEmpty()) {
                    prevEnergy = latestPoints.get(0).getValue();
                }
            }

            // Calculate new energy (assuming 60 seconds since last datapoint)
            // Energy (Wh) = Power (W) * Time (h), where time is 60 seconds = 1/60 hour
            double newEnergy = prevEnergy + (power * (1.0 / 60.0));

            // Start transaction
            db.getTransaction().begin();

            // Save the speed datapoint
            if (speedMeasurement != null) {
                DataPoint speedDataPoint = new DataPoint();
                speedDataPoint.setMeasurement(speedMeasurement);
                speedDataPoint.setTimestamp(timestamp);
                speedDataPoint.setValue(speed);
                db.persist(speedDataPoint);
            }

            // Save the power datapoint
            if (powerMeasurement != null) {
                DataPoint powerDataPoint = new DataPoint();
                powerDataPoint.setMeasurement(powerMeasurement);
                powerDataPoint.setTimestamp(timestamp);
                powerDataPoint.setValue(power);
                db.persist(powerDataPoint);
            }

            // Save the energy datapoint
            if (energyMeasurement != null) {
                DataPoint energyDataPoint = new DataPoint();
                energyDataPoint.setMeasurement(energyMeasurement);
                energyDataPoint.setTimestamp(timestamp);
                energyDataPoint.setValue(newEnergy);
                db.persist(energyDataPoint);
            }

            // Commit transaction
            db.getTransaction().commit();

            // Return success response
            context.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("status", "success").encode());

        } catch (Exception e) {
            // Rollback transaction if active
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            context.response().setStatusCode(500).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }
}
