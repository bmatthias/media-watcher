import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class FilesProcessor implements Runnable {
    private Map<Path,Set<Path>> files;
    private MusicWatcherConfig config;

    public FilesProcessor(Map<Path,Set<Path>> files, MusicWatcherConfig config) {
        this.files = files;
        this.config = config;
    }

    public void run() {
        try{
            for(Map.Entry<Path,Set<Path>> folder : files.entrySet()) {
                if(folder.getValue().isEmpty()) {
                    continue;
                }
                boolean isVarious = isVariousArtists(folder);
                for (Path path : folder.getValue()) {
                    Mp3File mp3File = new Mp3File(path.toString());
                    ID3v2 id3v2 = mp3File.getId3v2Tag();

                    if(id3v2.getAlbumArtist() == null) {
                        if(isVarious) {
                            id3v2.setAlbumArtist("Various Artists");
                            id3v2.setCompilation(true);
                            mp3File.setId3v2Tag(id3v2);
                        }
                        else {
                            id3v2.setAlbumArtist(getFirstArtist(id3v2));
                            mp3File.setId3v2Tag(id3v2);
                        }
                    }

                    StringBuffer sb = new StringBuffer(config.getTargetDir().toString() + File.separator);

                    for(String subFolderMethod : config.getFolderStructure()) {
                        String subFolderName = (String)id3v2.getClass().getMethod(subFolderMethod).invoke(id3v2);
                        if(subFolderMethod == "getAlbumArtist" && (subFolderMethod.startsWith("The ") ||subFolderMethod.startsWith("Die "))) {
                            String suffix = subFolderName.substring(5);
                            subFolderName = subFolderName.replace("The ", "") + ", " + suffix;
                        }
                        sb.append(subFolderName);
                        sb.append(File.separator);
                    }

                    Files.createDirectories(Paths.get(sb.toString()));

                    for(String namingPart : config.getNaming()) {
                        sb.append(((String)id3v2.getClass().getMethod(namingPart).invoke(id3v2)).split("/")[0]);
                        if(!(config.getNaming().indexOf(namingPart) == config.getNaming().size() - 1)) {
                            sb.append(" " + config.getSeparator() + " ");
                        }
                    }
                    System.out.println("saving " + sb.toString() + ".mp3");
                    mp3File.save(sb.toString() + ".mp3");
                }
            }
            System.out.println("done processing mp3 files");
            return;
        }
        catch (Exception e) {
            System.out.println("Exception");
        }
    }

    private boolean isVariousArtists(Map.Entry<Path,Set<Path>> folder) {
        boolean isVarious = false;
        try {
            Iterator iterator = folder.getValue().iterator();
            Mp3File firstFile = new Mp3File(iterator.next().toString());
            ID3v2 firstFileId3v2Tag = firstFile.getId3v2Tag();
            while(iterator.hasNext()) {
                Mp3File mp3File = new Mp3File(iterator.next().toString());
                ID3v2 id3v2 = mp3File.getId3v2Tag();
                isVarious |= !(getFirstArtist(id3v2).equals(getFirstArtist(firstFileId3v2Tag)));
            }
        } catch (Exception e) {
            System.out.println("Exception");
        }
        return isVarious;
    }

    private String getFirstArtist(ID3v2 id3v2) {
        return id3v2.getArtist()
                .split("/")[0]
                .split(",")[0]
                .split(";")[0]
                .split(" featuring ")[0]
                .split(" Featuring ")[0]
                .split(" feat_ ")[0]
                .split(" feat. ")[0]
                .split(" feat ")[0]
                .split(" Feat_ ")[0]
                .split(" Feat. ")[0]
                .split(" Feat ")[0]
                .split(" ft_ ")[0]
                .split(" ft. ")[0]
                .split(" Ft. ")[0]
                .split(" ft ")[0]
                .split(" Ft ")[0]
                .split(" FT ")[0].trim();
    }
}
