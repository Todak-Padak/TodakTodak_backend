package com.padaks.todaktodak.event.controller;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
public class GoogleCalendarProxyController {

    // 이미 인코딩된 URL을 그대로 사용
    String fullUrl = "https://www.googleapis.com/calendar/v3/calendars/ko.south_korea%23holiday@group.v.calendar.google.com/events?key=AIzaSyBLS5qxSTQgJkCjNk42fw9ErYOkCqcShzo";

    private final WebClient webClient = WebClient.create();

    @GetMapping("/api/google-calendar-holidays")
    public Mono<String> fetchHolidays() {
        // URI.create로 이미 인코딩된 URL을 사용하여 이중 인코딩 방지
        return webClient.get()
                .uri(URI.create(fullUrl))  // URI.create로 완전히 인코딩된 URL 전달
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> System.err.println("Error fetching holidays: " + error.getMessage()));
    }
}