import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DATABASE = "QuanLyCoiThi";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "123@Thang";

    public DbDataResponse loadInputData() {
        try (Connection connection = getConnection()) {
            List<ExamOfficer> officers = loadOfficers(connection);
            List<Integer> rooms = loadRooms(connection);
            if (officers.isEmpty()) {
                return DbDataResponse.error("Bang danh_sach_gv chua co du lieu.");
            }
            if (rooms.isEmpty()) {
                return DbDataResponse.error("Bang danh_sach_phong chua co du lieu.");
            }
            return new DbDataResponse(true,
                    "Da tai " + officers.size() + " can bo va " + rooms.size() + " phong thi tu MySQL.",
                    officers, rooms);
        } catch (SQLException ex) {
            return DbDataResponse.error("Khong doc duoc MySQL: " + ex.getMessage());
        }
    }

    public ScheduleResponse save(ScheduleRequest request, ScheduleResponse response) {
        try (Connection connection = getConnection()) {
            createTables(connection);
            String scheduleName = getNextScheduleName(connection);
            ScheduleResponse namedResponse = response.withScheduleName(scheduleName);
            long scheduleId = insertSchedule(connection, request, namedResponse);
            insertRooms(connection, scheduleId, namedResponse);
            insertCorridors(connection, scheduleId, namedResponse);
            return namedResponse;
        } catch (SQLException ex) {
            System.out.println("Khong the luu MySQL: " + ex.getMessage());
            return response;
        }
    }

    private String getNextScheduleName(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT ten_danh_sach FROM lich_coi_thi WHERE ten_danh_sach LIKE 'DANH_SACH_CAN_BO_COI_THI_CA_%' ORDER BY id DESC")) {
            int maxCa = 0;
            while (rs.next()) {
                String name = rs.getString(1);
                if (name != null) {
                    try {
                        int ca = Integer.parseInt(name.substring("DANH_SACH_CAN_BO_COI_THI_CA_".length()));
                        if (ca > maxCa) maxCa = ca;
                    } catch (Exception ignored) {}
                }
            }
            return "DANH_SACH_CAN_BO_COI_THI_CA_" + (maxCa + 1);
        }
    }

    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://" + value("DB_HOST", DEFAULT_HOST) + ":"
                + value("DB_PORT", DEFAULT_PORT) + "/" + value("DB_NAME", DEFAULT_DATABASE)
                + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh";
        return DriverManager.getConnection(url, value("DB_USER", DEFAULT_USER), value("DB_PASSWORD", DEFAULT_PASSWORD));
    }

    private void createTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lich_coi_thi ("
                    + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                    + "so_phong INT NOT NULL,"
                    + "so_giam_thi INT NOT NULL,"
                    + "ghi_chu VARCHAR(255),"
                    + "ten_danh_sach VARCHAR(255),"
                    + "ngay_tao TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS phan_cong_phong ("
                    + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                    + "lich_id BIGINT NOT NULL,"
                    + "phong INT NOT NULL,"
                    + "ma_gt1 VARCHAR(100), ten_gt1 VARCHAR(255), ngay_sinh_gt1 VARCHAR(20), don_vi_gt1 VARCHAR(255),"
                    + "ma_gt2 VARCHAR(100), ten_gt2 VARCHAR(255), ngay_sinh_gt2 VARCHAR(20), don_vi_gt2 VARCHAR(255),"
                    + "FOREIGN KEY (lich_id) REFERENCES lich_coi_thi(id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS phan_cong_hanh_lang ("
                    + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                    + "lich_id BIGINT NOT NULL,"
                    + "ma_gt VARCHAR(100), ten_gt VARCHAR(255), ngay_sinh VARCHAR(20), don_vi VARCHAR(255),"
                    + "tu_phong INT, den_phong INT,"
                    + "FOREIGN KEY (lich_id) REFERENCES lich_coi_thi(id))");
            addColumnIfMissing(statement, "phan_cong_phong", "ngay_sinh_gt1", "VARCHAR(20)");
            addColumnIfMissing(statement, "phan_cong_phong", "ngay_sinh_gt2", "VARCHAR(20)");
            addColumnIfMissing(statement, "phan_cong_hanh_lang", "ngay_sinh", "VARCHAR(20)");
            addColumnIfMissing(statement, "lich_coi_thi", "ten_danh_sach", "VARCHAR(255)");
        }
    }

    private void addColumnIfMissing(Statement statement, String table, String column, String definition) {
        try {
            statement.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException ignored) {
        }
    }

    private List<ExamOfficer> loadOfficers(Connection connection) throws SQLException {
        java.util.Map<String, ExamOfficer> officerMap = new java.util.LinkedHashMap<>();
        String sql = "SELECT ma_gv, ho_ten, ngay_sinh, don_vi_cong_tac FROM danh_sach_gv ORDER BY ma_gv, ho_ten, ngay_sinh, don_vi_cong_tac";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("ma_gv");
                String name = rs.getString("ho_ten");
                String birthDate = formatDate(rs.getDate("ngay_sinh"));
                String unit = rs.getString("don_vi_cong_tac");
                
                if (id == null || id.isBlank()) continue;
                String key = id + "_" + birthDate;
                if (officerMap.containsKey(key)) {
                    ExamOfficer existing = officerMap.get(key);
                    if (unit != null && !unit.isBlank() && !existing.getUnit().contains(unit)) {
                        String newUnit = existing.getUnit() + " - " + unit;
                        officerMap.put(key, new ExamOfficer(id, name, birthDate, newUnit, ""));
                    }
                } else {
                    officerMap.put(key, new ExamOfficer(id, name, birthDate, unit, ""));
                }
            }
        }
        return new ArrayList<>(officerMap.values());
    }

    private List<Integer> loadRooms(Connection connection) throws SQLException {
        List<Integer> rooms = new ArrayList<>();
        String sql = "SELECT phong_thi FROM danh_sach_phong ORDER BY phong_thi";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rooms.add(rs.getInt("phong_thi"));
            }
        }
        return rooms;
    }

    private long insertSchedule(Connection connection, ScheduleRequest request, ScheduleResponse response) throws SQLException {
        String sql = "INSERT INTO lich_coi_thi(so_phong, so_giam_thi, ghi_chu, ten_danh_sach) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, request.getRoomCount());
            ps.setInt(2, request.getOfficerCount());
            ps.setString(3, response.getMessage());
            ps.setString(4, response.getScheduleName());
            ps.executeUpdate();
            try (java.sql.ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private void insertRooms(Connection connection, long scheduleId, ScheduleResponse response) throws SQLException {
        String sql = "INSERT INTO phan_cong_phong(lich_id, phong, ma_gt1, ten_gt1, ngay_sinh_gt1, don_vi_gt1, ma_gt2, ten_gt2, ngay_sinh_gt2, don_vi_gt2)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Assignment assignment : response.getRoomAssignments()) {
                ExamOfficer a = assignment.getOfficer1();
                ExamOfficer b = assignment.getOfficer2();
                ps.setLong(1, scheduleId);
                ps.setInt(2, assignment.getRoomNumber());
                ps.setString(3, a.getId());
                ps.setString(4, a.getName());
                ps.setString(5, a.getBirthDate());
                ps.setString(6, a.getUnit());
                ps.setString(7, b.getId());
                ps.setString(8, b.getName());
                ps.setString(9, b.getBirthDate());
                ps.setString(10, b.getUnit());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertCorridors(Connection connection, long scheduleId, ScheduleResponse response) throws SQLException {
        String sql = "INSERT INTO phan_cong_hanh_lang(lich_id, ma_gt, ten_gt, ngay_sinh, don_vi, tu_phong, den_phong)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Assignment assignment : response.getCorridorAssignments()) {
                ExamOfficer officer = assignment.getOfficer1();
                ps.setLong(1, scheduleId);
                ps.setString(2, officer.getId());
                ps.setString(3, officer.getName());
                ps.setString(4, officer.getBirthDate());
                ps.setString(5, officer.getUnit());
                ps.setInt(6, assignment.getFromRoom());
                ps.setInt(7, assignment.getToRoom());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static String value(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatDate(java.sql.Date date) {
        return date == null ? "" : date.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
