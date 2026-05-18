import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScheduleRequest implements Serializable {
    private final int roomCount;
    private final int officerCount;
    private final List<ExamOfficer> officers;
    private final List<Integer> roomNumbers;

    public ScheduleRequest(int roomCount, int officerCount, List<ExamOfficer> officers) {
        this(roomCount, officerCount, officers, Collections.emptyList());
    }

    public ScheduleRequest(int roomCount, int officerCount, List<ExamOfficer> officers, List<Integer> roomNumbers) {
        this.roomCount = roomCount;
        this.officerCount = officerCount;
        this.officers = officers;
        this.roomNumbers = new ArrayList<>(roomNumbers);
    }

    public int getRoomCount() {
        return roomCount;
    }

    public int getOfficerCount() {
        return officerCount;
    }

    public List<ExamOfficer> getOfficers() {
        return officers;
    }

    public List<Integer> getRoomNumbers() {
        return roomNumbers;
    }
}
