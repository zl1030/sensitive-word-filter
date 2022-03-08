package com.nirvana.service;

import com.nirvana.service.SensitiveWordUtil.SensitiveWord;
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
            System.out.println("WORD_PATH:" + path);
            Set<SensitiveWord> words = Files.lines(Paths.get(path), StandardCharsets.UTF_8)
                .distinct()
                .parallel()
                .map(word -> new SensitiveWord((byte) 1, word.toLowerCase())).collect(Collectors.toSet());
            SensitiveWordUtil.init(words);
            System.out.println("init words num:" + words.size());
            Quarkus.waitForExit();
            return 0;
        }
    }
}
