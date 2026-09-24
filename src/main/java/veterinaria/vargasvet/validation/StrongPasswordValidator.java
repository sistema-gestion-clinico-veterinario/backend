package veterinaria.vargasvet.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    private static final Pattern UPPER = Pattern.compile("[A-ZÁÉÍÓÚÑÜ]");
    private static final Pattern LOWER = Pattern.compile("[a-záéíóúñü]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9ÁÉÍÓÚÑÜáéíóúñü]");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) return true;

        return UPPER.matcher(value).find()
                && LOWER.matcher(value).find()
                && DIGIT.matcher(value).find()
                && SYMBOL.matcher(value).find();
    }
}
