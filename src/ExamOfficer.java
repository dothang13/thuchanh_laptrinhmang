import java.io.Serializable;

public class ExamOfficer implements Serializable {
    private final String id;
    private final String name;
    private final String birthDate;
    private final String unit;
    private final String oldRoom;

    public ExamOfficer(String id, String name, String unit, String oldRoom) {
        this(id, name, "", unit, oldRoom);
    }

    public ExamOfficer(String id, String name, String birthDate, String unit, String oldRoom) {
        this.id = clean(id);
        this.name = clean(name);
        this.birthDate = clean(birthDate);
        this.unit = clean(unit);
        this.oldRoom = clean(oldRoom);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getUnit() {
        return unit;
    }

    public String getOldRoom() {
        return oldRoom;
    }

    public String displayName() {
        if (!id.isBlank() && !name.isBlank()) {
            String date = birthDate.isBlank() ? "" : " - " + birthDate;
            String school = unit.isBlank() ? "" : " - " + unit;
            return id + " - " + name + date + school;
        }
        return name.isBlank() ? id : name;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
