import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ExamServer {
    private static final int PORT = 5000;
    private final ExamScheduler scheduler = new ExamScheduler();
    private final DatabaseManager databaseManager = new DatabaseManager();

    public static void main(String[] args) {
        new ExamServer().start();
    }

    public void start() {
        System.out.println("Server sap xep coi thi dang chay tai cong " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handle(socket)).start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handle(Socket socket) {
        String clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        System.out.println("-> Client da ket noi tu: " + clientInfo);
        try (Socket client = socket;
             ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(client.getInputStream())) {
            Object object = input.readObject();
            if (object instanceof DbDataRequest) {
                System.out.println("   [" + clientInfo + "] Yeu cau tai du lieu tu CSDL.");
                output.writeObject(databaseManager.loadInputData());
                output.flush();
                return;
            }
            if (!(object instanceof ScheduleRequest)) {
                System.out.println("   [" + clientInfo + "] Du lieu client khong hop le.");
                output.writeObject(ScheduleResponse.error("Du lieu client khong hop le."));
                return;
            }
            ScheduleRequest request = (ScheduleRequest) object;
            System.out.println("   [" + clientInfo + "] Yeu cau sap xep " + request.getRoomCount() + " phong, " + request.getOfficerCount() + " can bo.");

            ScheduleResponse response = scheduler.schedule(request);
            if (response.isSuccess()) {
                databaseManager.save(request, response);
                System.out.println("   [" + clientInfo + "] Sap xep thanh cong. Da luu vao CSDL.");
            } else {
                System.out.println("   [" + clientInfo + "] Sap xep that bai: " + response.getMessage());
            }
            output.writeObject(response);
            output.flush();
        } catch (Exception ex) {
            System.out.println("   [" + clientInfo + "] Loi: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            System.out.println("<- Ngat ket noi voi client: " + clientInfo);
        }
    }
}
