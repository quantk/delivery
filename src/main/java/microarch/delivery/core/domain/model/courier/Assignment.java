package microarch.delivery.core.domain.model.courier;

import jakarta.persistence.*;
import libs.ddd.BaseEntity;
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
@Table(name = "assignments")
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
public class Assignment extends BaseEntity<UUID> {
    private static final int MAX_COMPLETE_DISTANCE = 1;

    @Column(name = "order_id")
    @Getter
    private UUID orderId;

    @Embedded
    @Getter
    private Volume volume;

    @Embedded
    @Getter
    private Location location;

    @Column(name = "status")
    @Getter
    @Enumerated(EnumType.STRING)
    private AssignmentStatus status;

    private Assignment(UUID orderId, Volume volume, Location location, AssignmentStatus status) {
        super(UUID.randomUUID());
        this.orderId = orderId;
        this.volume = volume;
        this.location = location;
        this.status = status;
    }

    public static Result<Assignment, Error> create(UUID orderId, Volume volume, Location location) {
        var err = Guard.combine(Guard.againstNullOrEmpty(orderId, "order_id"), Guard.againstNull(volume, "volume"),
                Guard.againstNull(location, "location"));

        if (err != null) {
            return Result.failure(err);
        }

        return Result.success(new Assignment(orderId, volume, location, AssignmentStatus.ASSIGNED));
    }

    UnitResult<Error> complete(Location courierLocation) {
        var err = Guard.againstNull(courierLocation, "courier_location");
        if (err != null) {
            return UnitResult.failure(err);
        }

        if (status == AssignmentStatus.COMPLETED) {
            return UnitResult.failure(Errors.assignmentAlreadyCompleted());
        }

        int distanceFromLocation = courierLocation.calculateDistance(location);
        if (distanceFromLocation > MAX_COMPLETE_DISTANCE) {
            return UnitResult.failure(Errors.courierTooFar(MAX_COMPLETE_DISTANCE, distanceFromLocation));
        }

        this.status = AssignmentStatus.COMPLETED;

        return UnitResult.success();
    }

    public static final class Errors {

        public static Error courierTooFar(int required, int actual) {
            return Error.of("assignment.courier.too.far", "Courier is too far from the order location: distance is "
                    + actual + ", must be at most " + required);
        }

        public static Error assignmentAlreadyCompleted() {
            return Error.of("assignment.already.completed", "Assignment is already completed");
        }
    }

}
