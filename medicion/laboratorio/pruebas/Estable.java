import java.util.ArrayList;
import java.util.List;

public class Estable {
    public static void main(String[] args) throws Exception {
        List<byte[]> retenidos = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            retenidos.add(new byte[128 * 1024]);
            if (retenidos.size() > 200) {
                retenidos.subList(0, 100).clear();
            }
        }
        System.gc();
        System.out.println("LISTO");
        Thread.sleep(Long.MAX_VALUE);
    }
}
