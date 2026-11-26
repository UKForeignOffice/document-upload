package uk.gov.fco.documentupload.poc;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTransformationBenchmarkTest {

    private static final float JPEG_QUALITY = 0.85f;

    record BenchmarkCase(String name, byte[] pdfBytes, int expectedPages, boolean hasLinks, boolean hasEmbeddedFiles) {}

    record TransformResult(byte[] outputBytes, long durationMs, long peakHeapUsedMb, int linksRemaining, int embeddedFilesRemaining) {}

    @Test
    void benchmarkPdfRasterisation() throws Exception {
        List<BenchmarkCase> cases = List.of(
                new BenchmarkCase("Passport scan (1p)", createScannedPdf(1, 1500, 2000), 1, false, false),
                new BenchmarkCase("Max-size scan (~4MB, 5p)", createScannedPdf(5, 1350, 1800), 5, false, false),
                new BenchmarkCase("Links + embedded file (5p)", createMaliciousPdf(5), 5, true, true),
                new BenchmarkCase("Dense bank statement (30p)", createDenseTextPdf(30), 30, false, false)
        );

        rasterisePdf(cases.get(0).pdfBytes(), 150f);

        StringBuilder report = new StringBuilder();
        report.append("\n\n=== PDF Rasterisation Benchmark (PDFBox 3.0) ===\n");
        report.append("| Test PDF | Input Size | Pages | Time (150 DPI) | Output (150 DPI) | Time (200 DPI) | Output (200 DPI) | Heap Used (200 DPI) | Links / Files After |\n");
        report.append("|---|---|---|---|---|---|---|---|---|\n");

        for (BenchmarkCase testCase : cases) {
            if (testCase.hasLinks() || testCase.hasEmbeddedFiles()) {
                try (PDDocument before = Loader.loadPDF(testCase.pdfBytes())) {
                    assertTrue(countAnnotations(before) > 0, "Expected clickable links before transform");
                    assertTrue(countEmbeddedFiles(before) > 0, "Expected embedded files before transform");
                }
            }

            TransformResult r150 = rasterisePdf(testCase.pdfBytes(), 150f);
            TransformResult r200 = rasterisePdf(testCase.pdfBytes(), 200f);

            assertEquals(0, r150.linksRemaining(), "Links must be stripped at 150 DPI");
            assertEquals(0, r150.embeddedFilesRemaining(), "Embedded files must be stripped at 150 DPI");
            assertEquals(0, r200.linksRemaining(), "Links must be stripped at 200 DPI");
            assertEquals(0, r200.embeddedFilesRemaining(), "Embedded files must be stripped at 200 DPI");

            report.append(String.format(
                    "| %s | %.2f MB | %d | %.2fs | %.2f MB | %.2fs | %.2f MB | %d MB | %d / %d |\n",
                    testCase.name(),
                    toMb(testCase.pdfBytes().length),
                    testCase.expectedPages(),
                    r150.durationMs() / 1000.0,
                    toMb(r150.outputBytes().length),
                    r200.durationMs() / 1000.0,
                    toMb(r200.outputBytes().length),
                    r200.peakHeapUsedMb(),
                    r200.linksRemaining(),
                    r200.embeddedFilesRemaining()
            ));
        }

        byte[] encryptedPdf = createEncryptedPdf();
        long start = System.nanoTime();
        assertThrows(InvalidPasswordException.class, () -> rasterisePdf(encryptedPdf, 150f));
        long encryptedMs = (System.nanoTime() - start) / 1_000_000;
        report.append(String.format(
                "| Password-protected (1p) | %.2f MB | 1 | Rejected (%.2fs) | N/A | Rejected | N/A | - | Rejected |\n",
                toMb(encryptedPdf.length),
                encryptedMs / 1000.0
        ));

        System.out.println(report);
    }

    @Test
    void stressTestConcurrentPdfRasterisation() throws Exception {
        byte[] fourMbScan = createScannedPdf(5, 1350, 1800);
        byte[] thirtyPageText = createDenseTextPdf(30);

        rasterisePdf(fourMbScan, 150f);

        StringBuilder report = new StringBuilder();
        report.append("\n\n=== Concurrent Stress Test (150 DPI, JVM maxHeap=")
                .append(Runtime.getRuntime().maxMemory() / (1024 * 1024))
                .append(" MB) ===\n");
        report.append("| Scenario | Concurrency | Total Wall Time | Min | Avg | Max | Peak Heap Used | Failures |\n");
        report.append("|---|---|---|---|---|---|---|---|\n");

        report.append(runConcurrentScenario("4 MB scan (5p)", fourMbScan, 5));
        report.append(runConcurrentScenario("4 MB scan (5p)", fourMbScan, 10));
        report.append(runConcurrentScenario("Dense text (30p)", thirtyPageText, 5));
        report.append(runConcurrentScenario("Dense text (30p)", thirtyPageText, 10));

        System.out.println(report);
    }

    private String runConcurrentScenario(String label, byte[] pdfBytes, int concurrency) throws Exception {
        System.gc();
        long wallStart = System.nanoTime();
        long peakHeapMb = 0;
        int failures = 0;
        long minMs = Long.MAX_VALUE;
        long maxMs = 0;
        long totalMs = 0;

        try (ExecutorService pool = Executors.newFixedThreadPool(concurrency)) {
            List<Callable<TransformResult>> tasks = Collections.nCopies(concurrency, () -> rasterisePdf(pdfBytes, 150f));
            List<Future<TransformResult>> futures = pool.invokeAll(tasks);

            for (Future<TransformResult> f : futures) {
                try {
                    TransformResult r = f.get();
                    minMs = Math.min(minMs, r.durationMs());
                    maxMs = Math.max(maxMs, r.durationMs());
                    totalMs += r.durationMs();
                    peakHeapMb = Math.max(peakHeapMb, r.peakHeapUsedMb());
                } catch (Throwable t) {
                    failures++;
                }
            }
        }

        assertEquals(0, failures, "Expected zero failures for scenario: " + label);

        long wallMs = (System.nanoTime() - wallStart) / 1_000_000;
        int succeeded = concurrency - failures;
        double avgSec = succeeded > 0 ? (totalMs / (double) succeeded) / 1000.0 : 0.0;
        double minSec = succeeded > 0 ? minMs / 1000.0 : 0.0;
        double maxSec = succeeded > 0 ? maxMs / 1000.0 : 0.0;

        return String.format(
                "| %s | %d parallel | %.2fs | %.2fs | %.2fs | %.2fs | %d MB | %d |\n",
                label, concurrency, wallMs / 1000.0, minSec, avgSec, maxSec, peakHeapMb, failures
        );
    }

    private TransformResult rasterisePdf(byte[] inputBytes, float dpi) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long peakUsed = runtime.totalMemory() - runtime.freeMemory();
        long startNs = System.nanoTime();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PDDocument source = Loader.loadPDF(inputBytes);
             PDDocument clean = new PDDocument()) {

            PDFRenderer renderer = new PDFRenderer(source);
            for (int pageIndex = 0; pageIndex < source.getNumberOfPages(); pageIndex++) {
                BufferedImage rendered = renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
                peakUsed = Math.max(peakUsed, runtime.totalMemory() - runtime.freeMemory());

                PDRectangle mediaBox = source.getPage(pageIndex).getMediaBox();
                PDPage newPage = new PDPage(new PDRectangle(mediaBox.getWidth(), mediaBox.getHeight()));
                clean.addPage(newPage);

                PDImageXObject image = JPEGFactory.createFromImage(clean, rendered, JPEG_QUALITY);
                try (PDPageContentStream cs = new PDPageContentStream(clean, newPage)) {
                    cs.drawImage(image, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
                }
            }
            clean.save(out);
        }

        long durationMs = (System.nanoTime() - startNs) / 1_000_000;
        byte[] outputBytes = out.toByteArray();

        int linksAfter;
        int filesAfter;
        try (PDDocument verify = Loader.loadPDF(outputBytes)) {
            linksAfter = countAnnotations(verify);
            filesAfter = countEmbeddedFiles(verify);
        }

        return new TransformResult(outputBytes, durationMs, peakUsed / (1024 * 1024), linksAfter, filesAfter);
    }

    private int countAnnotations(PDDocument doc) throws IOException {
        int count = 0;
        for (PDPage page : doc.getPages()) {
            count += page.getAnnotations().size();
        }
        return count;
    }

    private int countEmbeddedFiles(PDDocument doc) throws IOException {
        PDDocumentNameDictionary names = doc.getDocumentCatalog().getNames();
        if (names == null || names.getEmbeddedFiles() == null || names.getEmbeddedFiles().getNames() == null) {
            return 0;
        }
        return names.getEmbeddedFiles().getNames().size();
    }

    private byte[] createScannedPdf(int pages, int width, int height) throws IOException {
        Random random = new Random(42);
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int p = 0; p < pages; p++) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                // Textured pseudo-scan image so JPEG compression produces realistic ~0.7-1.0 MB per page
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);
                for (int y = 0; y < height; y += 4) {
                    for (int x = 0; x < width; x += 4) {
                        int shade = 160 + random.nextInt(90);
                        g.setColor(new Color(shade, shade, shade));
                        g.fillRect(x, y, 4, 4);
                    }
                }
                g.setColor(Color.BLACK);
                g.drawString("Sample Scanned Document Page " + (p + 1), 100, 150);
                g.dispose();

                PDImageXObject pdImage = JPEGFactory.createFromImage(doc, img, 0.90f);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(pdImage, 0, 0, PDRectangle.A4.getWidth(), PDRectangle.A4.getHeight());
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    private byte[] createMaliciousPdf(int pages) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int p = 0; p < pages; p++) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(font, 12);
                    cs.newLineAtOffset(50, 750);
                    cs.showText("Document page " + (p + 1) + " with clickable link: https://malicious.example.test");
                    cs.endText();
                }

                PDAnnotationLink link = new PDAnnotationLink();
                link.setRectangle(new PDRectangle(50, 740, 300, 20));
                PDActionURI action = new PDActionURI();
                action.setURI("https://malicious.example.test/phish");
                link.setAction(action);
                page.getAnnotations().add(link);
            }

            // Attach an embedded file to reproduce the ITHC finding
            byte[] payload = "embedded-payload-content".getBytes(StandardCharsets.UTF_8);
            PDEmbeddedFile embeddedFile = new PDEmbeddedFile(doc, new ByteArrayInputStream(payload));
            embeddedFile.setSubtype("text/plain");
            embeddedFile.setSize(payload.length);

            PDComplexFileSpecification fs = new PDComplexFileSpecification();
            fs.setFile("hidden-attachment.txt");
            fs.setEmbeddedFile(embeddedFile);

            PDEmbeddedFilesNameTreeNode efTree = new PDEmbeddedFilesNameTreeNode();
            efTree.setNames(Collections.singletonMap("hidden-attachment.txt", fs));
            PDDocumentNameDictionary names = new PDDocumentNameDictionary(doc.getDocumentCatalog());
            names.setEmbeddedFiles(efTree);
            doc.getDocumentCatalog().setNames(names);

            doc.save(out);
            return out.toByteArray();
        }
    }

    private byte[] createDenseTextPdf(int pages) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int p = 0; p < pages; p++) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(font, 9);
                    cs.setLeading(12f);
                    cs.newLineAtOffset(40, 800);
                    for (int line = 0; line < 60; line++) {
                        cs.showText(String.format("2026-11-%02d  TXN-%04d-%02d  CARD PAYMENT LONDON UK   GBP %d.99",
                                (line % 28) + 1, p + 1, line + 1, (line * 17) % 500));
                        cs.newLine();
                    }
                    cs.endText();
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    private byte[] createEncryptedPdf() throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage(PDRectangle.A4));
            StandardProtectionPolicy policy = new StandardProtectionPolicy("owner-pass", "user-pass", new AccessPermission());
            policy.setEncryptionKeyLength(128);
            doc.protect(policy);
            doc.save(out);
            return out.toByteArray();
        }
    }

    private double toMb(int bytes) {
        return bytes / (1024.0 * 1024.0);
    }
}
