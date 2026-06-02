package com.example.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service de stockage des fichiers sur le système de fichiers local.
 * Les fichiers sont sauvegardés dans un répertoire configurable (upload.dir).
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("Répertoire de stockage initialisé : {}", rootLocation);
        } catch (IOException e) {
            log.error("Impossible de créer le répertoire de stockage : {}", rootLocation, e);
            throw new RuntimeException("Impossible de créer le répertoire de stockage", e);
        }
    }

    /**
     * Sauvegarde un fichier et retourne le nom unique généré.
     *
     * @param file le fichier multipart à sauvegarder
     * @return le nom du fichier sauvegardé (UUID + extension)
     */
    public String storeFile(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"
        );

        // Extraire l'extension du fichier
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }

        // Générer un nom unique pour éviter les conflits
        String storedName = UUID.randomUUID().toString() + extension;

        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Le fichier est vide : " + originalFilename);
            }
            if (originalFilename.contains("..")) {
                throw new RuntimeException("Nom de fichier invalide : " + originalFilename);
            }

            Path targetLocation = rootLocation.resolve(storedName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Fichier sauvegardé : {} -> {}", originalFilename, storedName);

            return storedName;
        } catch (IOException e) {
            log.error("Erreur lors de la sauvegarde du fichier {} : {}", originalFilename, e.getMessage());
            throw new RuntimeException("Erreur lors de la sauvegarde du fichier : " + originalFilename, e);
        }
    }

    /**
     * Charge un fichier depuis le stockage et retourne son chemin.
     *
     * @param filename le nom du fichier stocké
     * @return le chemin du fichier
     */
    public Path loadFile(String filename) {
        Path file = rootLocation.resolve(filename).normalize();
        if (!Files.exists(file)) {
            throw new RuntimeException("Fichier non trouvé : " + filename);
        }
        return file;
    }

    /**
     * Supprime un fichier du stockage.
     *
     * @param filename le nom du fichier à supprimer
     */
    public void deleteFile(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier {} : {}", filename, e.getMessage());
        }
    }
}
