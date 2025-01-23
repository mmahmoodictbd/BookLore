package com.adityachandel.booklore.service.fileprocessor;

import com.adityachandel.booklore.config.AppProperties;
import com.adityachandel.booklore.model.entity.BookMetadataEntity;
import com.adityachandel.booklore.repository.BookRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

@AllArgsConstructor
@Service
@Slf4j
public class FileProcessingUtils {

    private final AppProperties appProperties;

    public void setBookCoverPath(long bookId, BookMetadataEntity bookMetadataEntity) {
        bookMetadataEntity.setThumbnail(appProperties.getPathConfig() + "/thumbs/" + bookId + "/f.jpg");
        bookMetadataEntity.setCoverUpdatedOn(Instant.now());
    }

    public boolean saveCoverImage(BufferedImage coverImage, long bookId) throws IOException {
        File coverDirectory = new File(appProperties.getPathConfig() + "/thumbs");
        BufferedImage resizedImage = resizeImage(coverImage, 250, 350);
        File bookDirectory = new File(coverDirectory, String.valueOf(bookId));
        if (!bookDirectory.exists()) {
            if (!bookDirectory.mkdirs()) {
                throw new IOException("Failed to create directory: " + bookDirectory.getAbsolutePath());
            }
        }
        String coverImageName = "f.jpg";
        File coverImageFile = new File(bookDirectory, coverImageName);
        return ImageIO.write(resizedImage, "JPEG", coverImageFile);
    }

    public BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        Image tmp = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resizedImage;
    }

    public void deleteBookCovers(Set<Long> bookIds) {
        for (Long bookId : bookIds) {
            String bookCoverFolder = appProperties.getPathConfig() + "/thumbs/" + bookId;
            Path folderPath = Paths.get(bookCoverFolder);
            try {
                if (Files.exists(folderPath) && Files.isDirectory(folderPath)) {
                    try (Stream<Path> walk = Files.walk(folderPath)) {
                        walk.sorted(Comparator.reverseOrder())
                                .forEach(path -> {
                                    try {
                                        Files.delete(path);
                                    } catch (IOException e) {
                                        log.error("Failed to delete file: {} - {}", path, e.getMessage());
                                    }
                                });
                    }
                }
            } catch (IOException e) {
                log.error("Error processing folder: {} - {}", folderPath, e.getMessage());
            }
        }
        log.info("Deleted {} book covers", bookIds.size());
    }
}
