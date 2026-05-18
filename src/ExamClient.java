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
import javax.swing.table.JTableHeader;
import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
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
        SwingUtilities.invokeLater(() -> {
            try {
                for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                // Ignore and use default
            }
            new ExamClient().setVisible(true);
        });
    }

    public ExamClient() {
        super("Phần mềm quản lý coi thi");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));
        
        // Setup table appearance
        officerTable.setRowHeight(25);
        resultTable.setRowHeight(25);
        officerTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        resultTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        officerTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        resultTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
        mainContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainContainer.add(header(), BorderLayout.NORTH);
        mainContainer.add(center(), BorderLayout.CENTER);
        mainContainer.add(actions(), BorderLayout.SOUTH);
        
        setContentPane(mainContainer);
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(41, 128, 185));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel title = new JLabel("HỆ THỐNG PHÂN CÔNG CÁN BỘ COI THI");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setHorizontalAlignment(JLabel.CENTER);
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
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Thông tin đầu vào",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 15), new Color(50, 50, 50)
        ));

        JPanel form = new JPanel(new GridLayout(4, 1, 8, 8));
        form.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel row0 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row0.add(new JLabel("IP Server:"));
        row0.add(serverIpField);
        JLabel lanHint = new JLabel("(Nhập IPv4 nếu chạy LAN)");
        lanHint.setForeground(Color.GRAY);
        lanHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        row0.add(lanHint);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("Phòng thi:"));
        row1.add(roomSpinner);
        row1.add(new JLabel("   Cán bộ:"));
        row1.add(officerSpinner);

        JPanel row2 = new JPanel(new BorderLayout(5, 5));
        JButton choose = createStyledButton("Chọn file Excel", new Color(46, 204, 113));
        JButton loadDatabase = createStyledButton("Tải từ CSDL", new Color(52, 152, 219));
        choose.addActionListener(e -> chooseExcel());
        loadDatabase.addActionListener(e -> loadFromDatabase());
        fileField.setEditable(false);
        row2.add(choose, BorderLayout.WEST);
        row2.add(loadDatabase, BorderLayout.EAST);
        row2.add(fileField, BorderLayout.CENTER);

        JLabel hint = new JLabel("Cấu trúc Excel: Cột A(Mã), B(Họ tên), C(Đơn vị), D(Phòng cũ)");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(new Color(100, 100, 100));

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
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Kết quả phân công",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 15), new Color(50, 50, 50)
        ));
        panel.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel actions() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 0, 0, 0));
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        connectionLabel.setForeground(new Color(150, 150, 150));
        statusPanel.add(connectionLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton reset = createStyledButton("Làm mới", new Color(149, 165, 166));
        JButton arrange = createStyledButton("Gửi server và sắp xếp", new Color(155, 89, 182));
        JButton export = createStyledButton("Xuất Excel", new Color(230, 126, 34));
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

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker()),
                new EmptyBorder(8, 15, 8, 15)
        ));
        return btn;
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
        String defaultName = lastResponse.getScheduleName() != null && !lastResponse.getScheduleName().isEmpty() 
            ? lastResponse.getScheduleName() + ".xls" 
            : "Danh_sach_can_bo_coi_thi.xls";
        chooser.setSelectedFile(new File(defaultName));
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
