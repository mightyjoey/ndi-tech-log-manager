package org.example.m11techlogapp.service;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.example.m11techlogapp.model.InspectionMethod;
import org.example.m11techlogapp.model.LogEntry;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PdfLogExporter {

    @FunctionalInterface
    public interface PageFiller {
        void fill(PDAcroForm acroForm, LinkedList<LogEntry> entries, String method, MonthDay splitDate) throws Exception;
    }

    public void export(
            File outputFolder,
            String outputPdfName,
            List<LogEntry> sourceEntries,
            String templatePdfName,
            boolean splitAllInspections,
            String selectedMethod,
            LocalDate annualLogStartDate,
            Class<?> resourceOwner,
            PageFiller pageFiller) throws Exception {

        PDFMergerUtility merger = new PDFMergerUtility();
        File outputFile = new File(outputFolder, outputPdfName);
        merger.setDestinationFileName(outputFile.getAbsolutePath());

        int pageCount = 1;
        MonthDay splitDate = annualLogStartDate == null ? null : MonthDay.from(annualLogStartDate);

        if (splitAllInspections) {
            for (Map.Entry<String, LinkedList<LogEntry>> methodEntries : InspectionMethod.groupEntriesByMethod(sourceEntries).entrySet()) {
                pageCount = addPages(
                        merger,
                        pageCount,
                        methodEntries.getValue(),
                        methodEntries.getKey(),
                        splitDate,
                        templatePdfName,
                        resourceOwner,
                        pageFiller);
            }
        } else {
            pageCount = addPages(
                    merger,
                    pageCount,
                    new LinkedList<>(sourceEntries),
                    selectedMethod,
                    splitDate,
                    templatePdfName,
                    resourceOwner,
                    pageFiller);
        }

        if (pageCount > 1) {
            merger.mergeDocuments(null);
            System.out.println("Saved merged PDF: " + outputFile.getAbsolutePath());
        }
    }

    private int addPages(
            PDFMergerUtility merger,
            int pageCount,
            LinkedList<LogEntry> entries,
            String method,
            MonthDay splitDate,
            String templatePdfName,
            Class<?> resourceOwner,
            PageFiller pageFiller) throws Exception {

        while (!entries.isEmpty()) {
            try (InputStream templateStream = resourceOwner.getClassLoader().getResourceAsStream(templatePdfName);
                 PDDocument document = PDDocument.load(templateStream)) {

                PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
                pageFiller.fill(acroForm, entries, method, splitDate);

                if (acroForm != null) {
                    acroForm.flatten();
                }

                File tempPage = File.createTempFile("page_" + pageCount++, ".pdf");
                tempPage.deleteOnExit();
                document.save(tempPage);
                merger.addSource(tempPage);
            }
        }

        return pageCount;
    }
}
