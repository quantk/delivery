package microarch.delivery.core.domain.model.order;

import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {
    private static final Volume VOLUME = Volume.create(5).getValueOrThrow();
    private static final Location LOCATION = Location.create(5, 5).getValueOrThrow();

    private static Order createOrder() {
        return Order.create(UUID.randomUUID(), LOCATION, VOLUME).getValueOrThrow();
    }

    private static Order createAssignedOrder() {
        var order = createOrder();
        order.assign().getOrElseThrow();
        return order;
    }

    private static Order createCompletedOrder() {
        var order = createAssignedOrder();
        order.complete().getOrElseThrow();
        return order;
    }

    @Test
    void derivedAggregate() {
        assertThat(Order.class.getSuperclass().getSimpleName()).isEqualTo("Aggregate");
    }

    @Test
    void shouldBeCreatedWhenParamsAreCorrectOnCreated() {
        // Arrange
        var orderId = UUID.randomUUID();

        // Act
        var result = Order.create(orderId, LOCATION, VOLUME);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        var order = result.getValue();
        assertThat(order.getId()).isEqualTo(orderId);
        assertThat(order.getLocation()).isEqualTo(LOCATION);
        assertThat(order.getVolume()).isEqualTo(VOLUME);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void shouldFailWhenIdIsNullOnCreated() {
        // Act
        var result = Order.create(null, LOCATION, VOLUME);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("order_id");
    }

    @Test
    void shouldFailWhenIdIsEmptyOnCreated() {
        // Arrange
        var emptyId = new UUID(0L, 0L);

        // Act
        var result = Order.create(emptyId, LOCATION, VOLUME);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("order_id");
    }

    @Test
    void shouldFailWhenLocationIsNullOnCreated() {
        // Act
        var result = Order.create(UUID.randomUUID(), null, VOLUME);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("location");
    }

    @Test
    void shouldFailWhenVolumeIsNullOnCreated() {
        // Act
        var result = Order.create(UUID.randomUUID(), LOCATION, null);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).contains("volume");
    }

    @Test
    void canAssignWhenCreated() {
        // Arrange
        var order = createOrder();

        // Act
        var result = order.assign();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
    }

    @Test
    void cannotAssignWhenAlreadyAssigned() {
        // Arrange
        var order = createAssignedOrder();

        // Act
        var result = order.assign();

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order.forbidden.status.transition");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
    }

    @Test
    void cannotAssignWhenCompleted() {
        // Arrange
        var order = createCompletedOrder();

        // Act
        var result = order.assign();

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order.forbidden.status.transition");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void canCompleteWhenAssigned() {
        // Arrange
        var order = createAssignedOrder();

        // Act
        var result = order.complete();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void cannotCompleteWhenCreated() {
        // Arrange
        var order = createOrder();

        // Act
        var result = order.complete();

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order.forbidden.status.transition");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void cannotCompleteTwice() {
        // Arrange
        var order = createCompletedOrder();

        // Act
        var result = order.complete();

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order.forbidden.status.transition");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
        // Arrange
        var orderId = UUID.randomUUID();
        var order1 = Order.create(orderId, LOCATION, VOLUME).getValueOrThrow();
        var order2 = Order.create(orderId, Location.create(1, 1).getValueOrThrow(), VOLUME).getValueOrThrow();

        // Act & Assert
        assertThat(order1).isEqualTo(order2);
    }

    @Test
    void shouldNotBeEqualWhenIdsAreDifferentWithSameData() {
        // Arrange
        var order1 = createOrder();
        var order2 = createOrder();

        // Act & Assert
        assertThat(order1).isNotEqualTo(order2);
    }
}
