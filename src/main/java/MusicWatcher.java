import com.tngtech.configbuilder.ConfigBuilder;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class MusicWatcher {
    public static void main(String[] args){

        MusicWatcherConfig config = new ConfigBuilder<>(MusicWatcherConfig.class).withCommandLineArgs(args).build();
        Map<WatchKey,Path> keys = new HashMap<>();
        Map<Path,Set<Path>> folders = new HashMap<>();

        try {
            WatchService watchService = config.getSourceDir().getFileSystem().newWatchService();
            WatchKey key = config.getSourceDir().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            keys.put(key,config.getSourceDir());

            for(;;) {
               WatchKey watchKey = watchService.poll(60, TimeUnit.SECONDS);
               if(watchKey == null && !folders.isEmpty()) {
                   FilesProcessor filesProcessor = new FilesProcessor(new HashMap<>(folders), config);
                   Thread t = new Thread(filesProcessor);
                   t.start();
                   folders.clear();
               }
               else if(watchKey != null) {
                  List<WatchEvent<?>> events = watchKey.pollEvents();
                  for (WatchEvent event : events) {
                      Path dir = keys.get(watchKey);
                      Path newPath = dir.resolve((Path)event.context());
                      System.out.println(event.kind().toString() + ": " + newPath);
                      if (dir == null) {
                          System.err.println("WatchKey not recognized!!");
                          continue;
                      }
                      if(!newPath.toFile().isDirectory() && newPath.toString().toLowerCase().endsWith(".mp3")) {
                          System.out.println(newPath + " is mp3 file");
                          if(!folders.containsKey(dir)) {
                              folders.put(dir,new HashSet<Path>());
                          }
                          folders.get(dir).add(newPath);
                      }
                      else if(newPath.toFile().isDirectory()) {
                          System.out.println(newPath + " is directory");
                          registerAll(newPath, keys, watchService);

                      }

                  }
                  if (!watchKey.reset()) {
                      keys.remove(watchKey);
                      if (keys.isEmpty()) {
                          break;
                      }
                  }
               }
            }
        } catch (Exception e) {
            System.out.println("Exception");
        }
    }

    private static void registerAll(final Path start, final Map<WatchKey,Path> keys, final WatchService watchService) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir, keys, watchService);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void register(Path dir, Map<WatchKey,Path> keys, WatchService watchService) throws IOException {
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);
        Path prev = keys.get(key);
        if (prev == null) {
            System.out.format("register: %s\n", dir);
        } else {
            if (!dir.equals(prev)) {
                System.out.format("update: %s -> %s\n", prev, dir);
            }
        }
        keys.put(key, dir);
    }
}
