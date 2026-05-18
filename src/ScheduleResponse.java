import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScheduleResponse implements Serializable {
    private final boolean success;
    private final String message;
    private final LocalDateTime createdAt;
    private final List<Assignment> roomAssignments;
    private final List<Assignment> corridorAssignments;
    private final String scheduleName;

    public ScheduleResponse(boolean success, String message, List<Assignment> roomAssignments,
                            List<Assignment> corridorAssignments) {
        this(success, message, roomAssignments, corridorAssignments, "Danh_sach_can_bo_coi_thi");
    }

    public ScheduleResponse(boolean success, String message, List<Assignment> roomAssignments,
                            List<Assignment> corridorAssignments, String scheduleName) {
        this.success = success;
        this.message = message;
        this.createdAt = LocalDateTime.now();
        this.roomAssignments = new ArrayList<>(roomAssignments);
        this.corridorAssignments = new ArrayList<>(corridorAssignments);
        this.scheduleName = scheduleName;
    }

    public static ScheduleResponse error(String message) {
        return new ScheduleResponse(false, message, Collections.emptyList(), Collections.emptyList(), "");
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Assignment> getRoomAssignments() {
        return roomAssignments;
    }

    public List<Assignment> getCorridorAssignments() {
        return corridorAssignments;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public ScheduleResponse withScheduleName(String scheduleName) {
        return new ScheduleResponse(this.success, this.message, this.roomAssignments, this.corridorAssignments, scheduleName);
    }
}
