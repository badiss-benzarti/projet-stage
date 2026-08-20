package com.gestionstages.evaluation.export;

import com.gestionstages.evaluation.config.EvaluationProperties;
import com.gestionstages.evaluation.entity.Evaluation;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Export XLSX des notes de stage, exige par le cahier des charges.
 *
 * Le fichier est genere en memoire : aucun fichier temporaire a nettoyer,
 * et le service reste sans etat.
 */
@Component
@RequiredArgsConstructor
public class XlsxExporter {

    private static final String[] ENTETES = {
            "Etudiant", "Type", "Entreprise", "Encadrant",
            "Technique", "Qualite", "Autonomie", "Communication", "Assiduite",
            "Note finale"
    };

    private final EvaluationProperties props;

    public byte[] export(List<Evaluation> evaluations) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Notes de stage");

            CellStyle titre    = styleTitre(wb);
            CellStyle entete   = styleEntete(wb);
            CellStyle note     = styleNote(wb, false);
            CellStyle noteFin  = styleNote(wb, true);

            // ---- Bandeau ----
            Row bandeau = sheet.createRow(0);
            Cell c0 = bandeau.createCell(0);
            c0.setCellValue("Notes de stage - genere le " + LocalDate.now());
            c0.setCellStyle(titre);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, ENTETES.length - 1));

            // ---- En-tetes ----
            Row header = sheet.createRow(2);
            for (int i = 0; i < ENTETES.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(ENTETES[i]);
                c.setCellStyle(entete);
            }

            // ---- Lignes ----
            int ligne = 3;
            for (Evaluation e : evaluations) {
                Row r = sheet.createRow(ligne++);
                r.createCell(0).setCellValue(nvl(e.getStudentName()));
                r.createCell(1).setCellValue(nvl(e.getInternshipType()));
                r.createCell(2).setCellValue(nvl(e.getCompanyName()));
                r.createCell(3).setCellValue(nvl(e.getSupervisorName()));

                cellNote(r, 4, e.getTechnicalScore(),     note);
                cellNote(r, 5, e.getQualityScore(),       note);
                cellNote(r, 6, e.getAutonomyScore(),      note);
                cellNote(r, 7, e.getCommunicationScore(), note);
                cellNote(r, 8, e.getPunctualityScore(),   note);
                cellNote(r, 9, e.getFinalScore(),         noteFin);
            }

            // ---- Pied : moyenne de la promotion ----
            if (!evaluations.isEmpty()) {
                Row pied = sheet.createRow(ligne + 1);
                Cell libelle = pied.createCell(8);
                libelle.setCellValue("Moyenne");
                libelle.setCellStyle(entete);

                Cell moyenne = pied.createCell(9);
                moyenne.setCellFormula("AVERAGE(J4:J" + (ligne) + ")");
                moyenne.setCellStyle(noteFin);
            }

            sheet.createFreezePane(0, 3);
            for (int i = 0; i < ENTETES.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private void cellNote(Row r, int col, Double valeur, CellStyle style) {
        Cell c = r.createCell(col);
        if (valeur != null) {
            c.setCellValue(valeur);
        }
        c.setCellStyle(style);
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private CellStyle styleTitre(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 13);
        s.setFont(f);
        return s;
    }

    private CellStyle styleEntete(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        border(s);
        return s;
    }

    private CellStyle styleNote(Workbook wb, boolean finale) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        if (finale) {
            Font f = wb.createFont();
            f.setBold(true);
            s.setFont(f);
        }
        border(s);
        return s;
    }

    private void border(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }
}
