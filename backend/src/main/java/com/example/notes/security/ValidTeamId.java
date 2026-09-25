package com.example.notes.security;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@NotBlank
@Size(max = 64)
@Pattern(regexp = "[A-Za-z0-9._-]+")
@Constraint(validatedBy = {})
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface ValidTeamId {
    String message() default "Team ID contains unsupported characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
