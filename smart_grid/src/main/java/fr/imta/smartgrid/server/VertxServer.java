package fr.imta.smartgrid.server;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.persistence.config.TargetServer;

import fr.imta.smartgrid.server.handlers.*;
import fr.imta.smartgrid.server.udp.UDPServer;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;

import static org.eclipse.persistence.config.PersistenceUnitProperties.*;

public class VertxServer {
    private Vertx vertx;
    private EntityManager db; // database object
    private UDPServer udpServer;

    public VertxServer() {
        this.vertx = Vertx.vertx();

        // setup database connexion
        Map<String, String> properties = new HashMap<>();

        properties.put(LOGGING_LEVEL, "FINE");
        properties.put(CONNECTION_POOL_MIN, "1");

        properties.put(TARGET_SERVER, TargetServer.None);

        var emf = Persistence.createEntityManagerFactory("smart-grid", properties);
        db = emf.createEntityManager();
        
        // Create UDP server for solar panel data
        this.udpServer = new UDPServer(vertx, db);
    }

    public void start() {
        Router router = Router.router(vertx);
        
        // Add body handler to parse request bodies
        router.route().handler(BodyHandler.create());

        // Example route
        router.get("/hello").handler(new ExampleHandler(this.db));
        
        // Grid routes
        router.get("/grids").handler(new GridsHandler(this.db));
        router.get("/grid/:id").handler(new GridHandler(this.db));
        router.get("/grid/:id/production").handler(new GridProductionHandler(this.db));
        router.get("/grid/:id/consumption").handler(new GridConsumptionHandler(this.db));
        
        // Person routes
        router.get("/persons").handler(new PersonsHandler(this.db));
        router.get("/person/:id").handler(new PersonHandler(this.db));
        router.post("/person/:id").handler(new PersonUpdateHandler(this.db));
        router.delete("/person/:id").handler(new PersonDeleteHandler(this.db));
        router.put("/person").handler(new PersonCreateHandler(this.db));
        
        // Sensor routes
        router.get("/sensor/:id").handler(new SensorHandler(this.db));
        router.get("/sensors/:kind").handler(new SensorsKindHandler(this.db));
        router.get("/consumers").handler(new ConsumersHandler(this.db));
        router.get("/producers").handler(new ProducersHandler(this.db));
        // Uncomment the sensor update route
        router.post("/sensor/:id").handler(new SensorUpdateHandler(this.db));
        
        // Measurement routes - uncomment these routes
        router.get("/measurement/:id").handler(new MeasurementHandler(this.db));
        router.get("/measurement/:id/values").handler(new MeasurementValuesHandler(this.db));
        
        // Ingress routes for sensor data
        router.post("/ingress/windturbine").handler(new WindTurbineIngressHandler(this.db));
        
        // Start the UDP server
        udpServer.start(12345);
        
        // start the HTTP server
        vertx.createHttpServer().requestHandler(router).listen(8080);
        System.out.println("Server started on port 8080");
    }

    public static void main(String[] args) {
        new VertxServer().start();
    }
}
