package com.example.drillix.bha;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity 
@Table(name="bha")
public class Bha {
    
    @Id 
    @GeneratedValue (strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(length=255)
    private String name;

    @CreationTimestamp 
    @Column (nullable=false, updatable=false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "bha", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BhaElement> elements = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<BhaElement> getElements() {
        return elements;
    }

    public void setElements(List<BhaElement> elements) {
        this.elements = elements;
    }

}
