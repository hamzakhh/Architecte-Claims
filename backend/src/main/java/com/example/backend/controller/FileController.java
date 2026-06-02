package com.example.backend.controller;

import com.example.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des fichiers (upload et download).
 * Toutes les routes sont préfixées par /api/files.
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * Upload un ou plusieurs fichiers.
     * Retourne la liste des noms de fichiers stockés.
     *
     * @param files les fichiers à uploader
     * @return la liste des noms générés
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ASSURE', 'EXPERT', 'GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<List<String>> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        List<String> storedNames = new ArrayList<>();
        for (MultipartFile file : files) {
            String storedName = fileStorageService.storeFile(file);
            storedNames.add(storedName);
        }
        log.info("{} fichier(s) uploadé(s)", storedNames.size());
        return ResponseEntity.ok(storedNames);
    }

    /**
     * Upload un seul fichier.
     *
     * @param file le fichier à uploader
     * @return le nom du fichier stocké
     */
    @PostMapping("/upload-single")
    @PreAuthorize("hasAnyRole('ASSURE', 'EXPERT', 'GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<Map<String, String>> uploadSingleFile(@RequestParam("file") MultipartFile file) {
        String storedName = fileStorageService.storeFile(file);
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        log.info("Fichier uploadé : {} -> {}", originalName, storedName);
        return ResponseEntity.ok(Map.of(
                "storedName", storedName,
                "originalName", originalName
        ));
    }

    /**
     * Supprime un fichier du stockage.
     *
     * @param filename le nom du fichier stocké
     * @return 200 si supprimé, 404 si non trouvé
     */
    @DeleteMapping("/{filename:.+}")
    @PreAuthorize("hasAnyRole('ASSURE', 'EXPERT', 'GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<Void> deleteFile(@PathVariable String filename) {
        try {
            Path filePath = fileStorageService.loadFile(filename);
            if (!java.nio.file.Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            fileStorageService.deleteFile(filename);
            log.info("Fichier supprimé : {}", filename);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.warn("Fichier non trouvé pour suppression : {}", filename);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Télécharge un fichier par son nom stocké.
     *
     * @param filename le nom du fichier stocké
     * @return le fichier en tant que ressource téléchargeable
     */
    @GetMapping("/download/{filename:.+}")
    @PreAuthorize("hasAnyRole('ASSURE', 'EXPERT', 'GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            Path filePath = fileStorageService.loadFile(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Déterminer le type MIME
            String contentType = "application/octet-stream";
            String fileName = filePath.getFileName().toString();
            if (fileName.endsWith(".pdf")) contentType = "application/pdf";
            else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (fileName.endsWith(".png")) contentType = "image/png";
            else if (fileName.endsWith(".gif")) contentType = "image/gif";
            else if (fileName.endsWith(".webp")) contentType = "image/webp";

            String encodedFilename = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                    .body(resource);
        } catch (Exception e) {
            log.error("Erreur lors du téléchargement du fichier {} : {}", filename, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
