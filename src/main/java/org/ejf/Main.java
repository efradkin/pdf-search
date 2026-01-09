package org.ejf;

import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java PDFSearcherWithOCR <folder> <search_text>");
            return;
        }

        String folderPath = args[0];
        String searchText = args[1].toLowerCase();

        List<String> result = PDFSearcherWithOCR.process(folderPath, searchText);
        result.forEach(doc -> {
            System.out.println("https://o-maps.spb.ru/docs/" + doc.replace("\\", "/"));
        });
    }
}
