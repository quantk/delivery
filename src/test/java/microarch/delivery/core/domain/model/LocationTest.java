package microarch.delivery.core.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class LocationTest {
    @Test
    void derivedAggregate() {
        assertThat(Location.class.getSuperclass().getSimpleName()).isEqualTo("ValueObject");
    }

    @ParameterizedTest(name = "({0},{1})")
    @CsvSource({ "1, 1", "1, 10", "10, 1", "10, 10", "5, 7", })
    void shouldCreateWithValidCoordinates(int x, int y) {
        var location = Location.create(x, y).getValueOrThrow();

        assertThat(location.getX()).isEqualTo(x);
        assertThat(location.getY()).isEqualTo(y);
    }

    @ParameterizedTest(name = "({0},{1}) -> {2}")
    @CsvSource({ "0, 5, min_x", "-1, 5, min_x", "5, 0, min_y", "11, 5, max_x", "5, 11, max_y", "0, 0, min_x",
            "11, 11, max_x", })
    void shouldFailOnOutOfRangeCoordinates(int x, int y, String expectedParam) {
        var result = Location.create(x, y);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains(expectedParam);
    }

    @ParameterizedTest(name = "({0},{1}) -> ({2},{3}) = {4}")
    @CsvSource({ "2, 6, 4, 9, 5", "4, 9, 2, 6, 5", "3, 3, 3, 3, 0", "1, 1, 10, 10, 18", "1, 5, 7, 5, 6",
            "5, 1, 5, 8, 7", })
    void shouldCalculateDistance(int x1, int y1, int x2, int y2, int expected) {
        var from = Location.create(x1, y1).getValueOrThrow();
        var to = Location.create(x2, y2).getValueOrThrow();

        assertThat(from.calculateDistance(to)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "({0},{1})")
    @CsvSource({ "2, 6", "1, 1", "10, 10", })
    void shouldBeEqualOnSameCoordinates(int x, int y) {
        var loc1 = Location.create(x, y).getValueOrThrow();
        var loc2 = Location.create(x, y).getValueOrThrow();

        assertThat(loc1).isEqualTo(loc2);
        assertThat(loc1.hashCode()).isEqualTo(loc2.hashCode());
    }

    @ParameterizedTest(name = "({0},{1}) != ({2},{3})")
    @CsvSource({ "2, 6, 4, 6", "2, 6, 2, 7", "2, 6, 6, 2", })
    void shouldBeNotEqualOnDifferentCoordinates(int x1, int y1, int x2, int y2) {
        var loc1 = Location.create(x1, y1).getValueOrThrow();
        var loc2 = Location.create(x2, y2).getValueOrThrow();

        assertThat(loc1).isNotEqualTo(loc2);
    }
}
