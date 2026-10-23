import java.util.ArrayList;
import java.util.List;
import tn.naizo.jauml.config.Comment;
import tn.naizo.jauml.config.Range;

@Comment("Example Bosses settings")
public class BossConfig {
    @Comment("Health multiplier for every boss")
    @Range(min = 0.1, max = 10)
    public double healthMultiplier = 1.0;

    @Comment("Seconds between boss abilities")
    @Range(min = 1, max = 60)
    public int abilityCooldown = 8;

    @Comment("Boss difficulty preset")
    public Difficulty difficulty = Difficulty.NORMAL;

    @Comment("Dimensions where bosses never spawn")
    public List<String> disabledDimensions =
        new ArrayList<>(List.of("minecraft:the_end"));
}
