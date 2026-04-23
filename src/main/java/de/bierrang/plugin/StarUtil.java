package de.bierrang.plugin;

public class StarUtil {

    // Gold: #FFD700
    private static final String GOLD = "§x§F§F§D§7§0§0";

    // Grau: #7A7A7A
    private static final String GRAY = "§x§7§A§7§A§7§A";

    // Dunkelblauer Rand: #0A1A4A
    private static final String BORDER = "§x§0§A§1§A§4§A";

    public static String getStars(int level) {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 1; i <= 5; i++) {
            if (i <= level) {
                sb.append(GOLD).append("★");
            } else {
                sb.append(BORDER).append("◆").append(GRAY).append("☆");
            }
        }

        sb.append("]");
        return sb.toString();
    }
}
