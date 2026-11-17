package core.utils.barcode;

import models.dto.StorageBinDTO;
import models.dto.WarehouseDTO;
import core.logging.Logger;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service to print storage bin labels matching C# barcode label printing specifications.
 * Supports printing single or multiple bins in bulk with configurable copies per bin.
 * Supported sizes (only those from C# code):
 *   - 100x20mm (3-column layout)
 *   - 50x25mm (2" x 1" single label)
 */
public class BinLabelPrintService {

    public enum LabelSize {
        SIZE_100x20("100x20mm", 100.0, 20.0),
        SIZE_50x25("50x25mm", 50.0, 25.0);

        private final String displayName;
        private final double widthMm;
        private final double heightMm;

        LabelSize(String displayName, double widthMm, double heightMm) {
            this.displayName = displayName;
            this.widthMm = widthMm;
            this.heightMm = heightMm;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getWidthMm() {
            return widthMm;
        }

        public double getHeightMm() {
            return heightMm;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static class BinPrintItem {
        private final StorageBinDTO bin;
        private int quantity;

        public BinPrintItem(StorageBinDTO bin, int quantity) {
            this.bin = bin;
            this.quantity = Math.max(quantity, 1);
        }

        public StorageBinDTO getBin() {
            return bin;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = Math.max(quantity, 1);
        }
    }

    private static final double MM_TO_POINTS = 72.0 / 25.4;

    /**
     * Prints multiple bin labels with user-selected label size and title.
     *
     * @param items       List of bins with quantities
     * @param warehouse   Warehouse info for title/header
     * @param size        Label size (100x20mm or 50x25mm)
     * @param customTitle Title text (defaults to warehouse name if empty)
     * @return true if printed successfully, false if cancelled or failed
     * @throws Exception on printing failure
     */
    public static boolean printBinLabels(List<BinPrintItem> items, WarehouseDTO warehouse,
                                         LabelSize size, String customTitle) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("No bins provided to print");
        }

        // Flatten items into sequential label list
        List<StorageBinDTO> labelsToPrint = new ArrayList<>();
        for (BinPrintItem item : items) {
            if (item.getBin() != null) {
                for (int q = 0; q < item.getQuantity(); q++) {
                    labelsToPrint.add(item.getBin());
                }
            }
        }

        if (labelsToPrint.isEmpty()) {
            throw new IllegalArgumentException("No valid bins found in print list");
        }

        String titleText = (customTitle != null && !customTitle.trim().isEmpty())
                ? customTitle.trim()
                : (warehouse != null && warehouse.getWarehouseName() != null ? warehouse.getWarehouseName() : "HIVON WMS");

        PrinterJob printerJob = PrinterJob.getPrinterJob();
        printerJob.setJobName("Bin Labels (" + labelsToPrint.size() + " items)");

        PageFormat pageFormat = printerJob.defaultPage();
        Paper paper = new Paper();

        Printable printable;
        if (size == LabelSize.SIZE_100x20) {
            // 96mm x 20mm paper for 100x20 layout
            double widthPt = 96.0 * MM_TO_POINTS;
            double heightPt = 20.0 * MM_TO_POINTS;
            paper.setSize(widthPt, heightPt);
            paper.setImageableArea(0, 0, widthPt, heightPt);
            pageFormat.setPaper(paper);
            pageFormat.setOrientation(PageFormat.PORTRAIT);

            printable = new Print100x20BarcodesPrintable(labelsToPrint, titleText);
        } else {
            // 50mm x 25mm (2" x 1")
            double widthPt = 50.0 * MM_TO_POINTS;
            double heightPt = 25.0 * MM_TO_POINTS;
            paper.setSize(widthPt, heightPt);
            paper.setImageableArea(0, 0, widthPt, heightPt);
            pageFormat.setPaper(paper);
            pageFormat.setOrientation(PageFormat.PORTRAIT);

            printable = new Print50x25BarcodesPrintable(labelsToPrint, titleText);
        }

        printerJob.setPrintable(printable, pageFormat);

        if (printerJob.printDialog()) {
            printerJob.print();
            Logger.log("PRINT", "Printed " + labelsToPrint.size() + " labels across " + items.size() + " bins [" + size.getDisplayName() + "]");
            return true;
        }
        return false;
    }

    /**
     * Backward compatible helper to print a single bin.
     */
    public static boolean printBinLabel(StorageBinDTO bin, WarehouseDTO warehouse,
                                        LabelSize size, int quantity, String customTitle) throws Exception {
        return printBinLabels(Collections.singletonList(new BinPrintItem(bin, quantity)), warehouse, size, customTitle);
    }

    /**
     * Printable for 50x25mm labels (1 label per page).
     */
    private static class Print50x25BarcodesPrintable implements Printable {
        private final List<StorageBinDTO> labels;
        private final String titleText;

        public Print50x25BarcodesPrintable(List<StorageBinDTO> labels, String titleText) {
            this.labels = labels;
            this.titleText = titleText;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex >= labels.size()) {
                return Printable.NO_SUCH_PAGE;
            }

            StorageBinDTO bin = labels.get(pageIndex);

            Graphics2D g = (Graphics2D) graphics;
            g.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            double labelWidth = 50.0 * MM_TO_POINTS;
            double labelHeight = 25.0 * MM_TO_POINTS;

            // 1. Black top header rectangle
            int rectHeight = 10;
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, (int) labelWidth, rectHeight);

            // 2. White title text centered in rectangle
            Font titleFont = new Font("Calibri", Font.BOLD, (int) (rectHeight * 0.75));
            g.setFont(titleFont);
            g.setColor(Color.WHITE);
            FontMetrics tfm = g.getFontMetrics(titleFont);
            float textX = (float) ((labelWidth - tfm.stringWidth(titleText)) / 2.0);
            float textY = (float) ((rectHeight - tfm.getHeight()) / 2.0 + tfm.getAscent());
            g.drawString(titleText, textX, textY);

            // 3. Barcode (38mm x 8mm)
            double barcodeWidth = 38.0 * MM_TO_POINTS;
            double barcodeHeight = 8.0 * MM_TO_POINTS;
            int xMargin = (int) ((labelWidth - barcodeWidth) / 2.0);
            int yMargin = rectHeight + 2;

            String binCode = bin.getBinCode() != null ? bin.getBinCode() : "N/A";
            BufferedImage barcodeImg = BarcodeGenerator.generateCode128(binCode, (int) (barcodeWidth * 4), (int) (barcodeHeight * 4));
            g.drawImage(barcodeImg, xMargin, yMargin, (int) barcodeWidth, (int) barcodeHeight, null);

            // 4. Item text below barcode: {binCode} - {zone}
            String zone = bin.getZone() != null ? bin.getZone() : (bin.getZoneCode() != null ? bin.getZoneCode() : "");
            String itemText = binCode + (zone.isEmpty() ? "" : " - " + zone);
            Font defaultFont = new Font("Arial", Font.BOLD, 7);
            g.setFont(defaultFont);
            g.setColor(Color.BLACK);
            FontMetrics ifm = g.getFontMetrics(defaultFont);
            float itemX = (float) ((labelWidth - ifm.stringWidth(itemText)) / 2.0);
            float itemY = (float) (yMargin + barcodeHeight + 2 + ifm.getAscent());
            g.drawString(itemText, itemX, itemY);

            // 5. Description text below item text
            String aisle = bin.getAisle() != null ? bin.getAisle() : "";
            String shelf = bin.getShelf() != null ? bin.getShelf() : "";
            String level = bin.getLevel() != null ? bin.getLevel() : "";
            String loc = (!aisle.isEmpty() || !shelf.isEmpty() || !level.isEmpty())
                    ? "A:" + aisle + " S:" + shelf + " L:" + level
                    : (bin.getBinDescription() != null ? bin.getBinDescription() : "");

            Font locFont = new Font("Arial", Font.BOLD, 5);
            g.setFont(locFont);
            FontMetrics lfm = g.getFontMetrics(locFont);
            float locX = (float) ((labelWidth - lfm.stringWidth(loc)) / 2.0);
            float locY = itemY + lfm.getHeight();
            g.drawString(loc, locX, locY);

            // 6. Bottom detail text: Type & Capacity
            String type = bin.getBinType() != null ? bin.getBinType() : "STORAGE";
            String cap = bin.getMaxCapacity() != null ? String.valueOf(bin.getMaxCapacity().intValue()) : "0";
            String rpText = type + " | Cap: " + cap;
            Font rpFont = new Font("Arial", Font.BOLD, 6);
            g.setFont(rpFont);
            FontMetrics rfm = g.getFontMetrics(rpFont);
            float rpX = (float) ((labelWidth - rfm.stringWidth(rpText)) / 2.0);
            float rpY = locY + rfm.getHeight() + 1;
            g.drawString(rpText, rpX, rpY);

            return Printable.PAGE_EXISTS;
        }
    }

    /**
     * Printable for 100x20mm labels (3 barcode areas across per page).
     */
    private static class Print100x20BarcodesPrintable implements Printable {
        private final List<StorageBinDTO> labels;
        private final String titleText;
        private final int totalPages;

        public Print100x20BarcodesPrintable(List<StorageBinDTO> labels, String titleText) {
            this.labels = labels;
            this.titleText = titleText;
            this.totalPages = (int) Math.ceil((double) labels.size() / 3.0);
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex >= totalPages) {
                return Printable.NO_SUCH_PAGE;
            }

            Graphics2D g = (Graphics2D) graphics;
            g.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            double barcodeAreaWidth = 30.0 * MM_TO_POINTS;
            double barcodeWidth = 26.0 * MM_TO_POINTS;
            double barcodeHeight = 8.0 * MM_TO_POINTS;
            double rectHeight = 2.5 * MM_TO_POINTS;
            double colSpacing = 2.0 * MM_TO_POINTS;

            Font titleFont = new Font("Calibri", Font.BOLD, 5);
            Font itemFont = new Font("Arial", Font.BOLD, 6);
            Font rpFont = new Font("Arial", Font.BOLD, 5);

            // Draw up to 3 columns across
            for (int col = 0; col < 3; col++) {
                int barcodeIdx = pageIndex * 3 + col;
                if (barcodeIdx >= labels.size()) {
                    break;
                }

                StorageBinDTO bin = labels.get(barcodeIdx);
                String binCode = bin.getBinCode() != null ? bin.getBinCode() : "N/A";
                BufferedImage barcodeImg = BarcodeGenerator.generateCode128(binCode, (int) (barcodeWidth * 4), (int) (barcodeHeight * 4));

                String zone = bin.getZone() != null ? bin.getZone() : (bin.getZoneCode() != null ? bin.getZoneCode() : "");
                String itemText = binCode + (zone.isEmpty() ? "" : " - " + zone);
                String rpText = (bin.getBinType() != null ? bin.getBinType() : "STORAGE") +
                        (bin.getMaxCapacity() != null ? " (" + bin.getMaxCapacity().intValue() + ")" : "");

                double xPos = col * (barcodeAreaWidth + colSpacing);
                double yPos = 0;

                // 1. Black rectangle
                g.setColor(Color.BLACK);
                g.fillRect((int) xPos, (int) yPos, (int) barcodeAreaWidth, (int) rectHeight);

                // 2. Title text in white
                g.setFont(titleFont);
                g.setColor(Color.WHITE);
                FontMetrics tfm = g.getFontMetrics(titleFont);
                float titleX = (float) (xPos + (barcodeAreaWidth - tfm.stringWidth(titleText)) / 2.0);
                float titleY = (float) (yPos + (rectHeight - tfm.getHeight()) / 2.0 + tfm.getAscent());
                g.drawString(titleText, titleX, titleY);

                // 3. Barcode below rectangle
                double barcodeY = yPos + rectHeight + 1;
                double barcodeX = xPos + (barcodeAreaWidth - barcodeWidth) / 2.0;
                g.drawImage(barcodeImg, (int) barcodeX, (int) barcodeY, (int) barcodeWidth, (int) barcodeHeight, null);

                // 4. Item text
                g.setFont(itemFont);
                g.setColor(Color.BLACK);
                FontMetrics ifm = g.getFontMetrics(itemFont);
                float itemX = (float) (xPos + (barcodeAreaWidth - ifm.stringWidth(itemText)) / 2.0);
                float itemY = (float) (barcodeY + barcodeHeight + 1 + ifm.getAscent());
                g.drawString(itemText, itemX, itemY);

                // 5. Detail text
                g.setFont(rpFont);
                FontMetrics rfm = g.getFontMetrics(rpFont);
                float rpX = (float) (xPos + (barcodeAreaWidth - rfm.stringWidth(rpText)) / 2.0);
                float rpY = itemY + rfm.getHeight();
                g.drawString(rpText, rpX, rpY);
            }

            return Printable.PAGE_EXISTS;
        }
    }
}
