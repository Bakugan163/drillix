package com.example.drillix.Well;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name="wells")
public class Well {

    @Id 
    @GeneratedValue (strategy=GenerationType.IDENTITY)
    private Long id;

    @Column (nullable=false, length=255)
    private String field; //месторождение
    
    @Column (nullable=false, length=20)
    private String wellNumber; //номер скважины

    @Enumerated (EnumType.STRING) //преобразование енамов в строки
    @Column (nullable=false)
    private WellType wellType; //тип скважины

    @Column (nullable=false)
    private Integer projectDepth; //проектная глубина

    @Column (nullable=false)
    private LocalDate drillingStartDate; //дата начала работ

    @Column (nullable=false)
    private LocalDate completionDate; //дата окончания работ

    @CreationTimestamp //Hibernate сам проставит момент создания записи
    @Column (name="created_at", nullable=false, updatable=false) //момент создания записи в системе
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getWellNumber() {
        return wellNumber;
    }

    public void setWellNumber(String wellNumber) {
        this.wellNumber = wellNumber;
    }

    public WellType getWellType() {
        return wellType;
    }

    public void setWellType(WellType wellType) {
        this.wellType = wellType;
    }

    public Integer getProjectDepth() {
        return projectDepth;
    }

    public void setProjectDepth(Integer projectDepth) {
        this.projectDepth = projectDepth;
    }

    public LocalDate getDrillingStartDate() {
        return drillingStartDate;
    }

    public void setDrillingStartDate(LocalDate drillingStartDate) {
        this.drillingStartDate = drillingStartDate;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDate completionDate) {
        this.completionDate = completionDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
}
