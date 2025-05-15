package fr.imta.smartgrid.server.udp;

import fr.imta.smartgrid.model.DataPoint;
import fr.imta.smartgrid.model.Measurement;
import fr.imta.smartgrid.model.SolarPanel;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import jakarta.persistence.EntityManager;

public class UDPServer {
    private final Vertx vertx;
    private final EntityManager db;
    private DatagramSocket socket;

    public UDPServer(Vertx vertx, EntityManager db) {
        this.vertx = vertx;
        this.db = db;
    }

    public void start(int port) {
        // Create UDP socket with options
        DatagramSocketOptions options = new DatagramSocketOptions();
        socket = vertx.createDatagramSocket(options);

        // Listen for incoming packets
        socket.listen(port, "0.0.0.0", res -> {
            if (res.succeeded()) {
                System.out.println("UDP Server listening on port " + port);
                
                // Set packet handler
                socket.handler(packet -> {
                    String message = packet.data().toString();
                    processUDPMessage(message);
                });
            } else {
                System.err.println("Failed to start UDP server: " + res.cause().getMessage());
            }
        });
    }

    // Process incoming UDP messages from solar panels
    private void processUDPMessage(String message) {
        try {
            // Parse the message with format: id:temperature:power:timestamp
            String[] parts = message.split(":");
            if (parts.length != 4) {
                System.err.println("Invalid UDP message format: " + message);
                return;
            }

            int solarPanelId = Integer.parseInt(parts[0]);
            double temperature = Double.parseDouble(parts[1]);
            double power = Double.parseDouble(parts[2]);
            long timestamp = Long.parseLong(parts[3]);

            // Look up the solar panel
            SolarPanel solarPanel = db.find(SolarPanel.class, solarPanelId);
            if (solarPanel == null) {
                System.err.println("Solar panel not found with ID: " + solarPanelId);
                return;
            }

            // Get measurements for temperature, power, and total energy
            Measurement tempMeasurement = null;
            Measurement powerMeasurement = null;
            Measurement energyMeasurement = null;

            for (Measurement m : solarPanel.getMeasurements()) {
                if ("temperature".equals(m.getName())) {
                    tempMeasurement = m;
                } else if ("power".equals(m.getName())) {
                    powerMeasurement = m;
                } else if ("total_energy_produced".equals(m.getName())) {
                    energyMeasurement = m;
                }
            }

            // Get latest energy datapoint to calculate the new total
            double prevEnergy = 0;
            if (energyMeasurement != null) {
                // Find the latest energy datapoint
                DataPoint latestEnergy = db.createQuery(
                    "SELECT dp FROM DataPoint dp WHERE dp.measurement = :measurement " +
                    "ORDER BY dp.timestamp DESC", DataPoint.class)
                    .setParameter("measurement", energyMeasurement)
                    .setMaxResults(1)
                    .getResultList()
                    .stream()
                    .findFirst()
                    .orElse(null);

                if (latestEnergy != null) {
                    prevEnergy = latestEnergy.getValue();
                }
            }

            // Calculate new energy (assuming 60 seconds between measurements)
            // Energy (Wh) = Power (W) * Time (h), where time is 60 seconds = 1/60 hour
            double newEnergy = prevEnergy + (power * (1.0 / 60.0));

            // Create temperature datapoint
            if (tempMeasurement != null) {
                saveDataPoint(tempMeasurement, timestamp, temperature);
            }

            // Create power datapoint
            if (powerMeasurement != null) {
                saveDataPoint(powerMeasurement, timestamp, power);
            }

            // Create energy datapoint
            if (energyMeasurement != null) {
                saveDataPoint(energyMeasurement, timestamp, newEnergy);
            }

            System.out.println("Processed solar panel data - ID: " + solarPanelId + 
                    ", Temperature: " + temperature + 
                    ", Power: " + power + 
                    ", Timestamp: " + timestamp);

        } catch (Exception e) {
            System.err.println("Error processing UDP message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveDataPoint(Measurement measurement, long timestamp, double value) {
        try {
            DataPoint datapoint = new DataPoint();
            datapoint.setMeasurement(measurement);
            datapoint.setTimestamp(timestamp);
            datapoint.setValue(value);

            db.getTransaction().begin();
            db.persist(datapoint);
            db.getTransaction().commit();
        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            System.err.println("Error saving datapoint: " + e.getMessage());
        }
    }
}
