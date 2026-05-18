import java.io.File;
import java.util.List;

public class SmokeTest {
    public static void main(String[] args) throws Exception {
        File excel = new File("..", "Danh sach can bo coi thi.xlsx");
        ExcelData data = new ExcelReader().read(excel);
        List<ExamOfficer> officers = data.getOfficers();
        System.out.println("So can bo doc duoc: " + officers.size());
        ScheduleResponse response = new ExamScheduler().schedule(new ScheduleRequest(100, 220, officers));
        System.out.println(response.getMessage());
        System.out.println("So phong thi: " + response.getRoomAssignments().size());
        System.out.println("So can bo hanh lang: " + response.getCorridorAssignments().size());
        if (!response.getCorridorAssignments().isEmpty()) {
            Assignment first = response.getCorridorAssignments().get(0);
            Assignment last = response.getCorridorAssignments().get(response.getCorridorAssignments().size() - 1);
            System.out.println("Hanh lang dau: phong " + first.getFromRoom() + " den " + first.getToRoom());
            System.out.println("Hanh lang cuoi: phong " + last.getFromRoom() + " den " + last.getToRoom());
        }
    }
}
