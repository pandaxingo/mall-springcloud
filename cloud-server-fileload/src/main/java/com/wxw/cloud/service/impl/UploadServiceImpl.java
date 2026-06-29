package com.wxw.cloud.service.impl;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.wxw.cloud.service.UploadService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class UploadServiceImpl implements UploadService {

    private static final List<String> CONTENT_TYPES = Arrays.asList("image/jpeg", "image/gif", "image/png");

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadServiceImpl.class);

    @Autowired(required = false)
    private FastFileStorageClient storageClient;

    @Value("${app.upload.mode:local}")
    private String uploadMode;

    @Value("${app.upload.local-dir:${user.dir}/upload-images}")
    private String localDir;

    @Value("${app.upload.url-prefix:http://localhost:8082/images/}")
    private String urlPrefix;

    @Override
    public String uploadImage(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        try {
            String contentType = file.getContentType();
            if (!CONTENT_TYPES.contains(contentType)) {
                LOGGER.info("Invalid file type: {}", originalFilename);
                return null;
            }

            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (bufferedImage == null) {
                LOGGER.info("Invalid image content: {}", originalFilename);
                return null;
            }

            String ext = StringUtils.substringAfterLast(originalFilename, ".");
            if ("fastdfs".equalsIgnoreCase(uploadMode)) {
                if (storageClient == null) {
                    LOGGER.info("FastDFS client is not available");
                    return null;
                }
                StorePath storePath = this.storageClient.uploadImageAndCrtThumbImage(
                        file.getInputStream(), file.getSize(), ext, null);
                return "http://image.wxw.com/" + storePath.getFullPath();
            }

            File uploadDir = new File(localDir);
            if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                LOGGER.info("Create upload directory failed: {}", uploadDir.getAbsolutePath());
                return null;
            }

            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(new File(uploadDir, filename));
            return urlPrefix + filename;
        } catch (IOException e) {
            LOGGER.info("Upload image failed: {}", originalFilename, e);
        }
        return null;
    }
}
