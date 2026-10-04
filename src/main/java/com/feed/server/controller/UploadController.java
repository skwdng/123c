package com.feed.server.controller;

import com.feed.common.exception.BaseException;
import com.feed.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传：本地磁盘存储，通过 /uploads/** 静态映射对外访问
 * （学习项目用本地存储即可，生产环境建议换 OSS）
 */
@RestController
@RequestMapping("/upload")
@Slf4j
public class UploadController {

    // 允许上传的视频类型
    private static final Set<String> VIDEO_TYPES = Set.of("mp4", "webm", "mov", "m4v");
    // 允许上传的图片类型
    private static final Set<String> IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "webp", "gif");
    // 单文件最大 200MB（同时受 application.yml multipart 配置限制）
    private static final long MAX_SIZE = 200L * 1024 * 1024;

    // 存储目录：项目运行目录下的 uploads/ 文件夹
    private static final Path UPLOAD_DIR = Paths.get(System.getProperty("user.dir"), "uploads");

    /**
     * 上传文件（视频或图片），返回可访问的 URL
     *
     * @param file 文件
     * @return 访问路径，如 /uploads/xxx.mp4
     */
    @PostMapping
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BaseException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BaseException("文件不能超过 200MB");
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (!VIDEO_TYPES.contains(ext) && !IMAGE_TYPES.contains(ext)) {
            throw new BaseException("只支持上传视频(mp4/webm/mov)或图片(jpg/png/webp)");
        }

        // 文件名用 UUID 防冲突和路径穿越，保留扩展名
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Files.createDirectories(UPLOAD_DIR);
        Path target = UPLOAD_DIR.resolve(filename);
        file.transferTo(target.toFile());

        String url = "/uploads/" + filename;
        log.info("文件上传成功：original={}, saved={}, size={}KB", original, filename, file.getSize() / 1024);

        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        data.put("type", VIDEO_TYPES.contains(ext) ? "video" : "image");
        return Result.success(data);
    }
}
