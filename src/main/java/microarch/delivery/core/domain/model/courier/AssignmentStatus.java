package microarch.delivery.core.domain.model.courier;

public enum AssignmentStatus {
    ASSIGNED, COMPLETED;

    public boolean isCompleted() {
        return this == COMPLETED;
    }
}
