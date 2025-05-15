package fr.imta.smartgrid.model;

import io.vertx.core.json.JsonObject;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "solar_panel")
@PrimaryKeyJoinColumn(name = "id")
public class SolarPanel extends Producer {
    private float efficiency;  // Changé de Double à float

    public float getEfficiency() {  // Changé de Double à float
        return efficiency;
    }

    public void setEfficiency(float efficiency) {  // Changé de Double à float
        this.efficiency = efficiency;
    }
}
