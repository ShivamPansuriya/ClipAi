package com.example.ClipAI.service.youtube;

import com.example.ClipAI.model.youtube.VideoRequest;

public interface VideoUploadService {
    VideoRequest uploadVideoWithDelay(long userId, VideoRequest request);
    VideoRequest uploadVideo(long userId, VideoRequest request);
}
