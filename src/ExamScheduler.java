import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ExamScheduler {
    private final Random random = new Random();

    public ScheduleResponse schedule(ScheduleRequest request) {
        int roomCount = request.getRoomCount();
        int officerCount = request.getOfficerCount();
        List<ExamOfficer> officers = normalize(request.getOfficers(), officerCount);

        if (roomCount <= 0 || officerCount <= 0) {
            return ScheduleResponse.error("So phong va so giam thi phai lon hon 0.");
        }
        if (officerCount < roomCount * 2 || officers.size() < roomCount * 2) {
            return ScheduleResponse.error("Khong du can bo, yeu cau nhap lai.");
        }
        List<Integer> rooms = normalizeRooms(request);

        List<Assignment> best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int attempt = 0; attempt < 5000; attempt++) {
            List<ExamOfficer> pool = new ArrayList<>(officers);
            Collections.shuffle(pool, random);
            List<Assignment> current = new ArrayList<>();
            Set<String> usedPairs = new HashSet<>();
            int score = 0;

            for (int roomIndex = 0; roomIndex < roomCount; roomIndex++) {
                int room = rooms.get(roomIndex);
                PairChoice choice = choosePair(pool, room, usedPairs);
                if (choice == null) {
                    score += 1000;
                    break;
                }
                current.add(Assignment.room(room, choice.first, choice.second));
                pool.remove(choice.first);
                pool.remove(choice.second);
                usedPairs.add(pairKey(choice.first, choice.second));
                score += choice.penalty;
            }

            if (current.size() == roomCount && score < bestScore) {
                best = current;
                bestScore = score;
                if (score == 0) {
                    break;
                }
            }
        }

        if (best == null) {
            return ScheduleResponse.error("Khong tim duoc cach sap xep phu hop, vui long tang so giam thi hoac doi danh sach.");
        }

        Set<String> assignedIds = new HashSet<>();
        for (Assignment assignment : best) {
            assignedIds.add(keyOf(assignment.getOfficer1()));
            assignedIds.add(keyOf(assignment.getOfficer2()));
        }

        List<ExamOfficer> corridorOfficers = new ArrayList<>();
        for (ExamOfficer officer : officers) {
            if (!assignedIds.contains(keyOf(officer))) {
                corridorOfficers.add(officer);
            }
        }

        List<Assignment> corridorAssignments = assignCorridor(corridorOfficers, rooms);
        String message = bestScore == 0
                ? "Sap xep thanh cong."
                : "Sap xep thanh cong, co mot so giam thi khong the tranh phong cu do rang buoc qua chat.";
        return new ScheduleResponse(true, message, best, corridorAssignments);
    }

    private PairChoice choosePair(List<ExamOfficer> pool, int room, Set<String> usedPairs) {
        if (pool.size() < 2) return null;

        // Try random sampling to find a 0-penalty pair fast without full N^2 scan
        for (int k = 0; k < 50; k++) {
            int i = random.nextInt(pool.size());
            int j = random.nextInt(pool.size());
            if (i == j) continue;
            ExamOfficer a = pool.get(i);
            ExamOfficer b = pool.get(j);
            if (usedPairs.contains(pairKey(a, b))) continue;
            
            if (oldRoomPenalty(a, room) + oldRoomPenalty(b, room) == 0) {
                return new PairChoice(a, b, 0);
            }
        }

        ExamOfficer bestA = null;
        ExamOfficer bestB = null;
        int bestPenalty = Integer.MAX_VALUE;
        int tieCount = 0;

        for (int i = 0; i < pool.size(); i++) {
            ExamOfficer a = pool.get(i);
            int penaltyA = oldRoomPenalty(a, room);
            for (int j = i + 1; j < pool.size(); j++) {
                ExamOfficer b = pool.get(j);
                if (usedPairs.contains(pairKey(a, b))) continue;
                
                int penalty = penaltyA + oldRoomPenalty(b, room);
                
                if (penalty < bestPenalty) {
                    bestPenalty = penalty;
                    bestA = a;
                    bestB = b;
                    tieCount = 1;
                } else if (penalty == bestPenalty) {
                    tieCount++;
                    if (random.nextInt(tieCount) == 0) {
                        bestA = a;
                        bestB = b;
                    }
                }
            }
        }
        return bestA != null ? new PairChoice(bestA, bestB, bestPenalty) : null;
    }

    private List<Assignment> assignCorridor(List<ExamOfficer> officers, List<Integer> rooms) {
        List<Assignment> result = new ArrayList<>();
        if (officers.isEmpty()) {
            return result;
        }
        int roomCount = rooms.size();
        int n = officers.size();
        for (int i = 0; i < n; i++) {
            int startIndex = (i * roomCount) / n;
            int endIndex = ((i + 1) * roomCount - 1) / n;
            
            // Dam bao nam trong gioi han mang an toan
            startIndex = Math.max(0, Math.min(startIndex, roomCount - 1));
            endIndex = Math.max(0, Math.min(endIndex, roomCount - 1));
            
            int fromRoom = rooms.get(startIndex);
            int toRoom = rooms.get(endIndex);
            result.add(Assignment.corridor(officers.get(i), fromRoom, toRoom));
        }
        return result;
    }

    private List<Integer> normalizeRooms(ScheduleRequest request) {
        List<Integer> rooms = new ArrayList<>();
        for (Integer room : request.getRoomNumbers()) {
            if (room != null) {
                rooms.add(room);
            }
            if (rooms.size() == request.getRoomCount()) {
                break;
            }
        }
        for (int room = rooms.size() + 1; rooms.size() < request.getRoomCount(); room++) {
            rooms.add(room);
        }
        return rooms;
    }

    private List<ExamOfficer> normalize(List<ExamOfficer> officers, int officerCount) {
        Set<String> seen = new LinkedHashSet<>();
        List<ExamOfficer> result = new ArrayList<>();
        for (ExamOfficer officer : officers) {
            String key = keyOf(officer);
            if (!key.isBlank() && seen.add(key)) {
                result.add(officer);
            }
        }
        // Shuffle uniquely parsed officers to prevent biasing top rows of the original file
        Collections.shuffle(result, random);
        if (result.size() > officerCount) {
            return new ArrayList<>(result.subList(0, officerCount));
        }
        return result;
    }

    private int oldRoomPenalty(ExamOfficer officer, int room) {
        String oldRoom = officer.getOldRoom().replaceAll("[^0-9]", "");
        if (oldRoom.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(oldRoom) == room ? 1 : 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String pairKey(ExamOfficer a, ExamOfficer b) {
        String left = keyOf(a);
        String right = keyOf(b);
        return left.compareTo(right) <= 0 ? left + "|" + right : right + "|" + left;
    }

    private String keyOf(ExamOfficer officer) {
        String id = officer.getId().isBlank() ? "NO_ID" : officer.getId();
        return cleanKey(id) + "_" + cleanKey(officer.getBirthDate());
    }

    private String cleanKey(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static class PairChoice {
        private final ExamOfficer first;
        private final ExamOfficer second;
        private final int penalty;

        private PairChoice(ExamOfficer first, ExamOfficer second, int penalty) {
            this.first = first;
            this.second = second;
            this.penalty = penalty;
        }
    }
}
