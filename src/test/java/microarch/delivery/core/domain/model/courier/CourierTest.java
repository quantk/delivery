package microarch.delivery.core.domain.model.courier;

import libs.errs.Error;
import libs.errs.UnitResult;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import microarch.delivery.core.domain.model.order.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourierTest {
    private static final String NAME = "Ivan";
    private static final Location COURIER_LOCATION = Location.create(5, 5).getValueOrThrow();

    private static Courier createCourier() {
        return Courier.create(NAME, COURIER_LOCATION).getValueOrThrow();
    }

    private static Order createOrder(int volume) {
        return createOrder(volume, COURIER_LOCATION);
    }

    private static Order createOrder(int volume, Location location) {
        return Order.create(UUID.randomUUID(), location, Volume.create(volume).getValueOrThrow()).getValueOrThrow();
    }

    private static UnitResult<Error> take(Courier courier, Order order) {
        return courier.take(order.getId(), order.getVolume(), order.getLocation());
    }

    private static Volume volume(int value) {
        return Volume.create(value).getValueOrThrow();
    }

    @Test
    void derivedAggregate() {
        assertThat(Courier.class.getSuperclass().getSimpleName()).isEqualTo("Aggregate");
    }

    @Test
    void shouldBeCreatedWhenParamsAreCorrectOnCreated() {
        // Act
        var result = Courier.create(NAME, COURIER_LOCATION);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        var courier = result.getValue();
        assertThat(courier.getId()).isNotNull();
        assertThat(courier.getName()).isEqualTo(NAME);
        assertThat(courier.getLocation()).isEqualTo(COURIER_LOCATION);
        assertThat(courier.getMaxVolume().getValue()).isEqualTo(20);
        assertThat(courier.getAssignments()).isEmpty();
    }

    @ParameterizedTest(name = "name = \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   " })
    void shouldFailWhenNameIsBlankOnCreated(String name) {
        // Act
        var result = Courier.create(name, COURIER_LOCATION);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("name");
    }

    @Test
    void shouldFailWhenLocationIsNullOnCreated() {
        // Act
        var result = Courier.create(NAME, null);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("location");
    }

    @ParameterizedTest(name = "volume {0}")
    @CsvSource({ "1", "10", "20", })
    void canTakeWhenOrderFitsMaxVolume(int volume) {
        // Arrange
        var courier = createCourier();

        // Act
        var result = courier.isCanTake(volume(volume));

        // Assert
        assertThat(result.isSuccess()).isTrue();
    }

    @ParameterizedTest(name = "volume {0}")
    @CsvSource({ "21", "100", })
    void cannotTakeWhenOrderExceedsMaxVolume(int volume) {
        // Arrange
        var courier = createCourier();

        // Act
        var result = courier.isCanTake(volume(volume));

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.is.full");
    }

    @ParameterizedTest(name = "taken {0} + new {1}")
    @CsvSource({ "15, 5", "19, 1", "10, 10", })
    void canTakeWhenTotalVolumeFitsMaxVolume(int taken, int volume) {
        // Arrange
        var courier = createCourier();
        take(courier, createOrder(taken)).getOrElseThrow();

        // Act
        var result = courier.isCanTake(volume(volume));

        // Assert
        assertThat(result.isSuccess()).isTrue();
    }

    @ParameterizedTest(name = "taken {0} + new {1}")
    @CsvSource({ "15, 6", "20, 1", "10, 11", })
    void cannotTakeWhenTotalVolumeExceedsMaxVolume(int taken, int volume) {
        // Arrange
        var courier = createCourier();
        take(courier, createOrder(taken)).getOrElseThrow();

        // Act
        var result = courier.isCanTake(volume(volume));

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.is.full");
    }

    @Test
    void canTakeWhenPreviousAssignmentsAreCompleted() {
        // Arrange
        var courier = createCourier();
        var completedOrder = createOrder(20);
        take(courier, completedOrder).getOrElseThrow();
        courier.complete(completedOrder.getId()).getOrElseThrow();

        // Act
        var result = courier.isCanTake(volume(20));

        // Assert
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void cannotCheckTakeWhenVolumeIsNull() {
        // Act
        var result = createCourier().isCanTake(null);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("volume");
    }

    @Test
    void shouldAddAssignmentOnTake() {
        // Arrange
        var courier = createCourier();
        var order = createOrder(5);

        // Act
        var result = take(courier, order);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(courier.getAssignments()).hasSize(1);
        var assignment = courier.getAssignments().getFirst();
        assertThat(assignment.getOrderId()).isEqualTo(order.getId());
        assertThat(assignment.getVolume()).isEqualTo(order.getVolume());
        assertThat(assignment.getLocation()).isEqualTo(order.getLocation());
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    void shouldNotAddAssignmentWhenCourierIsFull() {
        // Arrange
        var courier = createCourier();
        take(courier, createOrder(20)).getOrElseThrow();

        // Act
        var result = take(courier, createOrder(1));

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.is.full");
        assertThat(courier.getAssignments()).hasSize(1);
    }

    @Test
    void cannotTakeWhenVolumeIsNull() {
        // Arrange
        var courier = createCourier();

        // Act
        var result = courier.take(UUID.randomUUID(), null, COURIER_LOCATION);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("volume");
        assertThat(courier.getAssignments()).isEmpty();
    }

    @Test
    void shouldNotExposeMutableAssignments() {
        // Arrange
        var courier = createCourier();
        take(courier, createOrder(5)).getOrElseThrow();

        // Act & Assert
        assertThat(courier.getAssignments()).isUnmodifiable();
    }

    @ParameterizedTest(name = "order at ({0},{1})")
    @CsvSource({ "5, 5", "4, 5", "6, 5", "5, 4", "5, 6", })
    void canCompleteWhenOrderIsWithinOneStep(int x, int y) {
        // Arrange
        var courier = createCourier();
        var order = createOrder(5, Location.create(x, y).getValueOrThrow());
        take(courier, order).getOrElseThrow();

        // Act
        var result = courier.complete(order.getId());

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(courier.getAssignments().getFirst().getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
    }

    @ParameterizedTest(name = "order at ({0},{1})")
    @CsvSource({ "4, 4", "6, 6", "3, 5", "5, 7", "1, 1", "10, 10", })
    void cannotCompleteWhenOrderIsTooFar(int x, int y) {
        // Arrange
        var courier = createCourier();
        var order = createOrder(5, Location.create(x, y).getValueOrThrow());
        take(courier, order).getOrElseThrow();

        // Act
        var result = courier.complete(order.getId());

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("assignment.courier.too.far");
        assertThat(courier.getAssignments().getFirst().getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    void shouldCompleteOnlyAssignmentOfGivenOrder() {
        // Arrange
        var courier = createCourier();
        var order1 = createOrder(5);
        var order2 = createOrder(5);
        take(courier, order1).getOrElseThrow();
        take(courier, order2).getOrElseThrow();

        // Act
        var result = courier.complete(order2.getId());

        // Assert
        assertThat(result.isSuccess()).isTrue();
        var assignments = courier.getAssignments();
        assertThat(assignments.get(0).getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(assignments.get(1).getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
    }

    @Test
    void cannotCompleteWhenOrderIsNotAssigned() {
        // Arrange
        var courier = createCourier();
        take(courier, createOrder(5)).getOrElseThrow();

        // Act
        var result = courier.complete(UUID.randomUUID());

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("assignment.not.found");
    }

    @Test
    void cannotCompleteTwice() {
        // Arrange
        var courier = createCourier();
        var order = createOrder(5);
        take(courier, order).getOrElseThrow();
        courier.complete(order.getId()).getOrElseThrow();

        // Act
        var result = courier.complete(order.getId());

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("assignment.already.completed");
    }

    @Test
    void cannotCompleteWhenOrderIdIsNull() {
        // Act
        var result = createCourier().complete(null);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("order_id");
    }

    @ParameterizedTest(name = "to ({0},{1})")
    @CsvSource({ "5, 5", "4, 5", "6, 5", "5, 4", "5, 6", })
    void canMoveWithinOneStep(int x, int y) {
        // Arrange
        var courier = createCourier();
        var target = Location.create(x, y).getValueOrThrow();

        // Act
        var result = courier.move(target);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(courier.getLocation()).isEqualTo(target);
    }

    @ParameterizedTest(name = "to ({0},{1})")
    @CsvSource({ "4, 4", "6, 6", "3, 5", "5, 7", "1, 1", "10, 10", })
    void cannotMoveFurtherThanOneStep(int x, int y) {
        // Arrange
        var courier = createCourier();

        // Act
        var result = courier.move(Location.create(x, y).getValueOrThrow());

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("location.is.too.far");
        assertThat(courier.getLocation()).isEqualTo(COURIER_LOCATION);
    }

    @Test
    void cannotMoveWhenLocationIsNull() {
        // Arrange
        var courier = createCourier();

        // Act
        var result = courier.move(null);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("new_location");
        assertThat(courier.getLocation()).isEqualTo(COURIER_LOCATION);
    }

    @Test
    void shouldNotBeEqualWhenIdsAreDifferentWithSameData() {
        // Arrange
        var courier1 = createCourier();
        var courier2 = createCourier();

        // Act & Assert
        assertThat(courier1).isNotEqualTo(courier2);
    }
}
