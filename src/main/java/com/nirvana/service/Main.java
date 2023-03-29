package com.nirvana.service;

import com.google.common.util.concurrent.RateLimiter;
import com.nirvana.service.SensitiveWordUtil.SensitiveWord;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;


@QuarkusMain
public class Main {

    public static final String WORD_PATH = "WORD_PATH";
    public static final String ADDON_WORD_PATH = "ADDON_WORD_PATH";
    private static FileAlterationMonitor dumpFileMonitor;

    public static void main(String[] args) {
        Quarkus.run(Service.class, args);
    }

    public static class Service implements QuarkusApplication {

        @Override
        public int run(String... args) throws Exception {

            int permitsPerSecond = 200;
            SensitiveWordFilterService.rateLimiter = RateLimiter.create(permitsPerSecond);

            loadBaseWords();
            updateAddonWords();
            monitorPathChangedToUpdateSensitiveWords(System.getenv(ADDON_WORD_PATH));

            Quarkus.waitForExit();
            return 0;
        }
    }

    private static boolean isAddonFile(File file) {
        String path1 = file.getAbsolutePath();
        String path2 = Paths.get(System.getenv(ADDON_WORD_PATH)).toAbsolutePath().toString();
        if (path1.equals(path2)) {
            return true;
        }
        return false;
    }

    private static void monitorPathChangedToUpdateSensitiveWords(String filePath) throws Exception {
        // 监控目录
        String rootDir = filePath;
        File file = new File(filePath);
        if (file.isFile()) {
            rootDir = file.getParent();
        }
        IOFileFilter fileFilter = FileFilterUtils.and(
            FileFilterUtils.fileFileFilter(),
            FileFilterUtils.suffixFileFilter(".txt"),
            HiddenFileFilter.VISIBLE
        );
        IOFileFilter dirFilter = FileFilterUtils.and(
            FileFilterUtils.directoryFileFilter(),
            HiddenFileFilter.VISIBLE
        );
        IOFileFilter filter = FileFilterUtils.or(dirFilter, fileFilter);
        FileAlterationObserver observer = new FileAlterationObserver(new File(rootDir), filter);
        observer.addListener(new FileAlterationListener() {
            @Override
            public void onStart(FileAlterationObserver observer) {
//                Log.info("onStart");
            }

            @Override
            public void onDirectoryCreate(File directory) {
            }

            @Override
            public void onDirectoryChange(File directory) {
            }

            @Override
            public void onDirectoryDelete(File directory) {
            }

            @Override
            public void onFileCreate(File file) {
                if (!isAddonFile(file)) {
                    return;
                }
                Log.info("onFileCreate");
                updateAddonWords();
            }

            @Override
            public void onFileChange(File file) {
                if (!isAddonFile(file)) {
                    return;
                }
                Log.info("onFileChange");
                updateAddonWords();
            }

            @Override
            public void onFileDelete(File file) {
                if (!isAddonFile(file)) {
                    return;
                }
                Log.info("onFileDelete");
                updateAddonWords();
            }

            @Override
            public void onStop(FileAlterationObserver observer) {
            }
        });
        dumpFileMonitor = new FileAlterationMonitor(5000, observer);
        dumpFileMonitor.start();
    }

    private static void loadBaseWords() {
        Set<String> baseWords = loadWordsFromEnv(WORD_PATH);

        Set<SensitiveWord> words = baseWords.stream()
            .parallel()
            .map(word -> new SensitiveWord((byte) 1, word.toLowerCase())).collect(Collectors.toSet());
        SensitiveWordUtil.initBaseWords(words);
        Log.info("initBaseWords num:" + words.size());
    }

    private static void updateAddonWords() {
        Set<String> addonWords = loadWordsFromEnv(ADDON_WORD_PATH);

        Set<SensitiveWord> words = addonWords.stream()
            .parallel()
            .map(word -> new SensitiveWord((byte) 1, word.toLowerCase())).collect(Collectors.toSet());
        SensitiveWordUtil.initAddonWords(words);
        Log.info("updateAddonWords num:" + words.size());
    }

    public static Set<String> loadWordsFromEnv(String envName) {
        String envPath = System.getenv(envName);
        if (envPath == null) {
            return Collections.EMPTY_SET;
        }
        Path path = Paths.get(envPath);
        Log.info("load words from env:" + envName + ", path:" + envPath);
        return loadWords(path);
    }

    public static Set<String> updateWordsFromEnv(String envName, Set<String> words) {
        String envPath = System.getenv(envName);
        Path path = Paths.get(envPath);
        try {
            Files.deleteIfExists(path);
            Files.write(path, words, StandardCharsets.UTF_8);
            Log.info("update words from env:" + envName + ", path:" + envPath);
            return loadWords(path);
        } catch (IOException e) {
            Log.error("update words error", e);
            return Collections.EMPTY_SET;
        }
    }

    private static Set<String> loadWords(Path path) {
        try {
            Set<String> words = Files.lines(path, StandardCharsets.UTF_8).collect(Collectors.toSet());
            Log.info("load path:" + path + ", words num:" + words.size());
            return words;
        } catch (Exception e) {
            Log.error("load words error", e);
            return Collections.EMPTY_SET;
        }
    }
}
