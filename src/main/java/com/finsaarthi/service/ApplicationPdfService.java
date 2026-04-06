package com.finsaarthi.service;

import com.finsaarthi.entity.Application;
import com.finsaarthi.entity.ApplicationDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ApplicationPdfService {

    private static final PDFont TITLE_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont SECTION_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final float PAGE_MARGIN = 50f;
    private static final float PAGE_BOTTOM_MARGIN = 55f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - (PAGE_MARGIN * 2);
    private static final float TITLE_SIZE = 18f;
    private static final float SECTION_SIZE = 13f;
    private static final float BODY_SIZE = 10.5f;
    private static final float LINE_SPACING = 15f;

    public byte[] generateApplicationPdf(Application application) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PdfCanvas canvas = new PdfCanvas(document);

            canvas.writeTitle("FinSaarthi Application Summary");
            canvas.writeMuted("Application PDF");
            canvas.writeSpacer(10f);

            canvas.writeKeyValue("Application ID", String.valueOf(application.getId()));
            canvas.writeKeyValue("Scholarship", safe(application.getScholarship().getTitle()));
            canvas.writeKeyValue("Provider", safe(application.getScholarship().getProvider()));
            canvas.writeKeyValue("Status", application.getStatus().name());
            canvas.writeKeyValue("Submitted At", safe(String.valueOf(application.getSubmittedAt())));
            canvas.writeSpacer(8f);

            canvas.writeSection("Personal Details");
            canvas.writeKeyValue("Name", safe(application.getApplicantName()));
            canvas.writeKeyValue("Email", safe(application.getApplicantEmail()));
            canvas.writeKeyValue("Phone", safe(application.getApplicantPhone()));
            canvas.writeKeyValue("Location", safe(application.getLocation()));
            canvas.writeParagraph("Address: " + safe(application.getApplicantAddress()));

            canvas.writeSection("Education Details");
            canvas.writeParagraph("Education: " + safe(application.getApplicantEducation()));
            canvas.writeKeyValue("Class / Course", safe(application.getStudentClass()));
            canvas.writeKeyValue("10th Marks", safeNumber(application.getMarks10th()));
            canvas.writeKeyValue("12th Marks", safeNumber(application.getMarks12th()));
            canvas.writeKeyValue("GPA / Percentage", safeNumber(application.getGpa()));

            canvas.writeSection("Parent Details");
            canvas.writeKeyValue("Parent Name", safe(application.getParentName()));
            canvas.writeKeyValue("Occupation", safe(application.getParentOccupation()));
            canvas.writeKeyValue("Mobile", safe(application.getParentMobile()));

            canvas.writeSection("Documents");
            if (application.getApplicationDocuments() == null || application.getApplicationDocuments().isEmpty()) {
                canvas.writeParagraph("No documents submitted.");
            } else {
                for (ApplicationDocument applicationDocument : application.getApplicationDocuments()) {
                    canvas.writeParagraph(applicationDocument.getDocumentName() + " -> " + safe(applicationDocument.getLink()));
                }
            }

            if (application.getAdminNotes() != null && !application.getAdminNotes().isBlank()) {
                canvas.writeSection("Admin Notes");
                canvas.writeParagraph(safe(application.getAdminNotes()));
            }

            canvas.close();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            log.error("Failed to generate application PDF for id {}: {}", application.getId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to generate application PDF.");
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : sanitize(value);
    }

    private String safeNumber(Number value) {
        return value == null ? "N/A" : value.toString();
    }

    private String sanitize(String value) {
        return value.replace("\r", " ").replace("\n", " ").replace("\t", " ");
    }

    private static final class PdfCanvas {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream stream;
        private float cursorY;

        private PdfCanvas(PDDocument document) throws IOException {
            this.document = document;
            addPage();
        }

        private void addPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            cursorY = page.getMediaBox().getHeight() - PAGE_MARGIN;
        }

        private void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }

        private void writeTitle(String text) throws IOException {
            writeWrappedText(text, TITLE_FONT, TITLE_SIZE, false);
        }

        private void writeMuted(String text) throws IOException {
            writeWrappedText(text, BODY_FONT, 9f, false);
        }

        private void writeSection(String text) throws IOException {
            writeSpacer(8f);
            writeWrappedText(text, SECTION_FONT, SECTION_SIZE, true);
            drawDivider();
            writeSpacer(6f);
        }

        private void writeKeyValue(String label, String value) throws IOException {
            writeWrappedText(label + ": " + value, BODY_FONT, BODY_SIZE, false);
        }

        private void writeParagraph(String text) throws IOException {
            writeWrappedText(text, BODY_FONT, BODY_SIZE, false);
        }

        private void writeSpacer(float height) {
            cursorY -= height;
        }

        private void writeWrappedText(String text, PDFont font, float fontSize, boolean keepWithNext) throws IOException {
            List<String> lines = wrapText(text, font, fontSize, CONTENT_WIDTH);
            float requiredHeight = Math.max(lines.size(), 1) * LINE_SPACING + (keepWithNext ? 10f : 0f);
            ensureSpace(requiredHeight);

            for (String line : lines) {
                stream.beginText();
                stream.setFont(font, fontSize);
                stream.newLineAtOffset(PAGE_MARGIN, cursorY);
                stream.showText(line);
                stream.endText();
                cursorY -= LINE_SPACING;
            }
        }

        private void drawDivider() throws IOException {
            ensureSpace(8f);
            stream.moveTo(PAGE_MARGIN, cursorY);
            stream.lineTo(PAGE_MARGIN + CONTENT_WIDTH, cursorY);
            stream.stroke();
            cursorY -= 4f;
        }

        private void ensureSpace(float height) throws IOException {
            if (cursorY - height < PAGE_BOTTOM_MARGIN) {
                addPage();
            }
        }

        private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            String normalized = text == null ? "" : text.trim();
            if (normalized.isEmpty()) {
                lines.add("");
                return lines;
            }

            StringBuilder currentLine = new StringBuilder();
            for (String word : normalized.split("\\s+")) {
                String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
                float candidateWidth = font.getStringWidth(candidate) / 1000 * fontSize;
                if (candidateWidth <= maxWidth) {
                    currentLine.setLength(0);
                    currentLine.append(candidate);
                } else {
                    if (!currentLine.isEmpty()) {
                        lines.add(currentLine.toString());
                    }
                    currentLine.setLength(0);
                    currentLine.append(word);
                }
            }

            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }

            return lines;
        }
    }
}
