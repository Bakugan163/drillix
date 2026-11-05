package com.example.drillix.bha;


public enum RecommendationStatus {
    DRAFT("Черновик — требует проверки технологом"),
    APPROVED("Согласована"),
    REJECTED("Отклонена");

    private final String label;

    RecommendationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}