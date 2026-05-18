import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ExamClient extends JFrame {
    private static final int PORT = 5000;

    private final JTextField serverIpField = new JTextField("localhost", 15);
    private final JSpinner roomSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 500, 1));
    private final JSpinner officerSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 2000, 1));
    private final JTextField fileField = new JTextField();
    private final DefaultTableModel officerModel = new DefaultTableModel(new Object[]{"Mã CB", "Họ tên", "Ngày sinh", "Đơn vị", "Phòng cũ"}, 0);
    private final DefaultTableModel resultModel = new DefaultTableModel(new Object[]{"Loại", "Phòng/Tuyến", "Cán bộ 1", "Cán bộ 2/Ghi chú"}, 0);
    private final JTable officerTable = new JTable(officerModel);
    private final JTable resultTable = new JTable(resultModel);
    private final List<ExamOfficer> officers = new ArrayList<>();
    private final List<Integer> roomsFromDatabase = new ArrayList<>();
    private final JLabel connectionLabel = new JLabel("Trạng thái: Chưa kết nối");
    private ScheduleResponse lastResponse;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ExamClient().setVisible(true));
    }

    public ExamClient() {
        super("Sắp xếp cán bộ coi thi");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1120, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        add(header(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);
        add(actions(), BorderLayout.SOUTH);
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 64, 175));
        JLabel title = new JLabel("  PHẦN MỀM SẮP XẾP CÁN BỘ COI THI");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 22));
        panel.add(title, BorderLayout.CENTER);
        return panel;
    }

    private JPanel center() {
        JPanel wrapper = new JPanel(new GridLayout(1, 2, 10, 10));
        wrapper.add(leftPanel());
        wrapper.add(rightPanel());
        return wrapper;
    }

    private JPanel leftPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridLayout(4, 1, 6, 6));

        JPanel row0 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row0.add(new JLabel("IP Server:"));
        row0.add(serverIpField);
        row0.add(new JLabel("(Nhập IPv4 của máy chủ nếu chạy qua LAN)"));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("Số phòng thi:"));
        row1.add(roomSpinner);
        row1.add(new JLabel("Số giám thị nhập vào:"));
        row1.add(officerSpinner);

        JPanel row2 = new JPanel(new BorderLayout(5, 5));
        JButton choose = new JButton("Chọn file Excel");
        JButton loadDatabase = new JButton("Tải từ CSDL");
        choose.addActionListener(e -> chooseExcel());
        loadDatabase.addActionListener(e -> loadFromDatabase());
        fileField.setEditable(false);
        row2.add(choose, BorderLayout.WEST);
        row2.add(loadDatabase, BorderLayout.EAST);
        row2.add(fileField, BorderLayout.CENTER);

        JLabel hint = new JLabel("Danh sách: cột A mã, cột B họ tên, cột C đơn vị, cột D phòng cũ.");
        form.add(row0);
        form.add(row1);
        form.add(row2);
        form.add(hint);

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(officerTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel rightPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JLabel label = new JLabel("Kết quả phân công");
        label.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(label, BorderLayout.NORTH);
        panel.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel actions() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectionLabel.setForeground(new Color(100, 100, 100));
        statusPanel.add(connectionLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton reset = new JButton("Làm mới");
        JButton arrange = new JButton("Gửi server và sắp xếp");
        JButton export = new JButton("Xuất Excel");
        reset.addActionListener(e -> resetForm());
        arrange.addActionListener(e -> arrange());
        export.addActionListener(e -> exportExcel());
        buttonPanel.add(reset);
        buttonPanel.add(arrange);
        buttonPanel.add(export);

        panel.add(statusPanel, BorderLayout.WEST);
        panel.add(buttonPanel, BorderLayout.EAST);
        return panel;
    }

    private void chooseExcel() {
        JFileChooser chooser = new JFileChooser(new File(".."));
        chooser.setDialogTitle("Chọn danh sách cán bộ coi thi");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            List<ExamOfficer> loaded = new ExcelReader().read(file);
            officers.clear();
            roomsFromDatabase.clear();
            officers.addAll(loaded);
            officerModel.setRowCount(0);
            for (ExamOfficer officer : officers) {
                officerModel.addRow(new Object[]{officer.getId(), officer.getName(), officer.getBirthDate(), officer.getUnit(), officer.getOldRoom()});
            }
            officerSpinner.setValue(Math.max(1, officers.size()));
            fileField.setText(file.getAbsolutePath());
        } catch (Exception ex) {
            showError("Không đọc được file Excel: " + ex.getMessage());
        }
    }

    private void arrange() {
        int roomCount = (Integer) roomSpinner.getValue();
        int officerCount = (Integer) officerSpinner.getValue();
        if (officerCount < roomCount * 2) {
            showError("Không đủ cán bộ yêu cầu nhập lại.");
            return;
        }
        if (officers.size() < officerCount) {
            showError("Danh sách Excel chỉ có " + officers.size() + " cán bộ, không đủ so với số đã nhập.");
            return;
        }

        String host = serverIpField.getText().trim();
        if (host.isEmpty()) host = "localhost";

        try (Socket socket = new Socket(host, PORT);
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
            String connStr = String.format("Trạng thái: Đã kết nối tới Server %s:%d (Local Port: %d)",
                    socket.getInetAddress().getHostAddress(), socket.getPort(), socket.getLocalPort());
            connectionLabel.setText(connStr);
            connectionLabel.setForeground(new Color(34, 139, 34));

            output.writeObject(new ScheduleRequest(roomCount, officerCount, officers, roomsFromDatabase));
            output.flush();
            Object object = input.readObject();
            if (!(object instanceof ScheduleResponse)) {
                showError("Server trả về dữ liệu không hợp lệ.");
                return;
            }
            lastResponse = (ScheduleResponse) object;
            if (!lastResponse.isSuccess()) {
                showError(lastResponse.getMessage());
                return;
            }
            renderResult();
            JOptionPane.showMessageDialog(this, lastResponse.getMessage(), "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Không kết nối được server. Hãy chạy ExamServer trước.\n" + ex.getMessage());
        }
    }

    private void loadFromDatabase() {
        String host = serverIpField.getText().trim();
        if (host.isEmpty()) host = "localhost";

        try (Socket socket = new Socket(host, PORT);
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
            String connStr = String.format("Trạng thái: Đã kết nối tới Server %s:%d (Local Port: %d)",
                    socket.getInetAddress().getHostAddress(), socket.getPort(), socket.getLocalPort());
            connectionLabel.setText(connStr);
            connectionLabel.setForeground(new Color(34, 139, 34));

            output.writeObject(new DbDataRequest());
            output.flush();
            Object object = input.readObject();
            if (!(object instanceof DbDataResponse)) {
                showError("Server trả về dữ liệu CSDL không hợp lệ.");
                return;
            }
            DbDataResponse response = (DbDataResponse) object;
            if (!response.isSuccess()) {
                showError(response.getMessage());
                return;
            }

            officers.clear();
            roomsFromDatabase.clear();
            officers.addAll(response.getOfficers());
            roomsFromDatabase.addAll(response.getRooms());
            officerModel.setRowCount(0);
            resultModel.setRowCount(0);
            for (ExamOfficer officer : officers) {
                officerModel.addRow(new Object[]{officer.getId(), officer.getName(), officer.getBirthDate(), officer.getUnit(), officer.getOldRoom()});
            }
            roomSpinner.setValue(Math.max(1, roomsFromDatabase.size()));
            officerSpinner.setValue(Math.max(1, officers.size()));
            fileField.setText("Đã tải từ MySQL: " + roomsFromDatabase.size() + " phòng, " + officers.size() + " cán bộ");
            lastResponse = null;
            JOptionPane.showMessageDialog(this, response.getMessage(), "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Không tải được dữ liệu từ server/MySQL.\n" + ex.getMessage());
        }
    }

    private void renderResult() {
        resultModel.setRowCount(0);
        for (Assignment assignment : lastResponse.getRoomAssignments()) {
            resultModel.addRow(new Object[]{
                    "Phòng thi",
                    "Phòng " + assignment.getRoomNumber(),
                    assignment.getOfficer1().displayName(),
                    assignment.getOfficer2().displayName()
            });
        }
        for (Assignment assignment : lastResponse.getCorridorAssignments()) {
            resultModel.addRow(new Object[]{
                    "Hành lang",
                    "Từ phòng " + assignment.getFromRoom() + " đến phòng " + assignment.getToRoom(),
                    assignment.getOfficer1().displayName(),
                    "Cán bộ giám sát"
            });
        }
    }

    private void exportExcel() {
        if (lastResponse == null || !lastResponse.isSuccess()) {
            showError("Chưa có kết quả để xuất.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Danh_sach_can_bo_coi_thi.xls"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".xls")) {
                file = new File(file.getParentFile(), file.getName() + ".xls");
            }
            new ExcelExporter().export(file, lastResponse);
            JOptionPane.showMessageDialog(this, "Đã xuất file: " + file.getAbsolutePath(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Không xuất được Excel: " + ex.getMessage());
        }
    }

    private void resetForm() {
        officers.clear();
        roomsFromDatabase.clear();
        officerModel.setRowCount(0);
        resultModel.setRowCount(0);
        roomSpinner.setValue(1);
        officerSpinner.setValue(1);
        fileField.setText("");
        connectionLabel.setText("Trạng thái: Chưa kết nối");
        connectionLabel.setForeground(new Color(100, 100, 100));
        lastResponse = null;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
