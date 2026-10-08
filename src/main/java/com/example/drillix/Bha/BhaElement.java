package com.example.drillix.Bha;

import com.example.drillix.Equipment.Equipment;
import com.example.drillix.Equipment.EquipmentType;

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

@Entity 
@Table (name="bha_elements")
public class BhaElement {

    @Id 
    @GeneratedValue (strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne (fetch=FetchType.LAZY)
    @JoinColumn (name="bha_id", nullable=false)
    private Bha bha;

    @Column (nullable=false)
    private Integer orderNo;


    // === РЕЖИМ 1: основной — ссылка на справочник ===

    @ManyToOne (fetch=FetchType.LAZY)
    @JoinColumn (name="equipment_id")
    private Equipment equipment;

     // === РЕЖИМ 2: свободный — когда в справочнике нет позиции ===

     @Enumerated (EnumType.STRING)
     @Column (name="free_type")
     private EquipmentType freeType;

     @Column(columnDefinition = "text")
     private String freeParams;

     @Column(length = 500)
     private String comment;

     public boolean isFromCatalog() {
        return equipment != null;
     }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bha getBha() {
        return bha;
    }

    public void setBha(Bha bha) {
        this.bha = bha;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public EquipmentType getFreeType() {
        return freeType;
    }

    public void setFreeType(EquipmentType freeType) {
        this.freeType = freeType;
    }

    public String getFreeParams() {
        return freeParams;
    }

    public void setFreeParams(String freeParams) {
        this.freeParams = freeParams;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
