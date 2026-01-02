package org.ejf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PDFTextSearcherStream {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Использование: java PDFTextSearcherStream <путь_к_папке> <строка_для_поиска>");
            return;
        }

        Path startPath = Paths.get(args[0]);
        String searchString = args[1].toLowerCase();

        try (Stream<Path> paths = Files.walk(startPath)) {
            List<String> foundFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                    .filter(p -> {
                        try {
                            return searchInPDF(p.toFile(), searchString);
                        } catch (IOException e) {
                            System.err.println("Ошибка при обработке " + p + ": " + e.getMessage());
                            return false;
                        }
                    })
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.toList());

            if (foundFiles.isEmpty()) {
                System.out.println("Строка не найдена ни в одном PDF-файле.");
            } else {
                System.out.println("Найдено в " + foundFiles.size() + " файлах:");
                foundFiles.forEach(System.out::println);
            }
        }
    }

    private static boolean searchInPDF(File pdfFile, String searchString) throws IOException {
        System.out.println("Проверка: " + pdfFile.getName());

        try (PDDocument document = PDDocument.load(pdfFile)) {
            if (document.isEncrypted()) {
                return false;
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document).toLowerCase();
            return text.contains(searchString);
        }
    }
}
