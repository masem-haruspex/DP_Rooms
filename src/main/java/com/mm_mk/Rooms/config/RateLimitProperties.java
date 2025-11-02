package com.mm_mk.Rooms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@Getter
@Setter
@Component
public class RateLimitProperties {

    private boolean enabled = true;
    private DefaultLimits defaults = new DefaultLimits();
    private Map<String, EndpointLimit> endpoints = new HashMap<>();

    @Getter
    @Setter
    public static class DefaultLimits {
        private int requests = 50;
        private Duration window = Duration.ofMinutes(1);
    }

    @Getter
    @Setter
    public static class EndpointLimit {
        private int requests = 50;
        private Duration window = Duration.ofMinutes(1);
        private boolean enabled = true;
    }

    public RateLimitProperties() {
        defaults.setRequests(50);
        defaults.setWindow(Duration.ofMinutes(1));


        EndpointLimit listRooms = new EndpointLimit();
        listRooms.setRequests(10);
        listRooms.setWindow(Duration.ofMinutes(1));
        endpoints.put("/api/rooms", listRooms);           // POST /api/rooms

        // Guest endpoints (join/leave/room info/participants) — stricter for join
        EndpointLimit join = new EndpointLimit();
        join.setRequests(10);
        join.setWindow(Duration.ofMinutes(1));
        endpoints.put("/api/rooms/*/join", join);

        EndpointLimit leave = new EndpointLimit();
        leave.setRequests(10);
        leave.setWindow(Duration.ofMinutes(1));
        endpoints.put("/api/rooms/*/leave", leave);

        EndpointLimit roomInfo = new EndpointLimit();
        roomInfo.setRequests(20);
        roomInfo.setWindow(Duration.ofMinutes(1));
        endpoints.put("/api/rooms/*", roomInfo);          // will match /api/rooms/{code} and similar prefixes

        EndpointLimit participants = new EndpointLimit();
        participants.setRequests(20);
        participants.setWindow(Duration.ofMinutes(1));
        endpoints.put("/api/rooms/*/participants", participants);

    }
}

