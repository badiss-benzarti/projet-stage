package com.gestionstages.evaluation.export;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Map;

/**
 * Rendu PDF a partir d'un template Thymeleaf.
 *
 * Thymeleaf produit du XHTML, openhtmltopdf le transforme en PDF. On
 * ecrit donc du HTML et du CSS plutot que de positionner des elements a
 * la main comme avec iText : la mise en page se relit et se modifie.
 *
 * Contrainte : le template doit etre du XHTML STRICT (balises fermees,
 * entites echappees), sinon le parseur XML echoue.
 */
@Component
@RequiredArgsConstructor
public class PdfExporter {

    private final TemplateEngine templateEngine;

    public byte[] render(String template, Map<String, Object> variables) {
        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariables(variables);

        String html = templateEngine.process(template, ctx);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Generation du PDF impossible : " + e.getMessage(), e);
        }
    }
}
