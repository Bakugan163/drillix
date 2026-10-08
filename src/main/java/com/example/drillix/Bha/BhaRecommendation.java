package com.example.drillix.Bha;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.example.drillix.Section.Section;

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
@Table(name="bha_recommendations")
public class BhaRecommendation {

    @Id 
    @GeneratedValue (strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn (name="section_id", nullable=false)
    private Section section;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn (name="bha_id", nullable=false)
    private Bha bha;

    @Enumerated (EnumType.STRING)
    @Column(nullable=false, columnDefinition="text")
    private RecommendationStatus status;

    @Column(nullable = false, columnDefinition = "text")
    private String justtification;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false, columnDefinition = "text")
    private String rawResponse;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public Bha getBha() {
        return bha;
    }

    public void setBha(Bha bha) {
        this.bha = bha;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }

    public String getJusttification() {
        return justtification;
    }

    public void setJusttification(String justtification) {
        this.justtification = justtification;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    
}
