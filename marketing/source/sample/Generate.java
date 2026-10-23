import java.nio.file.Path;
import tn.naizo.jauml.config.TypedConfig;

// Writes bosses.json the way a mod's TypedConfig would on first launch.
public class Generate {
    public static void main(String[] args) {
        TypedConfig.builder(BossConfig.class).path(Path.of(args[0])).build();
    }
}
