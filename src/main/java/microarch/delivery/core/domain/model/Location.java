package microarch.delivery.core.domain.model;

import jakarta.persistence.Embeddable;
import libs.ddd.ValueObject;
import libs.errs.Error;
import libs.errs.Guard;
import libs.errs.Result;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Embeddable
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Location extends ValueObject<Location> {
    private static final int MIN_COORDINATE = 1;
    private static final int MAX_COORDINATE = 10;

    private final int x;
    private final int y;

    public static Result<Location, Error> create(int x, int y) {
        var err = Guard.combine(Guard.againstLessThan(x, MIN_COORDINATE, "min_x"),
                Guard.againstLessThan(y, MIN_COORDINATE, "min_y"), Guard.againstGreaterThan(x, MAX_COORDINATE, "max_x"),
                Guard.againstGreaterThan(y, MAX_COORDINATE, "max_y"));

        if (err != null) {
            return Result.failure(err);
        }

        return Result.success(new Location(x, y));
    }

    public int calculateDistance(Location another) {
        return Math.abs(this.x - another.x) + Math.abs(this.y - another.y);
    }

    @Override
    protected Iterable<Object> equalityComponents() {
        return List.of(this.x, this.y);
    }
}
