package com.wxw.cloud.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author twx
 * @create:   2026-5-16
 */
public interface UploadService {

    String uploadImage(MultipartFile file);
}
