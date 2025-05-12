package com.example.ClipAI.service;

import com.example.ClipAI.config.AppConfig;
import com.example.ClipAI.model.ClipAIRest;
import com.example.ClipAI.model.audio.TimedWord;
import com.example.ClipAI.model.video.ImageTiming;
import com.example.ClipAI.service.audio.AudioService;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for creating videos from images and audio.
 */
@Service
public class VideoCreatorRefactored {
    // Video constants
    private static final int VIDEO_WIDTH = 1080;
    private static final int VIDEO_HEIGHT = 1920;
    private static final int FPS = 30;
    private static final double TRANSITION_DURATION = 0.5; // seconds
    private static final double PRE_KEYWORD_TRANSITION = 0.2; // seconds before keyword
    private static final int COLOR_DEPTH = BufferedImage.TYPE_3BYTE_BGR;

    private final Logger logger = LoggerFactory.getLogger(VideoCreatorRefactored.class);

    @Autowired
    private AudioService audioService;

    @Autowired
    private AppConfig appConfig;



    /**
     * Creates a video from audio and images.
     *
     * @param audioPath The path to the audio file
     * @param imagesDir The directory containing the images
     * @param outputPath The path where the output video should be saved
     * @param clipAIRest The ClipAIRest object containing the script
     */
    public void createVideo(String audioPath, String imagesDir, String outputPath, ClipAIRest clipAIRest) {
        try {
            logger.info("Starting video creation process...");

            // 1. Analyze audio and get word timings
            List<TimedWord> timedWords = audioService.transcribeAudio(audioPath, clipAIRest).getSegments();
            logger.info("Audio analysis complete. Found {} words", timedWords.size());

            // 2. Get and sort image files
            File[] imageFiles = getImageFiles(imagesDir);
            logger.info("Found {} images", imageFiles.length);

            // 3. Match images to keywords
            List<ImageTiming> imageTiming = matchImagesToKeywords(timedWords, imageFiles);
            logger.info("Matched {} images to keywords", imageTiming.size());

            // 4. Generate video
            generateVideo(audioPath, imageTiming, timedWords, outputPath);

            addBackground();
            logger.info("Video creation complete!");

        } catch (Exception e) {
            logger.error("Error creating video: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets image files from the specified directory.
     *
     * @param imagesDir The directory containing the images
     * @return An array of image files
     */
    private File[] getImageFiles(String imagesDir) {
        File dir = new File(imagesDir);
        return dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
    }

    /**
     * Matches images to keywords in the script.
     *
     * @param timedWords The timed words from the script
     * @param imageFiles The image files
     * @return A list of image timings
     */
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
        for (int i = 0; i < scriptLength; ++i) {
            String word = timedWords.get(i).getWord().toLowerCase();
            for (String keyword : keywordToImage.keySet()) {
                if (keyword.contains(word)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(timedWords.get(i).getWord());
                    int count = 0, index = i;
                    while (index + 1 < scriptLength && count < 8 && !sb.toString().equals(keyword)) {
                        sb.append(" ").append(timedWords.get(++index).getWord());
                        count++;
                    }
                    if (count == 8) {
                        continue;
                    }
                    double showTime = timedWords.get(i).getStartTime() - PRE_KEYWORD_TRANSITION;
                    if (imageTiming.isEmpty()) {
                        showTime = 0;
                    }
                    imageTiming.add(new ImageTiming(
                            keywordToImage.get(keyword),
                            showTime,
                            keyword
                    ));
                    i += count;
                    break;
                }
            }
        }

        imageTiming.sort((a, b) -> Double.compare(a.getShowTime(), b.getShowTime()));
        return imageTiming;
    }

    /**
     * Generates a video from audio and images.
     *
     * @param audioPath The path to the audio file
     * @param imageTiming The image timings
     * @param timedWords The timed words from the script
     * @param outputPath The path where the output video should be saved
     * @throws Exception If an error occurs
     */
    private void generateVideo(String audioPath, List<ImageTiming> imageTiming,
            List<TimedWord> timedWords, String outputPath) throws Exception {
        logger.info("Generating video: {}", outputPath);

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

    /**
     * Configures the video recorder.
     *
     * @param recorder The recorder to configure
     */
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

    /**
     * Loads a custom font.
     *
     * @return The loaded font
     */
    private Font loadCustomFont() {
        try {
            return Font.createFont(Font.TRUETYPE_FONT,
                    new File("src/main/resources/fonts/Bangers-Regular.ttf")).deriveFont(60f);
        } catch (Exception e) {
            logger.warn("Error loading custom font, using default", e);
            return new Font("Arial", Font.BOLD, 60);
        }
    }

    /**
     * Generates video frames.
     *
     * @param recorder The recorder to use
     * @param converter The converter to use
     * @param font The font to use for subtitles
     * @param imageTiming The image timings
     * @param timedWords The timed words from the script
     * @param duration The duration of the video
     * @throws Exception If an error occurs
     */
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
                    logger.info("Progress: {:.1f}%, Current time: {:.2f}s, Image: {}",
                            (frameNumber * 100.0) / totalFrames,
                            currentTime,
                            currentImageIndex >= 0 ? imageTiming.get(currentImageIndex).getKeyword() : "none");
                }
            } catch (Exception e) {
                logger.error("Error in generating frames", e);
                throw new RuntimeException(e);
            }
        }

        // Clear image cache
        imageCache.clear();
    }

    /**
     * Creates a frame with transition.
     *
     * @param currentImage The current image
     * @param nextImage The next image
     * @param transitionProgress The transition progress
     * @return The created frame
     */
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

    /**
     * Configures graphics quality.
     *
     * @param g The graphics to configure
     */
    private void configureGraphicsQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    }

    /**
     * Loads and resizes an image.
     *
     * @param imagePath The path to the image
     * @return The loaded and resized image
     * @throws IOException If an error occurs
     */
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

    /**
     * Resizes an image with high quality.
     *
     * @param original The original image
     * @return The resized image
     */
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

    /**
     * Gets the current subtitle.
     *
     * @param timedWords The timed words
     * @param currentTime The current time
     * @return The current subtitle
     */
    private String getCurrentSubtitle(List<TimedWord> timedWords, double currentTime) {
        for (TimedWord word : timedWords) {
            if (currentTime >= word.getStartTime() && currentTime <= word.getEndTime()) {
                return word.getWord();
            }
        }
        return null;
    }

    /**
     * Adds a subtitle to a frame.
     *
     * @param frame The frame
     * @param subtitle The subtitle
     * @param font The font
     */
    private void addSubtitle(BufferedImage frame, String subtitle, Font font) {
        Graphics2D g = frame.createGraphics();
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Calculate text position
        FontMetrics metrics = g.getFontMetrics(font);
        int textWidth = metrics.stringWidth(subtitle);
        int x = (VIDEO_WIDTH - textWidth) / 2;
        int y = VIDEO_HEIGHT - 100;

        // Draw text shadow
        g.setColor(Color.BLACK);
        g.drawString(subtitle, x + 2, y + 2);

        // Draw text
        g.setColor(Color.WHITE);
        g.drawString(subtitle, x, y);

        g.dispose();
    }

    /**
     * Adds audio to a video.
     *
     * @param recorder The recorder
     * @param audioPath The path to the audio file
     * @throws Exception If an error occurs
     */
    private void addAudioToVideo(FFmpegFrameRecorder recorder, String audioPath) throws Exception {
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

    /**
     * Adds a background to the video.
     */
    private void addBackground() {
        try {
            String inputFilePath = appConfig.getVideoOutputPath();
            String outputFilePath = appConfig.getOutputDirectory() + "/output_video.mp4";
            String backgroundAudioPath = appConfig.getOutputDirectory() + "/background.mp3";

            // Check if background audio exists
            File backgroundFile = new File(backgroundAudioPath);
            if (!backgroundFile.exists()) {
                logger.warn("Background audio file not found: {}", backgroundAudioPath);
                return;
            }

            // Open video file
            FFmpegFrameGrabber videoGrabber = new FFmpegFrameGrabber(inputFilePath);
            videoGrabber.start();

            // Get video properties
            int videoWidth = videoGrabber.getImageWidth();
            int videoHeight = videoGrabber.getImageHeight();
            int audioChannels = videoGrabber.getAudioChannels();
            int sampleRate = videoGrabber.getSampleRate();

            // Open background audio file
            FFmpegFrameGrabber bgAudioGrabber = new FFmpegFrameGrabber(backgroundAudioPath);
            bgAudioGrabber.start();

            // Create output recorder
            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFilePath, videoWidth, videoHeight, audioChannels)) {
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setFormat("mp4");
                recorder.setFrameRate(videoGrabber.getFrameRate());
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder.setSampleRate(sampleRate);
                recorder.setAudioBitrate(192000);  // Increased for better audio quality
                recorder.setVideoBitrate(videoGrabber.getVideoBitrate());
                recorder.start();

                Frame videoFrame;
                Frame originalAudioFrame;
                Frame bgAudioFrame;

                // Background audio volume (0.2 = 20% of original volume)
                float bgVolume = 0.08f;

                while ((videoFrame = videoGrabber.grabFrame()) != null) {
                    // Handle video frame
                    if (videoFrame.image != null) {
                        recorder.record(videoFrame);
                    }

                    // Handle audio mixing
                    if (videoFrame.samples != null) {
                        originalAudioFrame = videoFrame;
                        bgAudioFrame = bgAudioGrabber.grabSamples();

                        if (bgAudioFrame != null && bgAudioFrame.samples != null) {
                            // Get the audio buffers
                            ShortBuffer originalAudio = (ShortBuffer) originalAudioFrame.samples[0];
                            ShortBuffer bgAudio = (ShortBuffer) bgAudioFrame.samples[0];

                            // Create a new buffer for mixed audio
                            ShortBuffer mixedAudio = ShortBuffer.allocate(originalAudio.capacity());

                            // Reset buffer positions
                            originalAudio.rewind();
                            bgAudio.rewind();

                            // Mix audio
                            for (int i = 0; i < originalAudio.capacity() && i < bgAudio.capacity(); i++) {
                                // Mix original audio with background audio at reduced volume
                                short originalSample = originalAudio.get(i);
                                short bgSample = (short) (bgAudio.get(i) * bgVolume);

                                // Simple mixing (clipping may occur)
                                short mixedSample = (short) Math.max(Short.MIN_VALUE,
                                        Math.min(Short.MAX_VALUE, originalSample + bgSample));

                                mixedAudio.put(mixedSample);
                            }

                            // Reset buffer position for reading
                            mixedAudio.rewind();

                            // Create a new frame with mixed audio
                            Frame mixedFrame = originalAudioFrame.clone();
                            mixedFrame.samples[0] = mixedAudio;

                            // Record the mixed frame
                            recorder.record(mixedFrame);
                        } else {
                            // If no background audio, just record the original
                            recorder.record(originalAudioFrame);
                        }
                    }
                }
            }

            // Cleanup
            videoGrabber.close();
            bgAudioGrabber.close();

            logger.info("Background audio added successfully");
        } catch (Exception e) {
            logger.error("Error adding background audio: {}", e.getMessage(), e);
        }
    }
}
