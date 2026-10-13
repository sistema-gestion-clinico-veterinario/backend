package veterinaria.vargasvet.util;

public final class DataMasking {

    private static final char MASK = '*';
    private static final int DOCUMENT_VISIBLE = 4;
    private static final int PHONE_VISIBLE = 3;

    private DataMasking() {
    }

    public static String documento(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String clean = value.trim();
        int visible = clean.length() > DOCUMENT_VISIBLE ? DOCUMENT_VISIBLE : 0;
        return maskedExceptLast(clean, visible, false);
    }

    public static String telefono(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String clean = value.trim();
        long digits = clean.chars().filter(Character::isDigit).count();
        int visible = digits > PHONE_VISIBLE ? PHONE_VISIBLE : 0;
        return maskedExceptLast(clean, visible, true);
    }

    public static String correo(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String clean = value.trim();
        int at = clean.lastIndexOf('@');
        if (at <= 0) {
            return clean.charAt(0) + "***";
        }
        return clean.charAt(0) + "***" + clean.substring(at);
    }

    private static String maskedExceptLast(String value, int visible, boolean digitsOnly) {
        StringBuilder result = new StringBuilder(value.length());
        int seen = 0;
        for (int i = value.length() - 1; i >= 0; i--) {
            char current = value.charAt(i);
            boolean maskable = !digitsOnly || Character.isDigit(current);
            if (!maskable) {
                result.append(current);
                continue;
            }
            seen++;
            result.append(seen <= visible ? current : MASK);
        }
        return result.reverse().toString();
    }
}
