package pw.gatchina.jups;

/**
 * @author maximvarentsov
 * @since 20.04.2022
 */
public class Utils {

    public static String toHex(String str) {
        return toHex(str.getBytes());
    }

    public static String toHex(byte[] bytes) {
        final var sb = new StringBuilder();
        var iter = 1;
        for (final var aByte : bytes) {
            sb.append(String.format("%02X ", aByte));
            if ((iter % 6) == 0) {
                sb.append("\n");
            }
            iter++;
        }

        return sb.toString();
    }
}
