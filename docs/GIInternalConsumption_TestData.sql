-- ==============================================================================
-- Test Data for GIInternalConsumptionForm (Issue #101 / OUT16)
-- Target Database: hivon
-- Compatible with MySQL 8.0+ / 9.0+
-- ==============================================================================

-- 1. Ensure OUT16 movement type exists in movement_types
INSERT INTO `movement_types` (`movement_type_id`, `movement_code`, `movement_name`, `category`)
VALUES (16, 'OUT16', 'Goods Issue - Internal Consumption', 'OUTBOUND')
ON DUPLICATE KEY UPDATE `movement_name` = VALUES(`movement_name`), `category` = VALUES(`category`);

-- 2. Ensure test storage bins exist
INSERT INTO `storage_bins` (`bin_id`, `warehouse_id`, `bin_code`, `bin_description`, `zone_code`, `aisle`, `shelf`, `level`, `bin_type`, `max_capacity`, `max_weight`, `current_capacity`, `is_frozen`)
VALUES 
(101, 1, 'BIN-C-01', 'Maintenance Parts Rack A1', 'Z-MNT', 'A', '1', '1', 'STORAGE', 500.00, 1000.00, 35.00, 0),
(102, 1, 'BIN-C-02', 'Stationery & Consumables Shelf B1', 'Z-OFF', 'B', '1', '1', 'STORAGE', 500.00, 500.00, 60.00, 0),
(103, 1, 'BIN-C-03', 'Safety & PPE Storage C1', 'Z-PPE', 'C', '1', '1', 'STORAGE', 500.00, 500.00, 150.00, 0)
ON DUPLICATE KEY UPDATE 
    `bin_code` = VALUES(`bin_code`),
    `bin_description` = VALUES(`bin_description`),
    `is_frozen` = 0;


-- 3. Ensure test materials exist
INSERT INTO `materials` (`material_id`, `material_code`, `material_description`, `base_uom`, `weight`, `volume`, `material_group`, `storage_type`, `is_batch_managed`, `min_stock_level`, `max_stock_level`, `reorder_point`, `unit_cost`)
VALUES
(101, 'MNT-001', 'Lubricating Oil ISO VG 68 (20L Drum)', 'DRUM', 18.500, 0.025, 'MAINTENANCE', 'DRUM_RACK', 0, 5.000, 50.000, 10.000, 45.00),
(102, 'OFF-001', 'A4 Premium Copy Paper 80GSM (Box of 5)', 'BOX', 12.000, 0.015, 'OFFICE', 'SHELF', 0, 10.000, 100.000, 20.000, 12.50),
(103, 'LAB-001', 'Nitrile Chemical Examination Gloves (Box of 100)', 'BOX', 0.800, 0.002, 'SAFETY', 'SHELF', 0, 20.000, 300.000, 50.000, 8.00)
ON DUPLICATE KEY UPDATE
    `material_code` = VALUES(`material_code`),
    `material_description` = VALUES(`material_description`),
    `base_uom` = VALUES(`base_uom`),
    `unit_cost` = VALUES(`unit_cost`);

-- 4. Ensure inventory stock exists (Note: available_quantity and total_value are generated columns in MySQL)
INSERT INTO `inventory` (`inventory_id`, `material_id`, `batch_id`, `bin_id`, `quantity`, `committed_quantity`, `unit_cost`, `last_movement_date`)
VALUES
(101, 101, NULL, 101, 35.000, 0.000, 45.00, NOW()),
(102, 102, NULL, 102, 60.000, 0.000, 12.50, NOW()),
(103, 103, NULL, 103, 150.000, 0.000, 8.00, NOW())
ON DUPLICATE KEY UPDATE
    `quantity` = VALUES(`quantity`),
    `committed_quantity` = VALUES(`committed_quantity`),
    `unit_cost` = VALUES(`unit_cost`),
    `last_movement_date` = NOW();
