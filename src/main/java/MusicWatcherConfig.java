import com.tngtech.configbuilder.FieldValueProvider;
import com.tngtech.configbuilder.annotation.configuration.CollectionType;
import com.tngtech.configbuilder.annotation.propertyloaderconfiguration.PropertiesFiles;
import com.tngtech.configbuilder.annotation.valueextractor.CommandLineValue;
import com.tngtech.configbuilder.annotation.valueextractor.DefaultValue;
import com.tngtech.configbuilder.annotation.valueextractor.PropertyValue;
import com.tngtech.configbuilder.annotation.valuetransformer.ValueTransformer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@PropertiesFiles("musicwatcher")
public class MusicWatcherConfig {

    public static class PathFactory implements FieldValueProvider<Path> {
        public Path getValue(String path) {
            return Paths.get(path);
        }
    }

    @CollectionType
    @DefaultValue("getTrack,getTitle")
    @PropertyValue("naming")
    private List<String> naming;

    @CollectionType
    @DefaultValue("getAlbumArtist,getAlbum")
    @PropertyValue("folderStructure")
    private List<String> folderStructure;

    @DefaultValue("-")
    @PropertyValue("separator")
    private String separator;

    @DefaultValue("/home/matthias/Musik/Download")
    @PropertyValue("sourceDir")
    @CommandLineValue(shortOpt = "s", longOpt = "source", hasArg = true)
    @ValueTransformer(PathFactory.class)
    private Path sourceDir;

    @DefaultValue("/home/matthias/Musik/Musik")
    @PropertyValue("targetDir")
    @CommandLineValue(shortOpt = "t", longOpt = "target", hasArg = true)
    @ValueTransformer(PathFactory.class)
    private Path targetDir;

    public Path getTargetDir() {
        return targetDir;
    }

    public Path getSourceDir() {
        return sourceDir;
    }

    public String getSeparator() {
        return separator;
    }

    public List<String> getNaming() {
        return naming;
    }

    public List<String> getFolderStructure() {
        return folderStructure;
    }
}
