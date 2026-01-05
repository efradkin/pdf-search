package org.ejf;

import net.sourceforge.tess4j.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PDFSearcherWithOCR {

    private static final String TIME_PATTERN = "HH:mm:ss dd.MM.yyyy";
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern(TIME_PATTERN).withZone(ZoneId.systemDefault());

    private static final ITesseract tesseract = new Tesseract();

    static {
        // Настройка Tesseract для русского языка
        tesseract.setDatapath("tessdata"); // путь к tessdata
        tesseract.setLanguage("rus+eng");
        tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java PDFSearcherWithOCR <folder> <search_text>");
            return;
        }

        String folderPath = args[0];
        String searchText = args[1].toLowerCase();

        Instant start = Instant.now();

        List<Path> pdfFiles = Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                .collect(Collectors.toList());

        int kPDFs = pdfFiles.size();
        System.out.println("Found " + kPDFs + " PDF files in the directory.");
        System.out.println("Searching '" + searchText + "' is started at " + TIME_FORMATTER.format(start));

        List<String> results = new ArrayList<>();

        AtomicInteger counter = new AtomicInteger(1);
        for (Path pdfPath : pdfFiles) {
            Path fileName = pdfPath.getFileName();
            System.out.println("Processing " + fileName + " (" + counter.getAndIncrement() + " of " + kPDFs + ")");

            try {
                // Сначала пробуем обычное извлечение текста
                String text = extractText(pdfPath);

                if (text.toLowerCase().contains(searchText)) {
                    results.add(pdfPath.toString());
                    System.out.println("FOUND (text) in " + fileName);
                } else {
                    // Если обычный текст не нашел, пробуем OCR
                    boolean searched = searchTextWithOCR(pdfPath, searchText);
                    if (searched) {
                        results.add(pdfPath.toString());
                        System.out.println("FOUND (OCR) in " + fileName);
                    } else {
                        System.out.println("Not found in " + fileName);
                    }
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        };

        System.out.println("\n=== RESULTS ===");
        System.out.println("Found in " + results.size() + " files:");
        results.forEach(System.out::println);

        Instant finish = Instant.now();
        String elapsed = getFormattedCurrentProgressTime(start, finish);
        System.out.println("It's finished in " + elapsed);
    }

    private static String extractText(Path pdfPath) throws IOException {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            if (document.isEncrypted()) {
                return "";
            }

            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private static boolean searchTextWithOCR(Path pdfPath, String searchText) throws Exception {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            if (document.isEncrypted()) {
                System.out.println(pdfPath.getFileName() + " is encrypted, so it can't be OCRed");
                return false;
            }

            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder textBuilder = new StringBuilder();

            // Конвертируем каждую страницу в изображение и распознаем
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 100); // 300 DPI для качественного OCR

                // Можно сохранить изображение для отладки
                // ImageIO.write(image, "png", new File("page_" + i + ".png"));

                String pageText = tesseract.doOCR(image);
                if (pageText.toLowerCase().contains(searchText)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static String getFormattedCurrentProgressTime(Instant start, Instant finish) {
        Duration dur = Duration.between(start, finish);
        return dur.toHoursPart() + "h " + dur.toMinutesPart() + "m " + dur.toSecondsPart() + "s";
    }
}
