package microarch.delivery.core.domain.model.courier;

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "couriers")
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
public class Courier extends Aggregate<UUID> {

    public static final Volume defaultCourierMaxVolume = Volume.create(20).getValueOrThrow();

    @Column(name = "name")
    @Getter
    private String name;

    @Embedded
    @Getter
    private Location location;

    @Embedded
    @Getter
    private Volume maxVolume;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "courier_id")
    private List<Assignment> assignments = new ArrayList<>();

    private Courier(String name, Location location, Volume maxVolume, List<Assignment> assignments) {
        super(UUID.randomUUID());
        this.name = name;
        this.location = location;
        this.maxVolume = maxVolume;
        this.assignments = assignments;
    }

    public List<Assignment> getAssignments() {
        return List.copyOf(assignments);
    }

    public static Result<Courier, Error> create(String name, Location location) {
        var err = Guard.combine(Guard.againstNullOrEmpty(name, "name"), Guard.againstNull(location, "location"));

        if (err != null) {
            return Result.failure(err);
        }

        return Result.success(new Courier(name, location, defaultCourierMaxVolume, new ArrayList<>()));
    }

    public UnitResult<Error> isCanTake(Volume volume) {
        var nullErr = Guard.againstNull(volume, "volume");
        if (nullErr != null) {
            return UnitResult.failure(nullErr);
        }

        if (this.getTotalVolumeWith(volume).isGreaterThan(this.getMaxVolume())) {
            return UnitResult.failure(Errors.courierIsFull());
        }

        return UnitResult.success();
    }

    public UnitResult<Error> take(UUID orderId, Volume volume, Location orderLocation) {
        var canTake = this.isCanTake(volume);
        if (canTake.isFailure()) {
            return canTake;
        }

        var newAssignment = Assignment.create(orderId, volume, orderLocation);
        if (newAssignment.isFailure()) {
            return UnitResult.failure(newAssignment.getError());
        }

        this.assignments.add(newAssignment.getValue());

        return UnitResult.success();
    }

    public UnitResult<Error> complete(UUID orderId) {
        var nullErr = Guard.againstNull(orderId, "order_id");
        if (nullErr != null) {
            return UnitResult.failure(nullErr);
        }

        var assignment = this.assignments.stream().filter(a -> a.getOrderId().equals(orderId)).findFirst();

        if (assignment.isEmpty()) {
            return UnitResult.failure(Errors.assignmentForOrderNotFound(orderId));
        }

        return assignment.get().complete(this.location);
    }

    public UnitResult<Error> move(Location newLocation) {
        var nullErr = Guard.againstNull(newLocation, "new_location");
        if (nullErr != null) {
            return UnitResult.failure(nullErr);
        }

        if (this.location.calculateDistance(newLocation) > 1) {
            return UnitResult.failure(Errors.locationIsTooFar());
        }

        this.location = newLocation;

        return UnitResult.success();
    }

    private Volume getTotalVolumeWith(Volume extra) {
        return this.assignments.stream().filter(assignment -> !assignment.getStatus().isCompleted())
                .map(Assignment::getVolume).reduce(extra, Volume::add);
    }

    public static final class Errors {

        public static Error courierIsFull() {
            return Error.of("courier.is.full", "Courier is full.");
        }

        public static Error locationIsTooFar() {
            return Error.of("location.is.too.far", "New location for courier is too far.");
        }

        public static Error assignmentForOrderNotFound(UUID orderId) {
            return Error.of("assignment.not.found", "Assignment for order with id " + orderId + " not found.");
        }
    }

}
