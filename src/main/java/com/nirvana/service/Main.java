package com.nirvana.service;

import com.google.common.util.concurrent.RateLimiter;
import com.nirvana.service.SensitiveWordUtil.SensitiveWord;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

@QuarkusMain
public class Main {

    public static void main(String[] args) {
        Quarkus.run(Service.class, args);
    }

    public static class Service implements QuarkusApplication {

        @Override
        public int run(String... args) throws Exception {

            String path = System.getenv("WORD_PATH");
            Log.info("WORD_PATH:" + path);
            Set<SensitiveWord> words = Files.lines(Paths.get(path), StandardCharsets.UTF_8)
                .distinct()
                .parallel()
                .map(word -> new SensitiveWord((byte) 1, word.toLowerCase())).collect(Collectors.toSet());
            SensitiveWordUtil.init(words);
            Log.info("init words num:" + words.size());

            int permitsPerSecond = 200;
            SensitiveWordFilterService.rateLimiter = RateLimiter.create(permitsPerSecond);

            Quarkus.waitForExit();
            return 0;
        }
    }
}
