package com.example.drillix.bha;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;

import java.util.List;

// Экспорт рекомендации КНБК в Excel (FR-4, Apache POI).
// Возвращает массив байт — готовый .xlsx, контроллер отдаст его файлом
@Service
public class BhaExportService {

    private final BhaRecommendationRepository recommendationRepository;
    private final BhaElementRepository bhaElementRepository;


    public BhaExportService(BhaRecommendationRepository recommendationRepository, BhaElementRepository bhaElementRepository) {
        this.recommendationRepository=recommendationRepository;
        this.bhaElementRepository=bhaElementRepository;
       
    }

    public byte[] exportToExcel(Long recommendationId) {
        // 1. Достаём рекомендацию с компоновкой и элементами
        BhaRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Рекомендация с id=" + recommendationId + " не найдена"));

        List<BhaElement> elements = bhaElementRepository
                .findAllByBhaOrderByOrderNoAsc(recommendation.getBha());

        // 2. Собираем книгу
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("КНБК");

            // Стиль заголовка: жирный
            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            int rowIdx = 0;

            // --- Шапка ---
            rowIdx = writeRow(sheet, boldStyle, rowIdx,
                    "РЕКОМЕНДАЦИЯ КНБК №" + recommendation.getId());
            rowIdx = writeRow(sheet, null, rowIdx,
                    "Статус: " + recommendation.getStatus().getLabel());
            rowIdx = writeRow(sheet, null, rowIdx,
                    "Скважина: " + recommendation.getSection().getWell().getWellNumber()
                            + " (" + recommendation.getSection().getWell().getField() + ")");
            rowIdx = writeRow(sheet, null, rowIdx,
                    "Секция: " + recommendation.getSection().getType()
                            + ", интервал " + recommendation.getSection().getDepthFrom()
                            + "–" + recommendation.getSection().getDepthTo() + " м"
                            + ", долото " + recommendation.getSection().getBitDiametr() + " мм");
            rowIdx = writeRow(sheet, null, rowIdx,
                    "Модель: " + recommendation.getModel()
                            + "    Дата: " + recommendation.getCreatedAt().toLocalDate());
            rowIdx++;   // пустая строка

            // --- Обоснование ---
            rowIdx = writeRow(sheet, boldStyle, rowIdx, "ОБОСНОВАНИЕ:");
            rowIdx = writeRow(sheet, null, rowIdx, recommendation.getJusttification());
            rowIdx++;

            // --- Таблица состава ---
            rowIdx = writeRow(sheet, boldStyle, rowIdx,
                    "№", "Тип", "Оборудование", "Параметры", "Комментарий");

            for (BhaElement el : elements) {
                String type = el.getEquipment() != null
                        ? el.getEquipment().getType().name()
                        : el.getFreeType().name();
                String name = el.getEquipment() != null
                        ? el.getEquipment().getName() + " (#" + el.getEquipment().getId() + ")"
                        : "нет в справочнике";
                String params = el.getFreeParams() != null ? el.getFreeParams() : "—";

                rowIdx = writeRow(sheet, null, rowIdx,
                        String.valueOf(el.getOrderNo()), type, name, params,
                        el.getComment() != null ? el.getComment() : "—");
            }

            rowIdx++;
            rowIdx = writeRow(sheet, null, rowIdx,
                    "Рекомендация предварительная, требует проверки технологом.");

            // 3. Книга → байты
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException("Ошибка формирования Excel: " + e.getMessage(), e);
        }
    }

    // Хелпер: строка из N ячеек. Возвращает следующий номер строки
    private int writeRow(Sheet sheet, CellStyle style, int rowIdx, String... values) {
        Row row = sheet.createRow(rowIdx);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i]);
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
        return rowIdx + 1;
    }

       
}