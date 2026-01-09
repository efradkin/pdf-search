package org.ejf;

import net.sourceforge.tess4j.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PDFSearcherWithOCR {

    public static final String O_MAPS_DOCS_TEXTS_FILE = "o-maps-docs-texts.json";

    private static final String TIME_PATTERN = "HH:mm:ss dd.MM.yyyy";
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern(TIME_PATTERN).withZone(ZoneId.systemDefault());

    private static final ITesseract tesseract = new Tesseract();
    public static final String TEXT_KEY = "text";
    public static final String OCR_KEY = "ocr";

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
        boolean needRewriteTextsFile = false;
        Map<String, Map<String, String>> storedTexts = Util.loadNestedMapFromFile(O_MAPS_DOCS_TEXTS_FILE);

        int counter = 1;
        for (Path pdfPath : pdfFiles) {
            Path fileName = pdfPath.getFileName();
            String pdfPathString = pdfPath.toString();
            String fileNameString = fileName.toString();
            System.out.println("Processing " + fileName + " (" + counter++ + " of " + kPDFs + ")");

            Map<String, String> storedDocTexts = storedTexts.get(fileNameString);
            if (storedDocTexts != null) {
                if (storedDocTexts.get(TEXT_KEY).contains(searchText) || storedDocTexts.get(OCR_KEY).contains(searchText)) {
                    results.add(pdfPathString);
                }
            } else {
                needRewriteTextsFile = true;
                storedDocTexts = new HashMap<>();
                try {
                    // Сначала пробуем обычное извлечение текста
                    String text = extractText(pdfPath);
                    String ocrText = searchTextWithOCR(pdfPath);
                    storedDocTexts.put(TEXT_KEY, text);
                    storedDocTexts.put(OCR_KEY, ocrText);
                    storedTexts.put(fileNameString, storedDocTexts);

                    if (text.toLowerCase().contains(searchText)) {
                        results.add(pdfPathString);
                        System.out.println("Found (text) in " + fileName);
                    } else {
                        // Если обычный текст не нашел, пробуем OCR
                        if (ocrText.toLowerCase().contains(searchText)) {
                            results.add(pdfPathString);
                            System.out.println("Found (OCR) in " + fileName);
                        } else {
                            System.out.println("Not found in " + fileName);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            }

        };

        if (needRewriteTextsFile) {
            Util.saveNestedMapToFile(storedTexts, O_MAPS_DOCS_TEXTS_FILE);
        }

        System.out.println("\n=== RESULTS ===");
        System.out.println("Found in " + results.size() + " files:");
        results.forEach(System.out::println);

        Instant finish = Instant.now();
        String elapsed = Util.getFormattedCurrentProgressTime(start, finish);
        System.out.println("It's finished in " + elapsed);
    }

    private static String extractText(Path pdfPath) throws IOException {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            if (document.isEncrypted()) {
                return "";
            }

            PDFTextStripper stripper = new PDFTextStripper();
            return Util.cleanText(stripper.getText(document));
        }
    }

    private static String searchTextWithOCR(Path pdfPath) throws Exception {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            if (document.isEncrypted()) {
                System.out.println(pdfPath.getFileName() + " is encrypted, so it can't be OCRed");
                return "";
            }

            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder textBuilder = new StringBuilder();

            // Конвертируем каждую страницу в изображение и распознаем
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 150);
                String pageText = tesseract.doOCR(image);
                textBuilder.append(pageText);
            }

            // На всякий случай, и в более высоком разрешении
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 300); // 300 DPI для качественного OCR
                String pageText = tesseract.doOCR(image);
                textBuilder.append(pageText);
            }

            return Util.cleanText(textBuilder.toString());
        }
    }

}
