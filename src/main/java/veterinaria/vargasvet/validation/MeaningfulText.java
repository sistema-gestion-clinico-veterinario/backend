package veterinaria.vargasvet.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = MeaningfulTextValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MeaningfulText {
    String message() default "El campo debe contener texto valido, no solo espacios, numeros o simbolos";

    boolean requireLetter() default true;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
