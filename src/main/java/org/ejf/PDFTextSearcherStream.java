package org.ejf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDFTextSearcherStream {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Использование: java PDFTextSearcherCyrillicV3 <путь_к_папке> <строка_для_поиска>");
            System.out.println("Пример: java PDFTextSearcherCyrillicV3 \"./documents\" \"договор\"");
            return;
        }

        String folderPath = args[0];
        String searchString = args[1];

        try {
            List<String> foundFiles = new ArrayList<>();
            File folder = new File(folderPath);

            if (!folder.exists() || !folder.isDirectory()) {
                throw new IllegalArgumentException("Указанный путь не является папкой или не существует: " + folderPath);
            }

            // Обработка файлов
            processFolder(folder, searchString, foundFiles);

            // Вывод результатов
            if (foundFiles.isEmpty()) {
                System.out.println("Строка \"" + searchString + "\" не найдена ни в одном PDF-файле.");
            } else {
                System.out.println("Строка \"" + searchString + "\" найдена в следующих файлах:");
                for (String filePath : foundFiles) {
                    System.out.println("- " + filePath);
                }
                System.out.println("Всего найдено: " + foundFiles.size() + " файлов");
            }
        } catch (Exception e) {
            System.err.println("Ошибка при поиске: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processFolder(File folder, String searchString, List<String> foundFiles) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                processFolder(file, searchString, foundFiles);
            } else if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
                try {
                    if (searchInPDF(file, searchString)) {
                        foundFiles.add(file.getAbsolutePath());
                    }
                } catch (IOException e) {
                    System.err.println("Ошибка при обработке файла " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private static boolean searchInPDF(File pdfFile, String searchString) throws IOException {
        System.out.println("Обработка файла: " + pdfFile.getName());

        // Используем Loader.loadPDF() для PDFBox 3.x
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            if (document.isEncrypted()) {
                System.out.println("  Предупреждение: Файл " + pdfFile.getName() + " зашифрован, пропускаем...");
                return false;
            }

            PDFTextStripper stripper = new PDFTextStripper();

            // Настройки для лучшего извлечения текста
            stripper.setSortByPosition(true);
            stripper.setAddMoreFormatting(false);

            String text = stripper.getText(document);

            // Поиск без учета регистра
            if (text != null) {
                return text.toLowerCase().contains(searchString.toLowerCase());
            }

            return false;
        }
    }
}