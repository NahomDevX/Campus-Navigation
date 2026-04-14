package com.example.campusnavigation.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {
    private static final ExecutorService IO = Executors.newFixedThreadPool(3);

    private AppExecutors() {
    }

    public static ExecutorService io() {
        return IO;
    }
}
