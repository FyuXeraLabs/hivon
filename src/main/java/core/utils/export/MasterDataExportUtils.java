/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.utils.export;

import models.dto.BatchDTO;
import models.dto.StorageBinDTO;
import models.dto.CustomerDTO;
import models.dto.UOMDTO;
import models.dto.UserDTO;
import models.dto.VendorDTO;
import models.dto.ZoneDTO;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Centralized export utility for Master Data forms:
 * - Batches (MD32)
 * - Storage Bins (MD33)
 * - Zones (MD34)
 * - Vendors (MD35)
 * - Customers (MD36)
 * - UOMs (MD37)
 * - Users (MD38)
 *
 * Supports XLSX via Apache POI and PDF via Apache PDFBox.
 *
 * @author Sanod
 */
public class MasterDataExportUtils {

    private static final DecimalFormat qtyFmt = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==========================================
    // 1. BATCHES (MD32)
    // ==========================================

    public static void exportBatchesToXLSX(List<BatchDTO> batches, File file) throws Exception {
        if (batches == null) throw new IllegalArgumentException("Batch data is null.");
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Batches");
            for (int i = 0; i < 7; i++) sheet.setColumnWidth(i, 20 * 256);

            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 11, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 10, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 10, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, null);

            int r = 0;
            row(sheet, r++, c(title, "HIVON WMS - BATCH MANAGEMENT (MD32)"));
            row(sheet, r++, c(bold, "Generated: " + LocalDateTime.now().format(dtf)));
            r++;

            row(sheet, r++, c(section, "BATCH RECORDS SUMMARY"));
            row(sheet, r++, c(bold, "Total Batches:"), c(bold, batches.size()));
            r++;

            row(sheet, r++,
                c(header, "Batch Number"),
                c(header, "Material Code"),
                c(header, "Material Description"),
                c(header, "Supplier Batch"),
                c(header, "Manufacture Date"),
                c(header, "Expiry Date"),
                c(header, "Quality Status")
            );

            for (BatchDTO b : batches) {
                row(sheet, r++,
                    c(normal, safe(b.getBatchNumber())),
                    c(normal, safe(b.getMaterialCode())),
                    c(normal, safe(b.getMaterialDescription())),
                    c(normal, safe(b.getSupplierBatch())),
                    c(normal, b.getManufactureDate() != null ? b.getManufactureDate().toString() : ""),
                    c(normal, b.getExpiryDate() != null ? b.getExpiryDate().toString() : ""),
                    c(normal, safe(b.getQualityStatus()))
                );
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    public static void exportBatchesToPDF(List<BatchDTO> batches, File file) throws Exception {
        if (batches == null) throw new IllegalArgumentException("Batch data is null.");
        file = ensureExtension(file, ".pdf");

        String[] headers = {"Batch Number", "Material Code", "Description", "Supplier Batch", "Mfg Date", "Expiry Date", "Quality Status"};
        float[] colW = { 100f, 90f, 180f, 100f, 75f, 75f, 90f };

        renderPdfDocument(file, "HIVON WMS - BATCH MANAGEMENT (MD32)", "Total Batches: " + batches.size(), headers, colW, (cs, fNormal, margin, y) -> {
            float currentY = y;
            for (BatchDTO b : batches) {
                String[] rowData = {
                    safe(b.getBatchNumber()),
                    safe(b.getMaterialCode()),
                    safe(b.getMaterialDescription()),
                    safe(b.getSupplierBatch()),
                    b.getManufactureDate() != null ? b.getManufactureDate().toString() : "-",
                    b.getExpiryDate() != null ? b.getExpiryDate().toString() : "-",
                    safe(b.getQualityStatus())
                };
                currentY = drawTableRow(cs, fNormal, 8.0f, margin, currentY, colW, rowData, false);
            }
            return currentY;
        }, batches.size());
    }

    // ==========================================
    // 2. STORAGE BINS (MD33)
    // ==========================================

    public static void exportBinsToXLSX(List<StorageBinDTO> bins, File file) throws Exception {
        if (bins == null) throw new IllegalArgumentException("Bin data is null.");
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Storage Bins");
            for (int i = 0; i < 9; i++) sheet.setColumnWidth(i, 18 * 256);

            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 11, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 10, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 10, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, null);
            CellStyle normalQty = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, "#,##0.00");

            int r = 0;
            row(sheet, r++, c(title, "HIVON WMS - STORAGE BIN MANAGEMENT (MD33)"));
            row(sheet, r++, c(bold, "Generated: " + LocalDateTime.now().format(dtf)));
            r++;

            row(sheet, r++, c(section, "STORAGE BIN SUMMARY"));
            row(sheet, r++, c(bold, "Total Bins:"), c(bold, bins.size()));
            r++;

            row(sheet, r++,
                c(header, "Bin Code"),
                c(header, "Zone"),
                c(header, "Aisle"),
                c(header, "Rack / Shelf"),
                c(header, "Level"),
                c(header, "Bin Type"),
                c(header, "Max Capacity"),
                c(header, "Used Capacity"),
                c(header, "Status")
            );

            for (StorageBinDTO b : bins) {
                row(sheet, r++,
                    c(normal, safe(b.getBinCode())),
                    c(normal, safe(b.getZoneCode())),
                    c(normal, safe(b.getAisle())),
                    c(normal, safe(b.getShelf())),
                    c(normal, safe(b.getLevel())),
                    c(normal, safe(b.getBinType())),
                    c(normalQty, b.getMaxCapacity()),
                    c(normalQty, b.getUsedCapacity()),
                    c(normal, (b.getIsFrozen() != null && b.getIsFrozen()) ? "Frozen" : "Active")
                );
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    public static void exportBinsToPDF(List<StorageBinDTO> bins, File file) throws Exception {
        if (bins == null) throw new IllegalArgumentException("Bin data is null.");
        file = ensureExtension(file, ".pdf");

        String[] headers = {"Bin Code", "Zone", "Aisle", "Rack", "Level", "Bin Type", "Max Cap", "Used Cap", "Status"};
        float[] colW = { 95f, 80f, 60f, 60f, 60f, 95f, 75f, 75f, 70f };

        renderPdfDocument(file, "HIVON WMS - STORAGE BIN MANAGEMENT (MD33)", "Total Bins: " + bins.size(), headers, colW, (cs, fNormal, margin, y) -> {
            float currentY = y;
            for (StorageBinDTO b : bins) {
                String[] rowData = {
                    safe(b.getBinCode()),
                    safe(b.getZoneCode()),
                    safe(b.getAisle()),
                    safe(b.getShelf()),
                    safe(b.getLevel()),
                    safe(b.getBinType()),
                    b.getMaxCapacity() != null ? qtyFmt.format(b.getMaxCapacity()) : "-",
                    b.getUsedCapacity() != null ? qtyFmt.format(b.getUsedCapacity()) : "-",
                    (b.getIsFrozen() != null && b.getIsFrozen()) ? "Frozen" : "Active"
                };
                currentY = drawTableRow(cs, fNormal, 8.0f, margin, currentY, colW, rowData, false);
            }
            return currentY;
        }, bins.size());
    }

    // ==========================================
    // 3. CUSTOMERS (MD36)
    // ==========================================

    public static void exportCustomersToXLSX(List<CustomerDTO> customers, File file) throws Exception {
        if (customers == null) throw new IllegalArgumentException("Customer data is null.");
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Customers");
            for (int i = 0; i < 7; i++) sheet.setColumnWidth(i, 20 * 256);

            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 11, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 10, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 10, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, null);

            int r = 0;
            row(sheet, r++, c(title, "HIVON WMS - CUSTOMER MASTER (MD36)"));
            row(sheet, r++, c(bold, "Generated: " + LocalDateTime.now().format(dtf)));
            r++;

            row(sheet, r++, c(section, "CUSTOMER SUMMARY"));
            row(sheet, r++, c(bold, "Total Customers:"), c(bold, customers.size()));
            r++;

            row(sheet, r++,
                c(header, "Customer Code"),
                c(header, "Customer Name"),
                c(header, "Contact Person"),
                c(header, "Phone"),
                c(header, "Email"),
                c(header, "Address"),
                c(header, "Shipping Address")
            );

            for (CustomerDTO c : customers) {
                row(sheet, r++,
                    c(normal, safe(c.getCustomerCode())),
                    c(normal, safe(c.getCustomerName())),
                    c(normal, safe(c.getContactPerson())),
                    c(normal, safe(c.getPhone())),
                    c(normal, safe(c.getEmail())),
                    c(normal, safe(c.getAddress())),
                    c(normal, safe(c.getShippingAddress()))
                );
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    public static void exportCustomersToPDF(List<CustomerDTO> customers, File file) throws Exception {
        if (customers == null) throw new IllegalArgumentException("Customer data is null.");
        file = ensureExtension(file, ".pdf");

        String[] headers = {"Code", "Customer Name", "Contact Person", "Phone", "Email", "Address"};
        float[] colW = { 70f, 150f, 110f, 90f, 140f, 210f };

        renderPdfDocument(file, "HIVON WMS - CUSTOMER MASTER (MD36)", "Total Customers: " + customers.size(), headers, colW, (cs, fNormal, margin, y) -> {
            float currentY = y;
            for (CustomerDTO c : customers) {
                String[] rowData = {
                    safe(c.getCustomerCode()),
                    safe(c.getCustomerName()),
                    safe(c.getContactPerson()),
                    safe(c.getPhone()),
                    safe(c.getEmail()),
                    safe(c.getAddress())
                };
                currentY = drawTableRow(cs, fNormal, 8.0f, margin, currentY, colW, rowData, false);
            }
            return currentY;
        }, customers.size());
    }

    // ==========================================
    // 4. UNITS OF MEASURE (MD37)
    // ==========================================

    public static void exportUOMsToXLSX(List<UOMDTO> uoms, File file) throws Exception {
        if (uoms == null) throw new IllegalArgumentException("UOM data is null.");
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("UOMs");
            for (int i = 0; i < 4; i++) sheet.setColumnWidth(i, 22 * 256);

            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 11, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 10, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 10, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, null);

            int r = 0;
            row(sheet, r++, c(title, "HIVON WMS - UNIT OF MEASURE (MD37)"));
            row(sheet, r++, c(bold, "Generated: " + LocalDateTime.now().format(dtf)));
            r++;

            row(sheet, r++, c(section, "UOM SUMMARY"));
            row(sheet, r++, c(bold, "Total Units of Measure:"), c(bold, uoms.size()));
            r++;

            row(sheet, r++,
                c(header, "UOM Code"),
                c(header, "UOM Name"),
                c(header, "Description"),
                c(header, "Category")
            );

            for (UOMDTO u : uoms) {
                row(sheet, r++,
                    c(normal, safe(u.getParamKey())),
                    c(normal, safe(u.getParamValue())),
                    c(normal, safe(u.getDescription())),
                    c(normal, safe(u.getCategory()))
                );
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    public static void exportUOMsToPDF(List<UOMDTO> uoms, File file) throws Exception {
        if (uoms == null) throw new IllegalArgumentException("UOM data is null.");
        file = ensureExtension(file, ".pdf");

        String[] headers = {"UOM Code", "UOM Name", "Description", "Category"};
        float[] colW = { 120f, 200f, 300f, 150f };

        renderPdfDocument(file, "HIVON WMS - UNIT OF MEASURE (MD37)", "Total Units of Measure: " + uoms.size(), headers, colW, (cs, fNormal, margin, y) -> {
            float currentY = y;
            for (UOMDTO u : uoms) {
                String[] rowData = {
                    safe(u.getParamKey()),
                    safe(u.getParamValue()),
                    safe(u.getDescription()),
                    safe(u.getCategory())
                };
                currentY = drawTableRow(cs, fNormal, 8.5f, margin, currentY, colW, rowData, false);
            }
            return currentY;
        }, uoms.size());
    }

    // ==========================================
    // 5. USERS (MD38)
    // ==========================================

    public static void exportUsersToXLSX(List<UserDTO> users, File file) throws Exception {
        if (users == null) throw new IllegalArgumentException("User data is null.");
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Users");
            for (int i = 0; i < 7; i++) sheet.setColumnWidth(i, 20 * 256);

            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 11, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 10, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 10, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, null);

            int r = 0;
            row(sheet, r++, c(title, "HIVON WMS - USER MANAGEMENT (MD38)"));
            row(sheet, r++, c(bold, "Generated: " + LocalDateTime.now().format(dtf)));
            r++;

            long activeCount = users.stream().filter(u -> u.getIsActive() != null && u.getIsActive()).count();
            row(sheet, r++, c(section, "USER DIRECTORY SUMMARY"));
            row(sheet, r++, c(bold, "Total Users:"), c(bold, users.size()),
                            c(bold, "Active Users:"), c(bold, activeCount),
                            c(bold, "Inactive:"), c(bold, (users.size() - activeCount)));
            r++;

            row(sheet, r++,
                c(header, "Username"),
                c(header, "Full Name"),
                c(header, "Email"),
                c(header, "Role"),
                c(header, "Status"),
                c(header, "Last Login"),
                c(header, "Created Date")
            );

            for (UserDTO u : users) {
                row(sheet, r++,
                    c(normal, safe(u.getUsername())),
                    c(normal, safe(u.getFullName())),
                    c(normal, safe(u.getEmail())),
                    c(normal, safe(u.getRole())),
                    c(normal, (u.getIsActive() != null && u.getIsActive()) ? "Active" : "Inactive"),
                    c(normal, u.getLastLogin() != null ? u.getLastLogin().format(dtf) : "-"),
                    c(normal, u.getCreatedDate() != null ? u.getCreatedDate().format(dtf) : "-")
                );
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    public static void exportUsersToPDF(List<UserDTO> users, File file) throws Exception {
        if (users == null) throw new IllegalArgumentException("User data is null.");
        file = ensureExtension(file, ".pdf");

        String[] headers = {"Username", "Full Name", "Email", "Role", "Status", "Last Login"};
        float[] colW = { 100f, 150f, 180f, 100f, 80f, 160f };

        renderPdfDocument(file, "HIVON WMS - USER MANAGEMENT (MD38)", "Total Users: " + users.size(), headers, colW, (cs, fNormal, margin, y) -> {
            float currentY = y;
            for (UserDTO u : users) {
                String[] rowData = {
                    safe(u.getUsername()),
                    safe(u.getFullName()),
                    safe(u.getEmail()),
                    safe(u.getRole()),
                    (u.getIsActive() != null && u.getIsActive()) ? "Active" : "Inactive",
                    u.getLastLogin() != null ? u.getLastLogin().format(dtf) : "-"
                };
                currentY = drawTableRow(cs, fNormal, 8.0f, margin, currentY, colW, rowData, false);
            }
            return currentY;
        }, users.size());
    }

    // ==========================================
    // 6. VENDORS (MD35)
    // ==========================================

    public static void exportVendorsToXLSX(List<VendorDTO> vendors, File file) throws Exception {
        if (vendors == null) throw new IllegalArgumentException("Vendor data is null.");
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Vendors");
            for (int i = 0; i < 6; i++) sheet.setColumnWidth(i, 20 * 256);

            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 11, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 10, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 10, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, null);

            int r = 0;
            row(sheet, r++, c(title, "HIVON WMS - VENDOR MASTER (MD35)"));
            row(sheet, r++, c(bold, "Generated: " + LocalDateTime.now().format(dtf)));
            r++;

            row(sheet, r++, c(section, "VENDOR SUMMARY"));
            row(sheet, r++, c(bold, "Total Vendors:"), c(bold, vendors.size()));
            r++;

            row(sheet, r++,
                c(header, "Vendor Code"),
                c(header, "Vendor Name"),
                c(header, "Contact Person"),
                c(header, "Phone"),
                c(header, "Email"),
                c(header, "Address")
            );

            for (VendorDTO v : vendors) {
                row(sheet, r++,
                    c(normal, safe(v.getVendorCode())),
                    c(normal, safe(v.getVendorName())),
                    c(normal, safe(v.getContactPerson())),
                    c(normal, safe(v.getPhone())),
                    c(normal, safe(v.getEmail())),
                    c(normal, safe(v.getAddress()))
                );
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    public static void exportVendorsToPDF(List<VendorDTO> vendors, File file) throws Exception {
        if (vendors == null) throw new IllegalArgumentException("Vendor data is null.");
        file = ensureExtension(file, ".pdf");

        String[] headers = {"Code", "Vendor Name", "Contact Person", "Phone", "Email", "Address"};
        float[] colW = { 70f, 150f, 110f, 90f, 140f, 210f };

        renderPdfDocument(file, "HIVON WMS - VENDOR MASTER (MD35)", "Total Vendors: " + vendors.size(), headers, colW, (cs, fNormal, margin, y) -> {
            float currentY = y;
            for (VendorDTO v : vendors) {
                String[] rowData = {
                    safe(v.getVendorCode()),
                    safe(v.getVendorName()),
                    safe(v.getContactPerson()),
                    safe(v.getPhone()),
                    safe(v.getEmail()),
                    safe(v.getAddress())
                };
                currentY = drawTableRow(cs, fNormal, 8.0f, margin, currentY, colW, rowData, false);
            }
            return currentY;
        }, vendors.size());
    }

    // ==========================================
    // 7. ZONES (MD34)
    // ==========================================

    public static void exportZonesToXLSX(List<ZoneDTO> zones, File file) throws Exception {
        if (zones == null) throw new IllegalArgumentException("Zone data is null.");
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Zones");
            for (int i = 0; i < 7; i++) sheet.setColumnWidth(i, 20 * 256);

            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 11, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 10, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 10, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, null);
            CellStyle normalQty = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, "#,##0.00");

            int r = 0;
            row(sheet, r++, c(title, "HIVON WMS - ZONE MANAGEMENT (MD34)"));
            row(sheet, r++, c(bold, "Generated: " + LocalDateTime.now().format(dtf)));
            r++;

            row(sheet, r++, c(section, "ZONE SUMMARY"));
            row(sheet, r++, c(bold, "Total Zones:"), c(bold, zones.size()));
            r++;

            row(sheet, r++,
                c(header, "Zone Code"),
                c(header, "Zone Name"),
                c(header, "Zone Type"),
                c(header, "Warehouse"),
                c(header, "Total Bins"),
                c(header, "Total Capacity"),
                c(header, "Utilization")
            );

            for (ZoneDTO z : zones) {
                row(sheet, r++,
                    c(normal, safe(z.getZoneCode())),
                    c(normal, safe(z.getZoneName())),
                    c(normal, safe(z.getZoneType())),
                    c(normal, safe(z.getWarehouseName())),
                    c(bold, z.getTotalBins() != null ? z.getTotalBins() : 0),
                    c(normalQty, z.getTotalCapacity() != null ? z.getTotalCapacity().doubleValue() : 0),
                    c(normal, String.format("%.1f%%", z.getUtilizationPercent()))
                );
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    public static void exportZonesToPDF(List<ZoneDTO> zones, File file) throws Exception {
        if (zones == null) throw new IllegalArgumentException("Zone data is null.");
        file = ensureExtension(file, ".pdf");

        String[] headers = {"Zone Code", "Zone Name", "Zone Type", "Warehouse", "Total Bins", "Total Capacity", "Utilization"};
        float[] colW = { 85f, 150f, 95f, 160f, 75f, 110f, 95f };

        renderPdfDocument(file, "HIVON WMS - ZONE MANAGEMENT (MD34)", "Total Zones: " + zones.size(), headers, colW, (cs, fNormal, margin, y) -> {
            float currentY = y;
            for (ZoneDTO z : zones) {
                String[] rowData = {
                    safe(z.getZoneCode()),
                    safe(z.getZoneName()),
                    safe(z.getZoneType()),
                    safe(z.getWarehouseName()),
                    String.valueOf(z.getTotalBins() != null ? z.getTotalBins() : 0),
                    z.getTotalCapacity() != null ? qtyFmt.format(z.getTotalCapacity()) : "0.00",
                    String.format("%.1f%%", z.getUtilizationPercent())
                };
                currentY = drawTableRow(cs, fNormal, 8.0f, margin, currentY, colW, rowData, false);
            }
            return currentY;
        }, zones.size());
    }

    // ==========================================
    // SHARED PDF & EXCEL HELPER UTILITIES
    // ==========================================

    @FunctionalInterface
    public interface PdfRowRenderer {
        float render(PDPageContentStream cs, PDFont fNormal, float margin, float y) throws Exception;
    }

    private static void renderPdfDocument(File file, String title, String subtitle, String[] headers, float[] colW,
                                          PdfRowRenderer rowRenderer, int totalCount) throws Exception {
        PDRectangle pageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        float pageW = pageSize.getWidth();
        float pageH = pageSize.getHeight();
        float margin = 36f;

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            PDFont fTitle = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float y = pageH - margin;

            // Title Header Banner
            cs.setNonStrokingColor(0.12f, 0.23f, 0.36f);
            cs.addRect(margin, y - 24, pageW - 2 * margin, 28);
            cs.fill();

            cs.setNonStrokingColor(1, 1, 1);
            text(cs, fTitle, 13f, margin + 10, y - 16, title);

            y -= 38;
            cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);
            text(cs, fNormal, 8.5f, margin, y, "Generated: " + LocalDateTime.now().format(dtf) + "   |   " + subtitle);

            y -= 22;

            // Summary Card Box
            cs.setNonStrokingColor(0.95f, 0.96f, 0.98f);
            cs.addRect(margin, y - 20, pageW - 2 * margin, 24);
            cs.fill();
            cs.setStrokingColor(0.85f, 0.88f, 0.92f);
            cs.addRect(margin, y - 20, pageW - 2 * margin, 24);
            cs.stroke();

            cs.setNonStrokingColor(0.15f, 0.2f, 0.25f);
            float sumY = y - 14;
            text(cs, fBold, 8.5f, margin + 12, sumY, subtitle);

            y -= 34;

            // Table headers
            y = drawTableRow(cs, fBold, 8.0f, margin, y, colW, headers, true);

            // Table rows
            rowRenderer.render(cs, fNormal, margin, y);

            cs.close();
            doc.save(file);
        }
    }

    private static float drawTableRow(PDPageContentStream cs, PDFont font, float fontSize,
                                      float startX, float y, float[] colW, String[] data,
                                      boolean isHeader) throws Exception {
        float rowHeight = 16f;

        if (isHeader) {
            cs.setNonStrokingColor(0.12f, 0.23f, 0.36f);
            float totalW = 0;
            for (float w : colW) totalW += w;
            cs.addRect(startX, y - 4, totalW, rowHeight);
            cs.fill();
            cs.setNonStrokingColor(1, 1, 1);
        } else {
            float totalW = 0;
            for (float w : colW) totalW += w;

            cs.setStrokingColor(0.92f, 0.92f, 0.92f);
            cs.moveTo(startX, y - 4);
            cs.lineTo(startX + totalW, y - 4);
            cs.stroke();

            cs.setNonStrokingColor(0.1f, 0.1f, 0.1f);
        }

        float curX = startX;
        for (int i = 0; i < colW.length && i < data.length; i++) {
            String text = data[i];
            if (text == null) text = "";
            int maxChars = (int) (colW[i] / (fontSize * 0.52f));
            if (text.length() > maxChars && maxChars > 3) {
                text = text.substring(0, maxChars - 3) + "...";
            }
            text(cs, font, fontSize, curX + 3, y, text);
            curX += colW[i];
        }

        cs.setNonStrokingColor(0, 0, 0);
        return y - rowHeight;
    }

    private static void text(PDPageContentStream cs, PDFont font, float size, float x, float y, String s) throws Exception {
        if (s == null || s.isEmpty()) return;
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(s);
        cs.endText();
    }

    private static CellStyle style(Workbook wb, Short fg, Short bg, int pts, boolean b, String fmt) {
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontName("Segoe UI");
        f.setFontHeightInPoints((short) pts);
        f.setBold(b);
        if (fg != null) f.setColor(fg);
        cs.setFont(f);
        if (bg != null) {
            cs.setFillForegroundColor(bg);
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (fmt != null) cs.setDataFormat(wb.createDataFormat().getFormat(fmt));
        return cs;
    }

    private static void row(Sheet s, int idx, CellVal... cells) {
        Row r = s.createRow(idx);
        for (int i = 0; i < cells.length; i++) {
            Cell c = r.createCell(i);
            c.setCellStyle(cells[i].style);
            Object v = cells[i].val;
            if (v instanceof Number) {
                c.setCellValue(((Number) v).doubleValue());
            } else {
                c.setCellValue(v != null ? v.toString() : "");
            }
        }
    }

    private static CellVal c(CellStyle s, Object v) {
        return new CellVal(s, v);
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static File ensureExtension(File f, String ext) {
        return f.getName().toLowerCase().endsWith(ext) ? f : new File(f.getAbsolutePath() + ext);
    }

    private static class CellVal {
        CellStyle style;
        Object val;
        CellVal(CellStyle s, Object v) {
            this.style = s;
            this.val = v;
        }
    }
}
