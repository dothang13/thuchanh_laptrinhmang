import java.util.List;

public class ExcelData {
    private final List<ExamOfficer> officers;
    private final List<Integer> rooms;

    public ExcelData(List<ExamOfficer> officers, List<Integer> rooms) {
        this.officers = officers;
        this.rooms = rooms;
    }

    public List<ExamOfficer> getOfficers() {
        return officers;
    }

    public List<Integer> getRooms() {
        return rooms;
    }
}
