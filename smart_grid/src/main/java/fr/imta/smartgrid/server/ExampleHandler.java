package fr.imta.smartgrid.server;

import fr.imta.smartgrid.model.EVCharger;
import fr.imta.smartgrid.model.Sensor;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

public class ExampleHandler implements Handler<RoutingContext> {
    EntityManager db;

    public ExampleHandler(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext event) {
        Long nbSensors = (Long)db.createNativeQuery("SELECT count(*) FROM sensor").getSingleResult();
        
        event.end("There are " + nbSensors + " sensors in database");

        // get a single object from DB, notice the second parameter to the createNativeQuery function
        // we also pass a parameter to the query with the ?, we use setParameter to set the value safely (otherwise SQL injection risk), first argument is the position of the ?, second is the value. Position start at 1 !
        Sensor s = (Sensor) db
            .createNativeQuery("SELECT * FROM sensor WHERE id = ?", Sensor.class)
            .setParameter(1, 4)
            .getSingleResult();

        System.out.println("id: " + s.getId());
        System.out.println("name: " + s.getName());
        System.out.println("description: " + s.getDescription());

        s.setDescription("you can change attributes");


        EVCharger charger = new EVCharger();
        charger.setName("my wonderful EVCharger");

        // when you want to make change to the DB you need to start a transaction
        db.getTransaction().begin();

        // then you can register your new or modified objects to be saved
        db.persist(charger);
        db.persist(s);

        // finally you can commit the change
        db.getTransaction().commit();
        // or rollback is something wrong happened
        // db.getTransaction().rollback();
    }
    
}
