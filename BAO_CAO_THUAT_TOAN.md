# BÁO CÁO THUẬT TOÁN: HỆ THỐNG PHÂN CÔNG CÁN BỘ COI THI

## 1. Giới thiệu bài toán
Bài toán đặt ra là phân công các cán bộ coi thi vào các phòng thi sao cho đáp ứng được các ràng buộc khắt khe của quy chế thi:
- Mỗi phòng thi cần chính xác **2 giám thị**.
- Tránh tình trạng **2 giám thị đã từng coi thi chung** bị lặp lại (trùng cặp).
- Tránh tình trạng giám thị bị phân công vào **đúng phòng thi cũ** của mình (trùng phòng cũ).
- Số cán bộ dư ra (nếu có) sẽ được phân công làm **Giám thị hành lang**, phụ trách giám sát một dải phòng thi.

Vì số lượng tổ hợp phân công là rất lớn và có thể dẫn đến ngõ cụt (deadlock) nếu sử dụng thuật toán quay lui (Backtracking) đơn thuần, hệ thống áp dụng **Thuật toán Tham lam ngẫu nhiên nhiều lần thử (Randomized Greedy Search with Multiple Attempts)** kết hợp với **Hàm đánh giá hình phạt (Penalty Scoring)**.

---

## 2. Chi tiết Thuật toán

### 2.1. Tiền xử lý và Chuẩn hóa dữ liệu (Normalization)
Trước khi tiến hành phân công, danh sách cán bộ được chuẩn hóa:
- **Lọc trùng lặp:** Loại bỏ các bản ghi trùng lặp thông qua Khóa duy nhất (kết hợp Mã CB và Ngày sinh).
- **Trộn ngẫu nhiên (Shuffle):** Danh sách được trộn ngẫu nhiên để đảm bảo tính công bằng, tránh việc các giáo viên ở đầu danh sách luôn được ưu tiên phân công trước qua nhiều lần chạy.

```java
private List<ExamOfficer> normalize(List<ExamOfficer> officers, int officerCount) {
    Set<String> seen = new LinkedHashSet<>();
    List<ExamOfficer> result = new ArrayList<>();
    for (ExamOfficer officer : officers) {
        String key = keyOf(officer);
        if (!key.isBlank() && seen.add(key)) {
            result.add(officer);
        }
    }
    // Trộn ngẫu nhiên để đảm bảo công bằng
    Collections.shuffle(result, random);
    if (result.size() > officerCount) {
        return new ArrayList<>(result.subList(0, officerCount));
    }
    return result;
}
```

### 2.2. Vòng lặp tối ưu hóa (Monte Carlo / Multiple Attempts)
Thuật toán sẽ thử phân công lặp lại **5000 lần**. Trong mỗi lần thử:
- Hệ thống cố gắng phân công giám thị cho toàn bộ danh sách phòng thi.
- Một "điểm hình phạt" (`score`) được ghi nhận nếu có cán bộ vi phạm ràng buộc (ví dụ: bị xếp lại vào phòng cũ).
- Nếu tìm thấy một cấu hình có điểm hình phạt bằng `0` (hoàn hảo), vòng lặp dừng lại ngay lập tức.
- Nếu sau 5000 lần vẫn không có cấu hình hoàn hảo, thuật toán sẽ trả về cấu hình có điểm hình phạt thấp nhất (`bestScore`).

```java
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
        // ... Cập nhật score và xóa cán bộ khỏi pool
    }

    if (current.size() == roomCount && score < bestScore) {
        best = current;
        bestScore = score;
        if (score == 0) break; // Tìm được phương án hoàn hảo tuyệt đối
    }
}
```

### 2.3. Chiến lược Chọn Cặp (Heuristic Pair Selection)
Hàm `choosePair` chịu trách nhiệm tìm ra 2 giám thị phù hợp nhất cho 1 phòng thi. Hàm này hoạt động theo 2 giai đoạn để tối ưu hiệu suất:

1. **Lấy mẫu ngẫu nhiên nhanh (Fast Random Sampling):** Chọn ngẫu nhiên 50 cặp. Nếu tìm thấy một cặp không trùng nhau, không vi phạm cặp cũ và không vi phạm phòng cũ (Penalty = 0), chọn ngay cặp này. Điều này giúp giảm độ phức tạp thời gian từ $O(N^2)$ xuống $O(1)$ trong hầu hết các trường hợp.
2. **Tìm kiếm toàn bộ (Exhaustive Search):** Nếu lấy mẫu thất bại (danh sách còn ít người hoặc ràng buộc quá chặt), duyệt toàn bộ các cặp còn lại để tìm ra cặp có Penalty thấp nhất.

```java
private PairChoice choosePair(List<ExamOfficer> pool, int room, Set<String> usedPairs) {
    if (pool.size() < 2) return null;

    // 1. Random Sampling (Fast Path)
    for (int k = 0; k < 50; k++) {
        int i = random.nextInt(pool.size());
        int j = random.nextInt(pool.size());
        if (i == j) continue;
        ExamOfficer a = pool.get(i);
        ExamOfficer b = pool.get(j);
        
        if (!usedPairs.contains(pairKey(a, b)) && 
            oldRoomPenalty(a, room) + oldRoomPenalty(b, room) == 0) {
            return new PairChoice(a, b, 0); // Trả về ngay khi đạt điểm hoàn hảo
        }
    }

    // 2. Exhaustive Search (Duyệt toàn bộ để tìm cặp ít vi phạm nhất)
    ExamOfficer bestA = null, bestB = null;
    int bestPenalty = Integer.MAX_VALUE;
    int tieCount = 0;

    for (int i = 0; i < pool.size(); i++) {
        ExamOfficer a = pool.get(i);
        int penaltyA = oldRoomPenalty(a, room);
        
        for (int j = i + 1; j < pool.size(); j++) {
            ExamOfficer b = pool.get(j);
            if (usedPairs.contains(pairKey(a, b))) continue; // Ràng buộc bắt buộc (Hard constraint)
            
            int penalty = penaltyA + oldRoomPenalty(b, room); // Ràng buộc linh hoạt (Soft constraint)
            if (penalty < bestPenalty) {
                bestPenalty = penalty;
                bestA = a; bestB = b;
                tieCount = 1;
            } else if (penalty == bestPenalty) {
                // Phá vỡ thế hòa ngẫu nhiên (Reservoir Sampling)
                tieCount++;
                if (random.nextInt(tieCount) == 0) {
                    bestA = a; bestB = b;
                }
            }
        }
    }
    return bestA != null ? new PairChoice(bestA, bestB, bestPenalty) : null;
}
```

### 2.4. Phân công Giám sát Hành lang
Những cán bộ không được phân công vào phòng thi sẽ được gom lại và chia đều để quản lý các "dải phòng". Thuật toán chia đều danh sách phòng thi dựa trên tỷ lệ mảng (`startIndex` và `endIndex`).

```java
private List<Assignment> assignCorridor(List<ExamOfficer> officers, List<Integer> rooms) {
    List<Assignment> result = new ArrayList<>();
    int roomCount = rooms.size();
    int n = officers.size();
    
    for (int i = 0; i < n; i++) {
        // Chia đều danh sách phòng thi thành các khoảng cho từng cán bộ
        int startIndex = (i * roomCount) / n;
        int endIndex = ((i + 1) * roomCount - 1) / n;
        
        int fromRoom = rooms.get(startIndex);
        int toRoom = rooms.get(endIndex);
        result.add(Assignment.corridor(officers.get(i), fromRoom, toRoom));
    }
    return result;
}
```

---

## 3. Đánh giá Thuật toán
- **Thời gian chạy:** Rất nhanh (chỉ mất vài mili-giây cho quy mô dưới 500 cán bộ) do tối ưu hóa bước "Fast Random Sampling".
- **Khả năng chống kẹt (Deadlock):** So với thuật toán Backtracking thông thường rất dễ bị treo máy khi không có lời giải hoàn hảo, phương pháp **Tham lam có hình phạt (Penalty-based Greedy)** luôn trả về một lời giải "tốt nhất có thể" (best effort), cho phép hệ thống luôn có kết quả ngay cả khi ràng buộc quá chặt.
- **Tính ngẫu nhiên (Stochastic):** Mỗi lần bấm xếp lại, hệ thống sẽ sinh ra một sơ đồ thi khác nhau, giúp phòng ngừa gian lận.
