package com.example.ClipAI.service;

import com.example.ClipAI.model.audio.TimedWord;
import com.example.ClipAI.model.video.ImageTiming;
import com.example.ClipAI.service.audio.AudioService;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VideoCreator {
    // Video constants
    private static final int VIDEO_WIDTH = 1080;
    private static final int VIDEO_HEIGHT = 1920;
    private static final int FPS = 30;
    private static final double TRANSITION_DURATION = 0.5; // seconds
    private static final double PRE_KEYWORD_TRANSITION = 0.2; // seconds before keyword
    private final Logger logger = LoggerFactory.getLogger(VideoCreator.class);
    private static final int COLOR_DEPTH = BufferedImage.TYPE_3BYTE_BGR;

    @Autowired
    private AudioService audioService;

    // Main processing methods
    public void createVideo(String audioPath, String imagesDir, String outputPath) {
        try {
            logger.debug("Starting video creation process...");

            // 1. Analyze audio and get word timings
            List<TimedWord> timedWords = audioService.transcribeAudio(audioPath).getSegments();
            System.out.println("Audio analysis complete. Found " + timedWords.size() + " words");

            // 2. Get and sort image files
            File[] imageFiles = getImageFiles(imagesDir);
            System.out.println("Found " + imageFiles.length + " images");

            // 3. Match images to keywords
            List<ImageTiming> imageTiming = matchImagesToKeywords(timedWords, imageFiles);
            System.out.println("Matched " + imageTiming.size() + " images to keywords");

            // 4. Generate video
            generateVideo(audioPath, imageTiming, timedWords, outputPath);

            System.out.println("Video creation complete!");

        } catch (Exception e) {
            System.err.println("Error creating video: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Path convertToWav(String audioPath) throws Exception {
        Path tempWavPath = Paths.get("temp_audio.wav");

        // Use FFmpeg to convert audio to WAV format suitable for Vosk
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(audioPath);
        grabber.start();

        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(tempWavPath.toString(), 1);
        recorder.setAudioChannels(1); // Mono
        recorder.setSampleRate(16000); // 16kHz
        recorder.setAudioCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PCM_S16LE);
        recorder.setFormat("wav");
        recorder.start();

        Frame frame;
        while ((frame = grabber.grab()) != null) {
            if (frame.samples != null) {
                recorder.record(frame);
            }
        }

        recorder.close();
        grabber.close();

        return tempWavPath;
    }

    private List<ImageTiming> matchImagesToKeywords(List<TimedWord> timedWords, File[] imageFiles) {
        List<ImageTiming> imageTiming = new ArrayList<>();
        Map<String, String> keywordToImage = new HashMap<>();

        // Create mapping of keywords to image files
        for (File imageFile : imageFiles) {
            String fileName = imageFile.getName().toLowerCase();
            String keyword = fileName.substring(0, fileName.lastIndexOf('.')).replace('_', ' ').toLowerCase();
            keywordToImage.put(keyword, imageFile.getPath());
        }

        int scriptLength = timedWords.size();
        // Match keywords to timed words
        for (int i =0; i<scriptLength; ++i) {
            String word = timedWords.get(i).getWord().toLowerCase();
            for (String keyword : keywordToImage.keySet()) {
                if (keyword.contains(word)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(timedWords.get(i).getWord());
                    int count = 0, index =i;
                    while (index+1 < scriptLength && count<8 && !sb.toString().equals(keyword)){
                        sb.append(" ").append(timedWords.get(++index).getWord());
                        count++;
                    }
                    if(count==8){
                        continue;
                    }
                    double showTime = timedWords.get(i).getStartTime() - PRE_KEYWORD_TRANSITION;
                    if(imageTiming.isEmpty()){
                        showTime=0;
                    }
                    imageTiming.add(new ImageTiming(
                            keywordToImage.get(keyword),
                            showTime,
                            keyword
                    ));
                    i+=count;
                    break;
                }
            }
        }

        imageTiming.sort((a, b) -> Double.compare(a.getShowTime(), b.getShowTime()));
        return imageTiming;
    }

    private void generateVideo(String audioPath, List<ImageTiming> imageTiming,
            List<TimedWord> timedWords, String outputPath) throws Exception {
        System.out.println("Generating video: " + outputPath);

        // Set up video recorder
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputPath, VIDEO_WIDTH, VIDEO_HEIGHT);
        configureRecorder(recorder);
        recorder.start();

        // Load font
        Font bangerFont = loadCustomFont();

        // Set up frame converter
        Java2DFrameConverter converter = new Java2DFrameConverter();

        // Process audio to get duration
        FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(audioPath);
        audioGrabber.start();
        double duration = audioGrabber.getLengthInTime() / 1000000.0;
        audioGrabber.close();

        // Generate video frames
        generateVideoFrames(recorder, converter, bangerFont, imageTiming, timedWords, duration);

        // Add audio
        addAudioToVideo(recorder, audioPath);

        // Cleanup
        recorder.stop();
        recorder.release();
    }

    private void configureRecorder(FFmpegFrameRecorder recorder) {
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");
        recorder.setFrameRate(FPS);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

        // Quality settings
        recorder.setVideoQuality(0);
        recorder.setVideoBitrate(8000000);
        recorder.setVideoOption("crf", "18");
        recorder.setVideoOption("preset", "slow");
        recorder.setVideoOption("profile", "high");
        recorder.setVideoOption("level", "4.2");

        // Color-related settings
        recorder.setVideoOption("colorspace", "bt709");
        recorder.setVideoOption("color_primaries", "bt709");
        recorder.setVideoOption("color_trc", "bt709");
        recorder.setVideoOption("colorrange", "tv");

        // Additional quality settings
        recorder.setVideoOption("x264opts", "no-fast-pskip:no-dct-decimate");
        recorder.setVideoOption("bf", "2");
        recorder.setVideoOption("refs", "4");

        // Audio settings
        recorder.setAudioChannels(2);
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        recorder.setSampleRate(44100);
        recorder.setAudioBitrate(320000);
    }

    private File[] getImageFiles(String imagesDir) {
        File dir = new File(imagesDir);
        return dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
    }
    private Font loadCustomFont() {
        try {
            return Font.createFont(Font.TRUETYPE_FONT,
                    new File("src/main/resources/fonts/Bangers-Regular.ttf")).deriveFont(60f);
        } catch (Exception e) {
            System.out.println("Error loading custom font, using default");
            return new Font("Arial", Font.BOLD, 60);
        }
    }

    private void generateVideoFrames(FFmpegFrameRecorder recorder,
            Java2DFrameConverter converter,
            Font font,
            List<ImageTiming> imageTiming,
            List<TimedWord> timedWords,
            double duration) throws Exception {
        int totalFrames = (int) (duration * FPS);
        BufferedImage currentImage = null;
        BufferedImage nextImage = null;
        int currentImageIndex = -1;  // Start with -1 to handle first image properly

        // Cache for loaded images to avoid reloading the same image multiple times
        Map<String, BufferedImage> imageCache = new HashMap<>();

        for (int frameNumber = 0; frameNumber < totalFrames; frameNumber++) {
            try {
                double currentTime = frameNumber / (double) FPS;
                boolean needNewImage = false;

                // Check if we need to show next image
                if (currentImageIndex + 1 < imageTiming.size() && currentTime >= imageTiming.get(
                        currentImageIndex + 1).getShowTime()) {
                    currentImageIndex++;
                    needNewImage = true;
                }

                // Load images only when needed
                if (needNewImage) {
                    // Load current image
                    String currentPath = imageTiming.get(currentImageIndex).getImagePath();
                    currentImage = imageCache.computeIfAbsent(currentPath,
                            imagePath1 -> {
                                try {
                                    return loadAndResizeImage(imagePath1);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                    // Preload next image if available
                    if (currentImageIndex + 1 < imageTiming.size()) {
                        String nextPath = imageTiming.get(currentImageIndex + 1).getImagePath();
                        nextImage = imageCache.computeIfAbsent(nextPath,
                                imagePath -> {
                                    try {
                                        return loadAndResizeImage(imagePath);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                    } else {
                        nextImage = null;
                    }
                }

                // Create frame with transition if needed
                BufferedImage frame;
                if (currentImage == null) {
                    // Create black frame for start of video
                    frame = new BufferedImage(VIDEO_WIDTH, VIDEO_HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = frame.createGraphics();
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT);
                    g.dispose();
                } else {
                    double transitionProgress = 0.0;
                    if (nextImage != null && currentImageIndex < imageTiming.size() - 1) {
                        double nextShowTime = imageTiming.get(currentImageIndex + 1).getShowTime();
                        double transitionStart = nextShowTime - TRANSITION_DURATION;

                        if (currentTime >= transitionStart) {
                            transitionProgress = Math.min(1.0, Math.max(0.0,
                                    (currentTime - transitionStart) / TRANSITION_DURATION));
                        }
                    }

                    frame = createFrame(currentImage, nextImage, transitionProgress);
                }

                // Add subtitle
                String subtitle = getCurrentSubtitle(timedWords, currentTime);
                if (subtitle != null) {
                    addSubtitle(frame, subtitle, font);
                }

                // Record frame
                recorder.record(converter.convert(frame));

                // Progress indication
                if (frameNumber % FPS == 0) {
                    System.out.printf("Progress: %.1f%%\n", (frameNumber * 100.0) / totalFrames);
                    System.out.printf("Current time: %.2fs, Image: %s\n", currentTime,
                            currentImageIndex >= 0 ? imageTiming.get(currentImageIndex).getKeyword() : "none");
                }
            } catch (Exception e) {
                logger.error("error in generating frames",e);
                throw new RuntimeException(e);
            }
        }

        // Clear image cache
        imageCache.clear();
    }

    // Updated createFrame method to simplify transition logic
//    private BufferedImage createFrame(BufferedImage currentImage,
//            BufferedImage nextImage,
//            double transitionProgress) {
//        BufferedImage frame = copyImage(currentImage);
//
//        if (nextImage != null && transitionProgress > 0) {
//            // Ensure alpha value is between 0.0 and 1.0
//            float alpha = (float) Math.min(1.0, Math.max(0.0, transitionProgress));
//            Graphics2D g = frame.createGraphics();
//            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
//            g.drawImage(nextImage, 0, 0, null);
//            g.dispose();
//        }
//
//        return frame;
//    }

    private BufferedImage createFrame(BufferedImage currentImage,
            BufferedImage nextImage,
            double transitionProgress) {
        BufferedImage frame = new BufferedImage(VIDEO_WIDTH, VIDEO_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = frame.createGraphics();

        configureGraphicsQuality(g);

        // Draw current image
        g.drawImage(currentImage, 0, 0, null);

        // Apply transition if needed
        if (nextImage != null && transitionProgress > 0) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    (float) Math.min(1.0, Math.max(0.0, transitionProgress))));
            g.drawImage(nextImage, 0, 0, null);
        }

        g.dispose();
        return frame;
    }

    // Helper method to copy an image
    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(),
                source.getHeight(),
                source.getType());
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

    private BufferedImage loadAndResizeImage(String imagePath) throws IOException {
        try {
            // Read image with explicit color space
            BufferedImage original = ImageIO.read(new File(imagePath));
            if (original == null) {
                throw new IOException("Failed to load image: " + imagePath);
            }

            // Convert to correct color space immediately
            BufferedImage convertedImg = new BufferedImage(
                    original.getWidth(),
                    original.getHeight(),
                    COLOR_DEPTH
            );
            Graphics2D g = convertedImg.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g.drawImage(original, 0, 0, null);
            g.dispose();

            return resizeImageHighQuality(convertedImg);
        } catch (Exception e) {
            throw new IOException("Error processing image: " + imagePath, e);
        }
    }

    private BufferedImage resizeImageHighQuality(BufferedImage original) {
        double scaleWidth = (double) VIDEO_WIDTH / original.getWidth();
        double scaleHeight = (double) VIDEO_HEIGHT / original.getHeight();
        double scale = Math.max(scaleWidth, scaleHeight);

        int scaledWidth = (int) (original.getWidth() * scale);
        int scaledHeight = (int) (original.getHeight() * scale);

        // Use correct color space for scaled image
        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, COLOR_DEPTH);
        Graphics2D g2d = scaledImage.createGraphics();

        configureGraphicsQuality(g2d);
        g2d.drawImage(original, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        // Center crop with correct color space
        int x = (scaledWidth - VIDEO_WIDTH) / 2;
        int y = (scaledHeight - VIDEO_HEIGHT) / 2;

        BufferedImage croppedImage = new BufferedImage(VIDEO_WIDTH, VIDEO_HEIGHT, COLOR_DEPTH);
        Graphics2D g = croppedImage.createGraphics();
        configureGraphicsQuality(g);

        g.drawImage(scaledImage,
                0, 0, VIDEO_WIDTH, VIDEO_HEIGHT,
                x, y, x + VIDEO_WIDTH, y + VIDEO_HEIGHT,
                null);
        g.dispose();

        return croppedImage;
    }

    private void configureGraphicsQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    }

//    private BufferedImage resizeImage(BufferedImage original) {
//        // Calculate the scale ratio to cover the entire video dimensions
//        double scaleWidth = (double) VIDEO_WIDTH / original.getWidth();
//        double scaleHeight = (double) VIDEO_HEIGHT / original.getHeight();
//
//        // Use the larger scale to ensure image covers the entire area
//        double scale = Math.max(scaleWidth, scaleHeight);
//
//        // Calculate dimensions after scaling
//        int scaledWidth = (int) (original.getWidth() * scale);
//        int scaledHeight = (int) (original.getHeight() * scale);
//
//        // Create a temporary image for the scaled version
//        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
//        Graphics2D g2d = scaledImage.createGraphics();
//        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//        g2d.drawImage(original, 0, 0, scaledWidth, scaledHeight, null);
//        g2d.dispose();
//
//        // Calculate cropping coordinates to center the image
//        int x = (scaledWidth - VIDEO_WIDTH) / 2;
//        int y = (scaledHeight - VIDEO_HEIGHT) / 2;
//
//        // Create the final cropped image
//        BufferedImage croppedImage = new BufferedImage(VIDEO_WIDTH, VIDEO_HEIGHT, BufferedImage.TYPE_INT_RGB);
//        Graphics2D g = croppedImage.createGraphics();
//
//        // Draw the cropped portion of the scaled image
//        g.drawImage(scaledImage,
//                0, 0, VIDEO_WIDTH, VIDEO_HEIGHT,  // Destination coordinates
//                x, y, x + VIDEO_WIDTH, y + VIDEO_HEIGHT,  // Source coordinates
//                null);
//        g.dispose();
//
//        return croppedImage;
//    }

    private BufferedImage createFrame(BufferedImage currentImage,
            BufferedImage nextImage,
            double currentTime,
            int currentImageIndex,
            List<ImageTiming> imageTiming) {
        if (currentImage == null) {
            BufferedImage black = new BufferedImage(VIDEO_WIDTH, VIDEO_HEIGHT,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = black.createGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT);
            g.dispose();
            return black;
        }

        BufferedImage frame = copyImage(currentImage);

        // Apply transition if needed
        if (nextImage != null && currentImageIndex < imageTiming.size()) {
            double transitionStart = imageTiming.get(currentImageIndex).getShowTime() - TRANSITION_DURATION;
            if (currentTime >= transitionStart) {
                double transitionProgress = (currentTime - transitionStart) / TRANSITION_DURATION;
                applyTransition(frame, nextImage, transitionProgress);
            }
        }

        return frame;
    }

    private void applyTransition(BufferedImage current,
            BufferedImage next,
            double progress) {
        Graphics2D g = current.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                (float)(1.0 - progress)));
        g.drawImage(next, 0, 0, null);
        g.dispose();
    }

    private void addSubtitle(BufferedImage image, String text, Font font) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);

        // Calculate text position
        FontMetrics metrics = g.getFontMetrics(font);
        int textWidth = metrics.stringWidth(text);
        int x = (VIDEO_WIDTH - textWidth) / 2;
        int y = VIDEO_HEIGHT - 200;

        // Draw text outline
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(3f));
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i != 0 || j != 0) {
                    g.drawString(text, x + i, y + j);
                }
            }
        }

        // Draw text
        g.setColor(Color.WHITE);
        g.drawString(text, x, y);
        g.dispose();
    }

    private String getCurrentSubtitle(List<TimedWord> timedWords, double currentTime) {
        for (TimedWord word : timedWords) {
            if (currentTime >= word.getStartTime() && currentTime <= word.getEndTime()) {
                return word.getWord();
            }
        }
        return null;
    }

    private void addAudioToVideo(FFmpegFrameRecorder recorder, String audioPath)
            throws Exception {
        FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(audioPath);
        audioGrabber.start();
        Frame audioFrame;
        while ((audioFrame = audioGrabber.grab()) != null) {
            if (audioFrame.samples != null) {
                recorder.record(audioFrame);
            }
        }
        audioGrabber.close();
    }


        // Example usage
//        String audioPath = "narration.mp3";
//        String imagesDir = "images";
//        String outputPath = "final_video.mp4";

}
