package microarch.delivery.core.domain.model.courier;

import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentTest {
    private static final Volume VOLUME = Volume.create(5).getValueOrThrow();
    private static final Location ORDER_LOCATION = Location.create(5, 5).getValueOrThrow();

    private static Assignment createAssignment() {
        return Assignment.create(UUID.randomUUID(), VOLUME, ORDER_LOCATION).getValueOrThrow();
    }

    @Test
    void derivedEntity() {
        assertThat(Assignment.class.getSuperclass().getSimpleName()).isEqualTo("BaseEntity");
    }

    @Test
    void shouldBeAssignedWhenParamsAreCorrectOnCreated() {
        // Arrange
        var orderId = UUID.randomUUID();

        // Act
        var result = Assignment.create(orderId, VOLUME, ORDER_LOCATION);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        var assignment = result.getValue();
        assertThat(assignment.getId()).isNotNull();
        assertThat(assignment.getOrderId()).isEqualTo(orderId);
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    void shouldFailWhenOrderIdIsNullOnCreated() {
        // Act
        var result = Assignment.create(null, VOLUME, ORDER_LOCATION);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("order_id");
    }

    @Test
    void shouldFailWhenOrderIdIsEmptyOnCreated() {
        // Arrange
        var emptyOrderId = new UUID(0L, 0L);

        // Act
        var result = Assignment.create(emptyOrderId, VOLUME, ORDER_LOCATION);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("order_id");
    }

    @Test
    void shouldFailWhenVolumeIsNullOnCreated() {
        // Act
        var result = Assignment.create(UUID.randomUUID(), null, ORDER_LOCATION);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("volume");
    }

    @Test
    void shouldFailWhenLocationIsNullOnCreated() {
        // Act
        var result = Assignment.create(UUID.randomUUID(), VOLUME, null);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("location");
    }

    @ParameterizedTest(name = "courier at ({0},{1})")
    @CsvSource({ "5, 5", "4, 5", "6, 5", "5, 4", "5, 6", })
    void canCompleteWhenCourierIsWithinOneStep(int x, int y) {
        // Arrange
        var assignment = createAssignment();
        var courierLocation = Location.create(x, y).getValueOrThrow();

        // Act
        var result = assignment.complete(courierLocation);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
    }

    @ParameterizedTest(name = "courier at ({0},{1})")
    @CsvSource({ "4, 4", "6, 6", "3, 5", "5, 7", "1, 1", "10, 10", })
    void cannotCompleteWhenCourierIsTooFar(int x, int y) {
        // Arrange
        var assignment = createAssignment();
        var courierLocation = Location.create(x, y).getValueOrThrow();

        // Act
        var result = assignment.complete(courierLocation);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("assignment.courier.too.far");
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    void cannotCompleteWhenCourierLocationIsNull() {
        // Arrange
        var assignment = createAssignment();

        // Act
        var result = assignment.complete(null);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("courier_location");
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    void cannotCompleteTwice() {
        // Arrange
        var assignment = createAssignment();
        assignment.complete(ORDER_LOCATION);

        // Act
        var result = assignment.complete(ORDER_LOCATION);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("assignment.already.completed");
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
    }

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
        // Arrange
        var assignment = createAssignment();

        // Act & Assert
        assertThat(assignment).isEqualTo(assignment);
    }

    @Test
    void shouldNotBeEqualWhenIdsAreDifferentWithSameData() {
        // Arrange
        var orderId = UUID.randomUUID();
        var assignment1 = Assignment.create(orderId, VOLUME, ORDER_LOCATION).getValueOrThrow();
        var assignment2 = Assignment.create(orderId, VOLUME, ORDER_LOCATION).getValueOrThrow();

        // Act & Assert
        assertThat(assignment1).isNotEqualTo(assignment2);
    }
}
