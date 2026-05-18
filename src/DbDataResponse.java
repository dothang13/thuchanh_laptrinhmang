import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DbDataResponse implements Serializable {
    private final boolean success;
    private final String message;
    private final List<ExamOfficer> officers;
    private final List<Integer> rooms;

    public DbDataResponse(boolean success, String message, List<ExamOfficer> officers, List<Integer> rooms) {
        this.success = success;
        this.message = message;
        this.officers = new ArrayList<>(officers);
        this.rooms = new ArrayList<>(rooms);
    }

    public static DbDataResponse error(String message) {
        return new DbDataResponse(false, message, new ArrayList<>(), new ArrayList<>());
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<ExamOfficer> getOfficers() {
        return officers;
    }

    public List<Integer> getRooms() {
        return rooms;
    }
}
