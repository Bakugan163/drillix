package com.example.drillix.Section;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.example.drillix.Well.Well;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name="sections")
@NoArgsConstructor
public class Section {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) 
    @JoinColumn ( nullable=false)
    private Well well;

    @Enumerated (EnumType.STRING)
    @Column (nullable=false)
    private SectionType type;

    @Column(nullable=false)
    private Integer depthFrom; //устье или кровля

    @Column(nullable=false)
    private Integer depthTo; //забой

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal bitDiametr;

    @Column(nullable=false)
    private TrajectoryTask trajectoryTask;

    @Column(name = "max_zenith_angle", precision = 5, scale = 1)
    private BigDecimal maxZenithAngle;

    @Column(columnDefinition = "text")
    private String drillingCondition;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Well getWell() {
        return well;
    }

    public void setWell(Well well) {
        this.well = well;
    }

    public SectionType getType() {
        return type;
    }

    public void setType(SectionType type) {
        this.type = type;
    }

    public Integer getDepthFrom() {
        return depthFrom;
    }

    public void setDepthFrom(Integer depthFrom) {
        this.depthFrom = depthFrom;
    }

    public Integer getDepthTo() {
        return depthTo;
    }

    public void setDepthTo(Integer depthTo) {
        this.depthTo = depthTo;
    }

    public BigDecimal getBitDiametr() {
        return bitDiametr;
    }

    public void setBitDiametr(BigDecimal bitDiametr) {
        this.bitDiametr = bitDiametr;
    }

    public TrajectoryTask getTrajectoryTask() {
        return trajectoryTask;
    }

    public void setTrajectoryTask(TrajectoryTask trajectoryTask) {
        this.trajectoryTask = trajectoryTask;
    }

    public BigDecimal getMaxZenithAngle() {
        return maxZenithAngle;
    }

    public void setMaxZenithAngle(BigDecimal maxZenithAngle) {
        this.maxZenithAngle = maxZenithAngle;
    }

    public String getDrillingCondition() {
        return drillingCondition;
    }

    public void setDrillingCondition(String drillingCondition) {
        this.drillingCondition = drillingCondition;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    
}
