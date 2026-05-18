import java.io.Serializable;

public class Assignment implements Serializable {
    private final int roomNumber;
    private final ExamOfficer officer1;
    private final ExamOfficer officer2;
    private final boolean corridor;
    private final int fromRoom;
    private final int toRoom;

    private Assignment(int roomNumber, ExamOfficer officer1, ExamOfficer officer2,
                       boolean corridor, int fromRoom, int toRoom) {
        this.roomNumber = roomNumber;
        this.officer1 = officer1;
        this.officer2 = officer2;
        this.corridor = corridor;
        this.fromRoom = fromRoom;
        this.toRoom = toRoom;
    }

    public static Assignment room(int roomNumber, ExamOfficer officer1, ExamOfficer officer2) {
        return new Assignment(roomNumber, officer1, officer2, false, 0, 0);
    }

    public static Assignment corridor(ExamOfficer officer, int fromRoom, int toRoom) {
        return new Assignment(0, officer, null, true, fromRoom, toRoom);
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public ExamOfficer getOfficer1() {
        return officer1;
    }

    public ExamOfficer getOfficer2() {
        return officer2;
    }

    public boolean isCorridor() {
        return corridor;
    }

    public int getFromRoom() {
        return fromRoom;
    }

    public int getToRoom() {
        return toRoom;
    }
}
