package org.ejf;

/*
import technology.tabula.*;
import technology.tabula.extractors.*;
*/
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Этот метод совсем не проверял
 */
public class PDFTextSearcherTabula {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java PDFTextSearcherTabula <folder> <search_text>");
            return;
        }

        String folderPath = args[0];
        String searchString = args[1].toLowerCase();
        List<String> results = new ArrayList<>();

        Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".pdf"))
                .forEach(pdfPath -> {
                    try {
                        if (searchInPDF(pdfPath, searchString)) {
                            results.add(pdfPath.toString());
                        }
                    } catch (Exception e) {
                        System.err.println("Error: " + e.getMessage());
                    }
                });

        if (results.isEmpty()) {
            System.out.println("Not found");
        } else {
            System.out.println("Found in " + results.size() + " files:");
            results.forEach(System.out::println);
        }
    }

    private static boolean searchInPDF(Path pdfPath, String searchText) throws IOException {
        System.out.print("Processing: " + pdfPath.getFileName() + "... ");

        StringBuilder allText = new StringBuilder();
        try (InputStream is = new FileInputStream(pdfPath.toFile())) {
/*
            ObjectExtractor oe = new ObjectExtractor(is);
            PageIterator pi = oe.extract();

            while (pi.hasNext()) {
                Page page = pi.next();
                List<TextElement> textElements = page.getText();

                for (TextElement te : textElements) {
                    allText.append(te.getText()).append(" ");

                    // Проверяем по мере накопления
                    if (allText.toString().toLowerCase().contains(searchText)) {
                        System.out.println("FOUND");
                        return true;
                    }
                }
            }
*/

            boolean found = allText.toString().toLowerCase().contains(searchText);
            System.out.println(found ? "FOUND" : "not found");
            return found;
        }
    }
}
