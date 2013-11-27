import com.tngtech.configbuilder.ConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class MusicWatcher {

    private final static Logger log = LoggerFactory.getLogger(MusicWatcher.class);

    private MusicWatcherConfig config;
    private Map<WatchKey,Path> keys = new HashMap<>();
    private Map<Path,Set<Path>> folders = new HashMap<>();
    private WatchService watchService;

    public void watch(String[] args){
        try {
            initialize(args);
            waitForAndProcessEvents();
        } catch (Exception e) {
            log.warn("Exception");
        }
    }

    private void initialize(String[] args) throws IOException {
        config = new ConfigBuilder<>(MusicWatcherConfig.class).withCommandLineArgs(args).build();
        watchService = config.getSourceDir().getFileSystem().newWatchService();
        WatchKey key = config.getSourceDir().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        keys.put(key,config.getSourceDir());
    }

    private void waitForAndProcessEvents() throws InterruptedException, IOException {
        for(;;) {
           WatchKey watchKey = watchService.poll(60, TimeUnit.SECONDS);
           if(watchKey == null && !folders.isEmpty()) {
               processFilesInNewThread();
           }
           else if(watchKey != null) {
               if (!watchKeyIsRecognizedAndProcessed(watchKey)) continue;
               if (noWatchKeysLeftAfterResettingOrRemoving(watchKey)) break;
           }
        }
    }

    private void processFilesInNewThread() {
        FilesProcessor filesProcessor = new FilesProcessor(new HashMap<>(folders), config);
        Thread t = new Thread(filesProcessor);
        t.start();
        folders.clear();
    }

    private boolean watchKeyIsRecognizedAndProcessed(WatchKey watchKey) throws IOException {
        List<WatchEvent<?>> events = watchKey.pollEvents();
        Path dir = keys.get(watchKey);
        if (dir == null) {
            log.warn("WatchKey not recognized!");
            return false;
        }
        else {
            processEvents(events, dir);
            return true;
        }
    }

    private void processEvents(List<WatchEvent<?>> events, Path dir) throws IOException {
        for (WatchEvent event : events) {
            Path newPath = dir.resolve((Path)event.context());
            log.debug(event.kind().toString() + ": " + newPath);
            if(!newPath.toFile().isDirectory() && newPath.toString().toLowerCase().endsWith(".mp3")) {
                log.debug(newPath + " is mp3 file");
                addMp3FileToMap(dir, newPath);
            }
            else if(newPath.toFile().isDirectory()) {
                log.debug(newPath + " is directory");
                registerDirAndSubDirs(newPath, keys, watchService);
            }
        }
    }

    private void addMp3FileToMap(Path dir, Path newPath) {
        if(!folders.containsKey(dir)) {
            folders.put(dir, new HashSet<Path>());
        }
        folders.get(dir).add(newPath);
    }

    private void registerDirAndSubDirs(final Path start, final Map<WatchKey, Path> keys, final WatchService watchService) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDir(dir, keys, watchService);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void registerDir(Path dir, Map<WatchKey, Path> keys, WatchService watchService) throws IOException {
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);
        Path prev = keys.get(key);
        if (prev == null) {
            System.out.format("registerDir: %s\n", dir);
        } else {
            if (!dir.equals(prev)) {
                System.out.format("update: %s -> %s\n", prev, dir);
            }
        }
        keys.put(key, dir);
    }

    private boolean noWatchKeysLeftAfterResettingOrRemoving(WatchKey watchKey) {
        if (!watchKey.reset()) {
            keys.remove(watchKey);
            if (keys.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
