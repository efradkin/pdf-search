package org.ejf;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.*;
import com.itextpdf.kernel.pdf.canvas.parser.listener.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class AdvancedPDFSearcher {

    // Статистика для отладки
    private static int textPDFs = 0;
    private static int scannedPDFs = 0;
    private static int encryptedPDFs = 0;
    private static int errorPDFs = 0;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Использование: java AdvancedPDFSearcher <папка> <текст>");
            System.out.println("Пример: java AdvancedPDFSearcher ./documents \"договор\"");
            System.out.println("\nОсобенности:");
            System.out.println("- Использует iText 7 для текстовых PDF");
            System.out.println("- Использует Apache PDFBox как запасной вариант");
            System.out.println("- Поддерживает кириллицу");
            return;
        }

        String folderPath = args[0];
        String searchString = args[1].toLowerCase();

        try {
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("sun.jnu.encoding", "UTF-8");

            List<String> foundFiles = searchInPDFs(folderPath, searchString);

            // Вывод статистики
            System.out.println("\n=== СТАТИСТИКА ===");
            System.out.println("Текстовых PDF: " + textPDFs);
            System.out.println("Зашифрованных PDF: " + encryptedPDFs);
            System.out.println("Ошибок обработки: " + errorPDFs);
            System.out.println("Всего обработано: " + (textPDFs + encryptedPDFs + errorPDFs));

            // Вывод результатов
            System.out.println("\n=== РЕЗУЛЬТАТЫ ===");
            if (foundFiles.isEmpty()) {
                System.out.println("Текст \"" + searchString + "\" не найден.");
            } else {
                System.out.println("Найдено в " + foundFiles.size() + " файлах:");
                foundFiles.forEach(f -> System.out.println("  ✓ " + f));
            }

        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static List<String> searchInPDFs(String folderPath, String searchString) throws IOException {
        Path startPath = Paths.get(folderPath);

        if (!Files.exists(startPath) || !Files.isDirectory(startPath)) {
            throw new IllegalArgumentException("Папка не существует: " + folderPath);
        }

        // Находим все PDF
        List<Path> pdfFiles = Files.walk(startPath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                .collect(Collectors.toList());

        System.out.println("Найдено PDF файлов: " + pdfFiles.size());

        List<String> foundFiles = new ArrayList<>();

        for (Path pdfPath : pdfFiles) {
            try {
                boolean found = processPDF(pdfPath, searchString);
                if (found) {
                    foundFiles.add(pdfPath.toAbsolutePath().toString());
                }
            } catch (Exception e) {
                errorPDFs++;
                System.err.println("Ошибка обработки " + pdfPath.getFileName() + ": " + e.getMessage());
            }
        }

        return foundFiles;
    }

    private static boolean processPDF(Path pdfPath, String searchText) {
        System.out.print("Обработка: " + pdfPath.getFileName() + " → ");

        // Пробуем разные методы по порядку:

        // 1. Сначала iText (лучше для кириллицы)
        try {
            boolean found = searchWithItext(pdfPath, searchText);
            if (found) {
                textPDFs++;
                return true;
            }
        } catch (Exception e) {
            System.out.print("[iText ошибка] ");
        }

        // 2. Потом PDFBox (запасной вариант)
        try {
            boolean found = searchWithPDFBox(pdfPath, searchText);
            if (found) {
                textPDFs++;
                return true;
            }
        } catch (Exception e) {
            System.out.print("[PDFBox ошибка] ");
        }

        // 3. Если оба метода не нашли и PDF не зашифрован,
        // возможно, это сканированный PDF - нужен OCR
        try {
            if (!isPDFEncrypted(pdfPath)) {
                System.out.print("[возможно сканированный] ");
                // Здесь можно добавить вызов OCR
            }
        } catch (Exception e) {
            // Игнорируем
        }

        System.out.println("не найден");
        return false;
    }

    private static boolean searchWithItext(Path pdfPath, String searchText) throws IOException {
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfPath.toString()))) {
            if (pdfDoc.getReader().isEncrypted()) {
                encryptedPDFs++;
                throw new IOException("Файл зашифрован");
            }

            ITextExtractionStrategy strategy = new SimpleTextExtractionStrategy();
            StringBuilder textBuilder = new StringBuilder();

            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                PdfPage page = pdfDoc.getPage(i);
                String pageText = PdfTextExtractor.getTextFromPage(page, strategy);
                textBuilder.append(pageText).append(" ");

                // Проверяем по мере извлечения для оптимизации
                if (textBuilder.toString().toLowerCase().contains(searchText)) {
                    System.out.println("найден (iText)");
                    return true;
                }
            }

            return false;
        }
    }

    private static boolean searchWithPDFBox(Path pdfPath, String searchText) throws IOException {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            if (document.isEncrypted()) {
                throw new IOException("Файл зашифрован");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setShouldSeparateByBeads(false);

            String text = stripper.getText(document);
            text = fixCyrillicEncoding(text);

            if (text.toLowerCase().contains(searchText)) {
                System.out.println("найден (PDFBox)");
                return true;
            }

            return false;
        }
    }

    private static boolean isPDFEncrypted(Path pdfPath) {
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfPath.toString()))) {
            return pdfDoc.getReader().isEncrypted();
        } catch (Exception e) {
            return false;
        }
    }

    private static String fixCyrillicEncoding(String text) {
        if (text == null) return "";

        // Быстрый маппинг распространенных проблем
        String[][] replacements = {
                {"Ã", "А"}, {"Ð", "Д"}, {"Å", "Е"}, {"Õ", "Х"}, {"Ó", "С"},
                {"à", "а"}, {"ð", "д"}, {"å", "е"}, {"õ", "х"}, {"ó", "с"},
                {"Â", "В"}, {"Ï", "П"}, {"Ò", "Т"}, {"Ì", "М"}, {"Ê", "К"},
                {"á", "б"}, {"ã", "г"}, {"è", "з"}, {"ë", "л"}, {"í", "н"},
                {"î", "о"}, {"ï", "п"}, {"ñ", "с"}, {"ò", "т"}, {"ô", "ф"},
                {"ö", "ц"}, {"÷", "ч"}, {"ø", "ш"}, {"ù", "щ"}, {"ú", "ъ"},
                {"û", "ы"}, {"ü", "ь"}, {"ý", "э"}, {"þ", "ю"}, {"ÿ", "я"}
        };

        String result = text;
        for (String[] replacement : replacements) {
            result = result.replace(replacement[0], replacement[1]);
        }

        return result;
    }
}