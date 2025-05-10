package com.example.ClipAI.service.youtube;

import com.example.ClipAI.model.ClipAIRest;
import com.example.ClipAI.model.youtube.*;
import com.google.api.services.youtube.model.Video;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.FileInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class YoutubeMonitorService
{

    @Autowired
    VideoUploadService videoUploadService;

    private final Path processedVideosFile;

    private Set<String> processedVideos;

    //    private WebDriver driver;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    private final ConcurrentHashMap<Long, YoutubeMonitor> videoMap = new ConcurrentHashMap<>();
    
    public YoutubeMonitorService()
    {

        processedVideosFile = Path.of("processed_videos.txt");
        loadProcessedVideos();
//        initializeWebDriver();
    }

//    private void initializeWebDriver() {
//        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless"); // Run in headless mode
//        options.addArguments("--disable-gpu");
//        options.addArguments("--no-sandbox");
//        options.addArguments("--disable-dev-shm-usage");
//        driver = new ChromeDriver(options);
//    }

    private void loadProcessedVideos()
    {

        processedVideos = new HashSet<>();
        try
        {
            if (Files.exists(processedVideosFile))
            {
                processedVideos.addAll(Files.readAllLines(processedVideosFile));
            }
        }
        catch (Exception e)
        {
            log.error("Error loading processed videos", e);
        }
    }

    private void saveProcessedVideo(String videoId)
    {

        try
        {
            Files.writeString(processedVideosFile, videoId + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            processedVideos.add(videoId);
        }
        catch (Exception e)
        {
            log.error("Error saving processed video", e);
        }
    }

    public void checkAndUploadVideo(YoutubeMonitor youtubeMonitor)
    {

        videoMap.put(youtubeMonitor.getUserId(), youtubeMonitor);
        List<Scheduler> schedulerList = youtubeMonitor.getSchedulers();
        schedulerList.forEach(scheduler ->
                executor.scheduleAtFixedRate(() ->
                        uploadAndDeleteVideo(youtubeMonitor.getUserId()), getInitialTime(scheduler.getStartTime()),
                        scheduler.getPeriodicInterval(), TimeUnit.MINUTES));
    }

    public void addChannelIds(YoutubeMonitor youtubeMonitor)
    {

        videoMap.get(youtubeMonitor.getUserId()).addChannelTags(youtubeMonitor.getChannelTags());
        log.info("added channel tag for {}, having total {} tags", youtubeMonitor.getUserId(), youtubeMonitor.getChannelTags()
                .size());
    }

    private void uploadAndDeleteVideo(long userId)
    {

        String videoName = null;
        try
        {
            YoutubeMonitor youtubeMonitor = videoMap.get(userId);
            checkForNewShorts(youtubeMonitor);
            log.info("check complete ");
            Thread.sleep(3000);
            List<String> fileName = getMP4FilesSortedByCreationTime("shorts/" + youtubeMonitor.getVideoCategory());
            VideoRequest videoRequest = new VideoRequest();
            videoRequest.setPrivacyStatus("public");

            if (!fileName.isEmpty())
            {
                videoName = fileName.get(0);
                String path = "shorts/%s/%s".formatted(youtubeMonitor.getVideoCategory(), videoName);
                videoRequest.setVideoPath(path);
                videoRequest.setUploadDelay(60);
                String title = videoName.substring(0, videoName.length() - 4);
                videoRequest.setDescription(title);
                if (youtubeMonitor.getPlayListUrl() != null && !youtubeMonitor.getPlayListUrl().isEmpty()) {
                    videoRequest.setDescription("This man surprised the judges with his unbelievable magic talent! \n #agt #americagottalent #AmericasGotTalent \n\nGoing for the gold! World-class judges Simon Cowell, Sofia Vergara, Heidi Klum and Howie Mandel and beloved host Terry Crews are back with an all-new season of awe-inspiring talent and jaw-dropping, Golden Buzzer-worthy moments.\n \nFind America's Got Talent trailers, full episode highlights, previews, promos, clips, shorts, and digital exclusives here.");
                }
                if (title.length() < 91)
                {
                    title += " #shorts";
                }
                if (title.length() > 100)
                {
                    title = title.substring(0, 100);
                }
                videoRequest.setTitle(title);
                videoUploadService.uploadVideoWithDelay(youtubeMonitor.getUserId(), videoRequest);
                deleteFile("shorts/%s".formatted(youtubeMonitor.getVideoCategory()), videoName);
            }
        }
        catch (Exception e)
        {
            log.error("Error uploading video {}", videoName, e);
        }
    }

    private void checkForNewShorts(YoutubeMonitor youtubeMonitor)
    {
        List<YTShort> newShorts = null;
        if (youtubeMonitor.getPlayListUrl() != null && !youtubeMonitor.getPlayListUrl().isEmpty()) {
            newShorts = getYtShorts(youtubeMonitor.getMonitorType(), youtubeMonitor.getPlayListUrl());
        }
        for (String channelTag : youtubeMonitor.getChannelTags()) {
            newShorts = getYtShorts(youtubeMonitor.getMonitorType(),
                    "https://www.youtube.com/@%s/shorts".formatted(channelTag));
        }
        if (!CollectionUtils.isEmpty(newShorts)) {
            try {
                for (YTShort short_ : newShorts) {
                    if (!processedVideos.contains(short_.getVideoId())) {
                        downloadVideo(short_, youtubeMonitor);
                        saveProcessedVideo(short_.getVideoId());
                    }
                }
            } catch (Exception e) {
                log.error("Error checking for new shorts", e);
            }
        }
    }

    private List<YTShort> getYtShorts(MonitorEnum monitorEnum, String shortUrl) {
        List<YTShort> newShorts = null;
        try
        {
            newShorts = scrapeShorts(monitorEnum, shortUrl);
        }
        catch (Exception e)
        {
            log.error("Error checking for new shorts", e);
        }
        return newShorts;
    }


    private List<YTShort> scrapeShorts(MonitorEnum monitorType, String shortsUrl)
    {

        List<YTShort> shorts = new ArrayList<>();
        try
        {
            // Use channel shorts URL
            if (MonitorEnum.INSTAGRAM.equals(monitorType))
            {
//                Set<String> videoIds= getInstagramVideoIds(channelTag);
//                if(!CollectionUtils.isEmpty(videoIds)){
//                    videoIds.forEach(videoId -> {
//                        if(!processedVideos.contains(videoId)){
//                            String videoUrl  = "https://www.instagram.com/reel/" + videoId;
//                            YTShort short_ = new YTShort();
//                            short_.setVideoId(videoId);
//                            short_.setUrl(videoUrl);
////                            short_.setTitle(video.attr("title"));
//                            short_.setPublishedAt(LocalDateTime.now()); // We can only get current time since we're scraping
//                            shorts.add(short_);
//                        }
//                    });
//                }
                return shorts;
            }

            // Use a realistic user agent
//            String fullPageContent = getFullPageContent(shortsUrl);
//            if (fullPageContent == null) {
//                return shorts;
//            }
//
//            Document doc = Jsoup.parse(fullPageContent);

            Document doc = Jsoup.connect(shortsUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();

            Elements scripts = doc.select("script");

            for (Element script : scripts)
            {
                String scriptContent = script.html();

                if (scriptContent.contains("shorts/"))
                {
                    String[] parts = scriptContent.split("shorts/");
                    for (int i = 1; i < parts.length; i++)
                    {
                        String videoId = parts[ i ].substring(0, 11); // YouTube video IDs are 11 characters
                        if (!processedVideos.contains(videoId) && isValidVideoId(videoId))
                        {
                            String videoUrl = "https://www.youtube.com/shorts/" + videoId;
                            YTShort short_ = new YTShort();
                            short_.setVideoId(videoId);
                            short_.setUrl(videoUrl);
//                            short_.setTitle(video.attr("title"));
                            short_.setPublishedAt(LocalDateTime.now()); // We can only get current time since we're scraping
                            shorts.add(short_);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("Error scraping shorts {}", shortsUrl, e);
        }
        return shorts;
    }

    private Set<String> getInstagramVideoIds(String channelTag)
    {
        // Command to be executed
        String command = "python instagram.py \"%s\"".formatted(channelTag);
        Set<String> videoId = new HashSet<>();
        try
        {
            // Create a Process to execute the command
            Process process = Runtime.getRuntime().exec(command);

            // Capture the output of the command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            StringBuilder data = new StringBuilder();
            String line;
            System.out.println("Output:");
            while (( line = reader.readLine() ) != null)
            {
                System.out.println(line);
                data.append(line);
            }

            System.out.println("Errors (if any):");
            while (( line = errorReader.readLine() ) != null)
            {
                System.err.println(line);
            }

            // Wait for the process to finish and get the exit code
            int exitCode = process.waitFor();
            System.out.println("Command executed with exit code: " + exitCode);
            videoId = extractReelIds(data.toString());
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
        return videoId;
    }

    public Set<String> extractReelIds(String input)
    {
        // Create a Set to store unique IDs
        Set<String> reelIds = new HashSet<>();  // LinkedHashSet maintains insertion order

        // Define regex pattern to match IDs
        // Matches content between /reel/ and / in the URL
        Pattern pattern = Pattern.compile("/reel/([A-Za-z0-9_-]+)/");
        Matcher matcher = pattern.matcher(input);

        // Find all matches and add to set
        while (matcher.find())
        {
            reelIds.add(matcher.group(1));
        }

        return reelIds;
    }

    private boolean isValidVideoId(String videoId)
    {
        // YouTube video IDs are 11 characters and contain only certain characters
        return videoId.matches("[A-Za-z0-9_-]{11}");
    }

    private void downloadVideo(YTShort short_, YoutubeMonitor youtubeMonitor)
    {

        try
        {
            String downloadDirectory = "shorts/" + youtubeMonitor.getVideoCategory();
            File outputDir = new File(downloadDirectory);
            if (!outputDir.exists())
            {
                outputDir.mkdirs();
            }

            String downloadParameter;
            if (MonitorEnum.YOUTUBE.equals(youtubeMonitor.getMonitorType()))
            {
                downloadParameter = downloadDirectory + "\\%(title)s.%(ext)s";
            }
            else
            {
                downloadParameter = downloadDirectory + "\\%(description)s.%(ext)s";
            }

            // Using yt-dlp for downloading with better options
            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best", // Prefer MP4
                    "-o", downloadParameter, "--embed-thumbnail", "--embed-metadata", "--no-warnings", short_.getUrl());
            System.out.println(short_.getUrl());
            Process process = pb.start();
            InputStream inputStream = process.getInputStream();
            String output = new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .collect(Collectors.joining("\n"));
            System.out.println(output); // Log the output

            int exitCode = process.waitFor();
            System.out.println(exitCode);
            if (exitCode == 0)
            {
                log.info("Successfully downloaded short: {} ({})", short_.getTitle(), short_.getVideoId());
            }
            else
            {
                log.error("Error downloading short: {} ({})", short_.getTitle(), short_.getVideoId());
            }
            process.destroy();
        }
        catch (Exception e)
        {
            log.error("Error downloading video", e);
        }
    }

    private long getInitialTime(int hour)
    {

        LocalTime now = LocalTime.now();
        LocalTime targetTime = LocalTime.of(hour, 0);

        // Calculate the initial delay
        long initialDelay;
        if (now.isBefore(targetTime))
        {
            initialDelay = Duration.between(now, targetTime).getSeconds();
        }
        else
        {
            initialDelay = Duration.between(now, targetTime.plusHours(24)).getSeconds() + 86400L;
        }
        initialDelay /= 60;
        System.out.println(initialDelay);
        return initialDelay;
    }

    public static List<String> getMP4FilesSortedByCreationTime(String directoryPath)
    {

        try
        {
            File directory = new File(directoryPath);
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp4"));

            if (files == null)
            {
                return new ArrayList<>();
            }

            List<MP4FileMetadata> fileMetadataList = new ArrayList<>();

            for (File file : files)
            {
                Path path = file.toPath();
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                fileMetadataList.add(new MP4FileMetadata(file.getName(), attrs.creationTime().toMillis()));
            }

            // Sort by creation time
            fileMetadataList.sort(Collections.reverseOrder());

            // Extract only the file names
            List<String> sortedFileNames = new ArrayList<>();
            for (MP4FileMetadata metadata : fileMetadataList)
            {
                sortedFileNames.add(metadata.fileName);
            }

            return sortedFileNames;

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Deletes a file from the specified directory
     *
     * @param directoryPath Directory containing the file
     * @param fileName      Name of the file to delete
     * @return true if deletion was successful, false otherwise
     */
    public static boolean deleteFile(String directoryPath, String fileName)
    {

        try
        {
            Path filePath = Paths.get(directoryPath, fileName);
            return Files.deleteIfExists(filePath);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private static class MP4FileMetadata implements Comparable<MP4FileMetadata>
    {

        String fileName;

        long creationTime;

        MP4FileMetadata(String fileName, long creationTime)
        {

            this.fileName = fileName;
            this.creationTime = creationTime;
        }

        @Override
        public int compareTo(MP4FileMetadata other)
        {

            return Long.compare(this.creationTime, other.creationTime);
        }

    }

}