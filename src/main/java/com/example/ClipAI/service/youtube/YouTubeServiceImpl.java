package com.example.ClipAI.service.youtube;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
@Slf4j
@RequiredArgsConstructor
public class YouTubeServiceImpl {

    private  final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);


}
