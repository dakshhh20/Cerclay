package com.mittiandmore.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class ReturnPhotoStorageService {
    private final Path root;
    public ReturnPhotoStorageService(@Value("${app.uploads.return-dir:${user.home}/.cerclay/uploads/returns}") String directory){
        this.root=Paths.get(directory).toAbsolutePath().normalize();
        try { Files.createDirectories(root); } catch(IOException e){ throw new IllegalStateException("Unable to create return upload directory", e); }
    }
    public StoredPhoto store(Long returnId, MultipartFile file) {
        if(file==null || file.isEmpty()) throw new IllegalArgumentException("Return photo is empty");
        String type=file.getContentType()==null?"":file.getContentType().toLowerCase();
        if(!SetSupport.IMAGE_TYPES.contains(type)) throw new IllegalArgumentException("Only JPG, PNG or WEBP return photos are allowed");
        if(file.getSize()>5L*1024*1024) throw new IllegalArgumentException("Each return photo must be 5 MB or smaller");
        String original=file.getOriginalFilename()==null?"photo":file.getOriginalFilename();
        String ext=extension(original,type);
        String name=UUID.randomUUID()+ext;
        Path dir=root.resolve(String.valueOf(returnId)).normalize();
        if(!dir.startsWith(root)) throw new IllegalArgumentException("Invalid return photo path");
        try { Files.createDirectories(dir); Files.copy(file.getInputStream(),dir.resolve(name),StandardCopyOption.REPLACE_EXISTING); } catch(IOException e){ throw new IllegalStateException("Unable to store return photo",e); }
        return new StoredPhoto(name,original,type);
    }
    private String extension(String name,String type){
        String lower=name.toLowerCase();
        if(lower.endsWith(".png")||"image/png".equals(type)) return ".png";
        if(lower.endsWith(".webp")||"image/webp".equals(type)) return ".webp";
        return ".jpg";
    }
    public record StoredPhoto(String fileName,String originalFileName,String contentType){}
    private static final class SetSupport { static final java.util.Set<String> IMAGE_TYPES=java.util.Set.of("image/jpeg","image/jpg","image/png","image/webp"); }
}
