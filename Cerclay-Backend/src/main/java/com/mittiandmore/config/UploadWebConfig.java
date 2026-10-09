package com.mittiandmore.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadWebConfig implements WebMvcConfigurer {

    private final String directory;
    private final String returnDirectory;

    public UploadWebConfig(
        @Value("${app.uploads.product-dir:./uploads/products}") String directory,
        @Value("${app.uploads.return-dir:./uploads/returns}") String returnDirectory
    ) {
        this.directory = directory;
        this.returnDirectory = returnDirectory;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        List<String> locations = new ArrayList<>();
        addLocation(locations, Paths.get(directory));

        // Keep existing locally-uploaded images visible when the backend is
        // moved/extracted into a new project directory during development.
        Path working = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        addLocation(locations, working.resolve("uploads/products"));
        addLocation(locations, working.resolve("../uploads/products"));
        addLocation(locations, working.resolve("../../uploads/products"));
        addLocation(locations, working.resolve("../Cerclay-Backend/uploads/products"));
        addLocation(locations, working.resolve("../../Cerclay-Backend/uploads/products"));

        Path parent = working.getParent();
        if (parent != null) {
            try (var stream = Files.list(parent)) {
                stream
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().toLowerCase().contains("backend"))
                    .forEach(p -> addLocation(locations, p.resolve("uploads/products")));
            } catch (Exception ignored) {
                // The primary configured location remains authoritative.
            }
        }

        registry.addResourceHandler("/uploads/products/**").addResourceLocations(locations.toArray(String[]::new));

        Path returnPath = Paths.get(returnDirectory).toAbsolutePath().normalize();
        addLocation(new ArrayList<>(), returnPath);
        String returnLocation = returnPath.toUri().toString();
        if (!returnLocation.endsWith("/")) returnLocation += "/";
        registry.addResourceHandler("/uploads/returns/**").addResourceLocations(returnLocation);
    }

    private void addLocation(List<String> locations, Path path) {
        try {
            Path normalized = path.toAbsolutePath().normalize();
            Files.createDirectories(normalized);
            String location = normalized.toUri().toString();
            if (!location.endsWith("/")) location += "/";
            if (!locations.contains(location)) locations.add(location);
        } catch (Exception ignored) {
            // Ignore an unavailable fallback directory.
        }
    }
}
