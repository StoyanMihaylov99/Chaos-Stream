package com.portfolio.chaosstream.validation.service;

import com.portfolio.chaosstream.model.TransactionEvent;
import com.portfolio.chaosstream.validation.model.ValidationResult;
import com.portfolio.chaosstream.validation.service.rule.TransactionValidationRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionValidationServiceTest {

    private final TransactionEvent event = mock(TransactionEvent.class);

    @Test
    void validateSuccessful_allRulesPass_returnsValid() {
        // Given
        TransactionValidationRule passingRule = mock(TransactionValidationRule.class);
        when(passingRule.validate(event)).thenReturn(Optional.empty());
        TransactionValidationService service = new TransactionValidationService(List.of(passingRule));

        // When
        ValidationResult result = service.validate(event);

        // Then
        assertTrue(result.isValid());
        assertEquals(List.of(), result.violations());
    }

    @Test
    void validateUnsuccessful_someRulesFail_returnsInvalidWithCollectedViolations() {
        // Given
        TransactionValidationRule passingRule = mock(TransactionValidationRule.class);
        when(passingRule.validate(event)).thenReturn(Optional.empty());

        TransactionValidationRule failingRule = mock(TransactionValidationRule.class);
        when(failingRule.validate(event)).thenReturn(Optional.of("violation-1"));

        TransactionValidationRule anotherFailingRule = mock(TransactionValidationRule.class);
        when(anotherFailingRule.validate(event)).thenReturn(Optional.of("violation-2"));

        // When
        TransactionValidationService service =
                new TransactionValidationService(List.of(passingRule, failingRule, anotherFailingRule));

        ValidationResult result = service.validate(event);

        // Then
        assertFalse(result.isValid());
        assertEquals(List.of("violation-1", "violation-2"), result.violations());
    }

    @Test
    void validate_noRules_returnsValid() {
        // Given
        TransactionValidationService service = new TransactionValidationService(List.of());

        // When
        ValidationResult result = service.validate(event);

        // Then
        assertTrue(result.isValid());
    }
}
