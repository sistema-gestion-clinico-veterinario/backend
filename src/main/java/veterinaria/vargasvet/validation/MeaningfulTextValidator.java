package veterinaria.vargasvet.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MeaningfulTextValidator implements ConstraintValidator<MeaningfulText, String> {
    private boolean requireLetter;

    @Override
    public void initialize(MeaningfulText constraintAnnotation) {
        this.requireLetter = constraintAnnotation.requireLetter();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) return true;

        String trimmed = value.trim();
        if (trimmed.isEmpty()) return false;
        if (!trimmed.equals(value)) return false;
        if (trimmed.matches("^[\\p{Punct}\\p{S}\\s]+$")) return false;
        if (requireLetter && !trimmed.matches(".*\\p{L}.*")) return false;
        if (requireLetter && trimmed.matches("^\\d+$")) return false;
        if (trimmed.matches(".*[\\p{Punct}\\p{S}]{6,}.*")) return false;
        long punctuationCount = trimmed.codePoints()
                .filter(cp -> !Character.isLetterOrDigit(cp) && !Character.isWhitespace(cp))
                .count();
        if (punctuationCount >= 8 && ((double) punctuationCount / trimmed.length()) > 0.45) return false;
        if (trimmed.matches("(?is).*<\\s*/?\\s*(script|iframe|object|embed|style|img|svg|body|html|link|meta)\\b.*")) return false;
        return !trimmed.matches("(?is).*(javascript:|data:text/html|on\\w+\\s*=).*");
    }
}
