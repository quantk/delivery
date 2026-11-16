package microarch.delivery.core.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class VolumeTest {
    @Test
    void derivedValueObject() {
        assertThat(Volume.class.getSuperclass().getSimpleName()).isEqualTo("ValueObject");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({ "1", "5", "100", })
    void shouldBeCorrectWhenValueIsPositiveOnCreated(int value) {
        // Act
        var result = Volume.create(value);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getValue()).isEqualTo(value);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({ "0", "-1", "-100", })
    void shouldFailWhenValueIsNotPositiveOnCreated(int value) {
        // Act
        var result = Volume.create(value);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("min_volume");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({ "1", "7", })
    void shouldBeEqualWhenValuesAreEqual(int value) {
        // Arrange
        var volume1 = Volume.create(value).getValueOrThrow();
        var volume2 = Volume.create(value).getValueOrThrow();

        // Act & Assert
        assertThat(volume1).isEqualTo(volume2);
        assertThat(volume1.hashCode()).isEqualTo(volume2.hashCode());
    }

    @ParameterizedTest(name = "{0} != {1}")
    @CsvSource({ "1, 2", "7, 3", })
    void shouldNotBeEqualWhenValuesAreDifferent(int value1, int value2) {
        // Arrange
        var volume1 = Volume.create(value1).getValueOrThrow();
        var volume2 = Volume.create(value2).getValueOrThrow();

        // Act & Assert
        assertThat(volume1).isNotEqualTo(volume2);
    }
}
