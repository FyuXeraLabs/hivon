package masterdata.controllers;

import core.api.dao.MaterialDAO;
import core.security.UserSession;
import core.logging.Logger;
import models.dto.MaterialDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Material master data operations.
 * Handles fetching, searching, creating, updating, and deleting materials.
 *
 * @author Sanod
 */
public class MaterialController {

    private final String username = UserSession.getInstance().getUsername();

    public MaterialController() {
    }

    // retrieves all materials from the system
    public List<MaterialDTO> getAllMaterials() {
        try {
            return MaterialDAO.getInstance().getMaterials(null);
        } catch (Exception e) {
            Logger.errlog("failed to get all materials: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // searches materials by the given search term
    public List<MaterialDTO> searchMaterials(String searchTerm) {
        try {
            return MaterialDAO.getInstance().getMaterials(searchTerm);
        } catch (Exception e) {
            Logger.errlog("search materials failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // retrieves a single material by its ID
    public MaterialDTO getMaterialById(int materialId) {
        try {
            return MaterialDAO.getInstance().getMaterialById(materialId);
        } catch (Exception e) {
            Logger.errlog("get material by id failed: " + e.getMessage(), e);
            return null;
        }
    }

    // creates a new material record; returns the new material ID on success
    public int createMaterial(MaterialDTO materialDto) {
        if (materialDto == null || materialDto.getMaterialCode() == null || materialDto.getMaterialCode().trim().isEmpty()) {
            Logger.errlog("create material failed: invalid material data", new IllegalArgumentException("Invalid material data"));
            return 0;
        }
        try {
            int materialId = MaterialDAO.getInstance().createMaterial(materialDto);
            if (materialId > 0) {
                Logger.log(username, "material created successfully: " + materialDto.getMaterialCode() + " (id: " + materialId + ")");
            }
            return materialId;
        } catch (Exception e) {
            Logger.errlog("create material failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // updates an existing material record
    public boolean updateMaterial(MaterialDTO materialDto) {
        if (materialDto == null || materialDto.getMaterialId() == null) {
            Logger.errlog("update material failed: invalid material data", new IllegalArgumentException("Invalid material data"));
            throw new IllegalArgumentException("Invalid material data");
        }
        try {
            boolean success = MaterialDAO.getInstance().updateMaterial(materialDto);
            if (success) {
                Logger.log(username, "material updated successfully: " + materialDto.getMaterialCode() + " (id: " + materialDto.getMaterialId() + ")");
            }
            return success;
        } catch (Exception e) {
            Logger.errlog("update material failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // deletes a material by its ID
    public boolean deleteMaterial(int materialId) {
        if (materialId <= 0) {
            Logger.errlog("delete material failed: invalid material id", new IllegalArgumentException("Invalid material id"));
            throw new IllegalArgumentException("Invalid material id");
        }
        try {
            boolean success = MaterialDAO.getInstance().deleteMaterial(materialId);
            if (success) {
                Logger.log(username, "material deleted successfully: id=" + materialId);
            }
            return success;
        } catch (Exception e) {
            Logger.errlog("failed to delete material: id=" + materialId + " - " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}