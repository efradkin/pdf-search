package org.ejf;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.*;
import com.itextpdf.kernel.pdf.canvas.parser.listener.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class PDFTextSearcherItext {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Использование: java PDFTextSearcherItext <путь_к_папке> <строка_для_поиска>");
            System.out.println("Пример: java PDFTextSearcherItext \"./documents\" \"договор\"");
            System.out.println("\nСовет: Для максимальной точности используйте комбинацию библиотек");
            return;
        }

        String folderPath = args[0];
        String searchString = args[1];

        // Устанавливаем кодировку
        System.setProperty("file.encoding", "UTF-8");

        List<String> foundFiles = searchInPDFs(folderPath, searchString);

        if (foundFiles.isEmpty()) {
            System.out.println("Строка \"" + searchString + "\" не найдена ни в одном PDF-файле.");
        } else {
            System.out.println("\nСтрока \"" + searchString + "\" найдена в следующих файлах:");
            for (String filePath : foundFiles) {
                System.out.println("- " + filePath);
            }
            System.out.println("\nВсего найдено: " + foundFiles.size() + " файлов");
        }
    }

    public static List<String> searchInPDFs(String folderPath, String searchString) throws IOException {
        Path startPath = Paths.get(folderPath);

        if (!Files.exists(startPath) || !Files.isDirectory(startPath)) {
            throw new IllegalArgumentException("Указанный путь не является папкой или не существует: " + folderPath);
        }

        // Находим все PDF файлы
        List<Path> pdfFiles;
        try {
            pdfFiles = Files.walk(startPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IOException("Ошибка при поиске файлов в папке: " + e.getMessage(), e);
        }

        System.out.println("Найдено " + pdfFiles.size() + " PDF-файлов для обработки...");

        List<String> foundFiles = new ArrayList<>();

        for (Path pdfPath : pdfFiles) {
            try {
                if (searchInPDFWithItext(pdfPath, searchString)) {
                    foundFiles.add(pdfPath.toAbsolutePath().toString());
                }
            } catch (Exception e) {
                System.err.println("Ошибка при обработке файла " + pdfPath + ": " + e.getMessage());
                // Пробуем альтернативный метод для этого файла
                try {
                    if (searchInPDFWithApache(pdfPath, searchString)) {
                        foundFiles.add(pdfPath.toAbsolutePath().toString());
                    }
                } catch (Exception ex) {
                    System.err.println("Альтернативный метод также не сработал для " + pdfPath);
                }
            }
        }

        return foundFiles;
    }

    private static boolean searchInPDFWithItext(Path pdfPath, String searchString) throws IOException {
        System.out.print("Обработка (iText): " + pdfPath.getFileName() + "... ");

        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfPath.toString()))) {
            if (pdfDoc.getReader().isEncrypted()) {
                System.out.println("зашифрован, пропускаем");
                return false;
            }

            StringBuilder textBuilder = new StringBuilder();
            SimpleTextExtractionStrategy strategy = new SimpleTextExtractionStrategy();

            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                PdfPage page = pdfDoc.getPage(i);
                String pageText = PdfTextExtractor.getTextFromPage(page, strategy);
                textBuilder.append(pageText);

                // Оптимизация: проверяем по мере извлечения
                if (textBuilder.toString().toLowerCase().contains(searchString.toLowerCase())) {
                    System.out.println("найдено!");
                    return true;
                }
            }

            String fullText = textBuilder.toString();
            boolean found = fullText.toLowerCase().contains(searchString.toLowerCase());
            System.out.println(found ? "найдено!" : "не найдено");
            return found;
        } catch (Exception e) {
            System.out.println("ошибка: " + e.getMessage());
            throw e;
        }
    }

    // Запасной вариант с Apache PDFBox
    private static boolean searchInPDFWithApache(Path pdfPath, String searchString) throws IOException {
        System.out.print("Обработка (Apache): " + pdfPath.getFileName() + "... ");

        try {
            org.apache.pdfbox.pdmodel.PDDocument document =
                    org.apache.pdfbox.pdmodel.PDDocument.load(pdfPath.toFile());

            if (document.isEncrypted()) {
                System.out.println("зашифрован");
                document.close();
                return false;
            }

            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            stripper.setSortByPosition(true);

            String text = stripper.getText(document);
            document.close();

            boolean found = text.toLowerCase().contains(searchString.toLowerCase());
            System.out.println(found ? "найдено!" : "не найдено");
            return found;

        } catch (Exception e) {
            System.out.println("ошибка: " + e.getMessage());
            throw e;
        }
    }
}