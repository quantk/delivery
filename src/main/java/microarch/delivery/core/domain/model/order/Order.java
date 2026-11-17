package microarch.delivery.core.domain.model.order;

import jakarta.persistence.*;
import libs.ddd.Aggregate;
import libs.errs.Error;
import libs.errs.Guard;
import libs.errs.Result;
import libs.errs.UnitResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import microarch.delivery.core.domain.model.Location;
import microarch.delivery.core.domain.model.Volume;

import java.util.UUID;

@Entity
@Table(name = "orders")
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
public class Order extends Aggregate<UUID> {

    @Embedded
    @Getter
    private Location location;

    @Embedded
    @Getter
    private Volume volume;

    @Column(name = "status")
    @Getter
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Order(UUID id, Location location, Volume volume, OrderStatus status) {
        super(id);
        this.location = location;
        this.volume = volume;
        this.status = status;
    }

    public static Result<Order, Error> create(UUID id, Location location, Volume volume) {
        var err = Guard.combine(Guard.againstNullOrEmpty(id, "order_id"), Guard.againstNull(location, "location"),
                Guard.againstNull(volume, "volume"));

        if (err != null) {
            return Result.failure(err);
        }

        return Result.success(new Order(id, location, volume, OrderStatus.CREATED));
    }

    public UnitResult<Error> assign() {
        if (status != OrderStatus.CREATED) {
            return UnitResult.failure(Errors.orderStatusTransitionForbidden(status, OrderStatus.ASSIGNED));
        }

        this.status = OrderStatus.ASSIGNED;

        return UnitResult.success();
    }

    public UnitResult<Error> complete() {
        if (status != OrderStatus.ASSIGNED) {
            return UnitResult.failure(Errors.orderStatusTransitionForbidden(status, OrderStatus.COMPLETED));
        }

        this.status = OrderStatus.COMPLETED;

        return UnitResult.success();
    }

    public static final class Errors {

        public static Error orderStatusTransitionForbidden(OrderStatus current, OrderStatus transitioned) {
            return Error.of("order.forbidden.status.transition",
                    "Can't change order status from " + current + " to " + transitioned);
        }
    }

}
