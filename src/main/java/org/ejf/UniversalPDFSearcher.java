package org.ejf;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UniversalPDFSearcher {

    private static final List<PDFSearcher> searchers = Arrays.asList(
            new ITextSearcher(),
            new PDFBoxSearcher(),
            new TabulaSearcher()
    );

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java UniversalPDFSearcher <folder> <search_text>");
            return;
        }

        Path folder = Paths.get(args[0]);
        String searchText = args[1];
        List<String> results = new ArrayList<>();

        Files.walk(folder)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                .forEach(pdfPath -> {
                    System.out.print("\nProcessing: " + pdfPath.getFileName() + " - ");

                    for (PDFSearcher searcher : searchers) {
                        try {
                            if (searcher.search(pdfPath, searchText)) {
                                results.add(pdfPath.toString());
                                System.out.println("FOUND with " + searcher.getName());
                                break;
                            }
                        } catch (Exception e) {
                            // Пробуем следующий поисковик
                        }
                    }
                });

        // Вывод результатов
        System.out.println("\n=== RESULTS ===");
        System.out.println("Total files found: " + results.size());
        results.forEach(System.out::println);
    }

    interface PDFSearcher {
        boolean search(Path pdfPath, String searchText) throws Exception;
        String getName();
    }

    static class ITextSearcher implements PDFSearcher {
        public boolean search(Path pdfPath, String searchText) throws Exception {
            // Реализация iText
            return false;
        }
        public String getName() { return "iText"; }
    }

    static class PDFBoxSearcher implements PDFSearcher {
        public boolean search(Path pdfPath, String searchText) throws Exception {
            // Реализация PDFBox
            return false;
        }
        public String getName() { return "PDFBox"; }
    }

    static class TabulaSearcher implements PDFSearcher {
        public boolean search(Path pdfPath, String searchText) throws Exception {
            // Реализация Tabula
            return false;
        }
        public String getName() { return "Tabula"; }
    }
}
