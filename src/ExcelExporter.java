import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

public class ExcelExporter {
    public void export(File file, ScheduleResponse response) throws Exception {
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("<html><head><meta charset='UTF-8'></head><body>");
            
            String th = "style='border:1pt solid black;font-weight:bold;vertical-align:middle;text-align:center'";
            String tdCenter = "style='border:1pt solid black;vertical-align:middle;text-align:center'";
            String tdLeft = "style='border:1pt solid black;vertical-align:middle;text-align:left'";
            
            // Unified table to guarantee column alignments across the whole sheet
            writer.println("<table style='border-collapse:collapse;font-family:Times New Roman;font-size:12pt;width:100%'>");
            
            // Define strict column widths for perfect A4 fitting
            writer.println("<colgroup>");
            writer.println("  <col style='width:50px'>");  // STT
            writer.println("  <col style='width:80pt'>");  // Mã GV
            writer.println("  <col style='width:260px'>"); // Họ và tên
            writer.println("  <col style='width:60pt'>");  // GT1 / Từ phòng
            writer.println("  <col style='width:60pt'>");  // GT2 / Đến phòng
            writer.println("  <col style='width:80pt'>");  // Phòng / Ghi chú
            writer.println("</colgroup>");
            
            // Header (Borderless)
            writer.println("<tr><td colspan='6' style='text-align:center;font-weight:bold;font-size:13pt;border:none'>CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</td></tr>");
            writer.println("<tr><td colspan='6' style='text-align:center;font-weight:bold;font-size:12pt;border:none'>Độc lập - Tự do - Hạnh phúc</td></tr>");
            
            String locationDate = "Đà Nẵng, ngày " + response.getCreatedAt().format(DateTimeFormatter.ofPattern("dd")) 
                + " tháng " + response.getCreatedAt().format(DateTimeFormatter.ofPattern("MM")) 
                + " năm " + response.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy"));
            writer.println("<tr><td colspan='6' style='text-align:right;font-style:italic;border:none'>" + locationDate + "</td></tr>");
            writer.println("<tr><td colspan='6' style='border:none'></td></tr>");
            writer.println("<tr><td colspan='6' style='text-align:center;font-weight:bold;font-size:14pt;border:none'>DANH SÁCH PHÂN CÔNG CÁN BỘ COI THI</td></tr>");
            writer.println("<tr><td colspan='6' style='border:none'></td></tr>");
            
            // Table Headers (Bordered)
            writer.println("<tr>"
                    + "<td rowspan='2' " + th + ">STT</td>"
                    + "<td rowspan='2' " + th + ">Mã GV</td>"
                    + "<td rowspan='2' " + th + ">Họ và tên</td>"
                    + "<td colspan='2' " + th + ">GIÁM THỊ</td>"
                    + "<td rowspan='2' " + th + ">Phòng thi</td></tr>");
            writer.println("<tr><td " + th + ">Giám thị 1</td><td " + th + ">Giám thị 2</td></tr>");

            int index = 1;
            for (Assignment assignment : response.getRoomAssignments()) {
                ExamOfficer a = assignment.getOfficer1();
                ExamOfficer b = assignment.getOfficer2();
                // Row 1
                writer.println("<tr><td " + tdCenter + ">" + String.format("%02d", index++) + "</td>"
                        + "<td " + tdCenter + ">" + escape(a.getId()) + "</td>"
                        + "<td " + tdLeft + ">" + escape(a.getName()) + "</td>"
                        + "<td " + tdCenter + ">X</td><td " + tdCenter + "></td>"
                        + "<td " + tdCenter + ">" + assignment.getRoomNumber() + "</td></tr>");
                // Row 2
                writer.println("<tr><td " + tdCenter + ">" + String.format("%02d", index++) + "</td>"
                        + "<td " + tdCenter + ">" + escape(b.getId()) + "</td>"
                        + "<td " + tdLeft + ">" + escape(b.getName()) + "</td>"
                        + "<td " + tdCenter + "></td><td " + tdCenter + ">X</td>"
                        + "<td " + tdCenter + ">" + assignment.getRoomNumber() + "</td></tr>");
            }

            // Separator between tables
            writer.println("<tr><td colspan='6' style='border:none'>&nbsp;</td></tr>");
            writer.println("<tr><td colspan='6' style='border:none'>&nbsp;</td></tr>");

            // --- TABLE 2: DANH SÁCH GIÁM SÁT ---
            writer.println("<tr><td colspan='6' style='text-align:center;font-weight:bold;font-size:14pt;border:none'>DANH SÁCH CÁN BỘ GIÁM SÁT HÀNH LANG</td></tr>");
            writer.println("<tr><td colspan='6' style='border:none'></td></tr>");

            writer.println("<tr>"
                    + "<td " + th + ">STT</td>"
                    + "<td " + th + ">Mã GV</td>"
                    + "<td " + th + ">Họ và tên</td>"
                    + "<td " + th + ">Từ phòng</td>"
                    + "<td " + th + ">Đến phòng</td>"
                    + "<td " + th + ">Ghi chú</td></tr>");

            index = 1;
            for (Assignment assignment : response.getCorridorAssignments()) {
                ExamOfficer officer = assignment.getOfficer1();
                writer.println("<tr><td " + tdCenter + ">" + String.format("%02d", index++) + "</td>"
                        + "<td " + tdCenter + ">" + escape(officer.getId()) + "</td>"
                        + "<td " + tdLeft + ">" + escape(officer.getName()) + "</td>"
                        + "<td " + tdCenter + ">" + assignment.getFromRoom() + "</td>"
                        + "<td " + tdCenter + ">" + assignment.getToRoom() + "</td>"
                        + "<td " + tdCenter + ">Cán bộ giám sát</td></tr>");
            }
            
            // Separator for Footer
            writer.println("<tr><td colspan='6' style='border:none'>&nbsp;</td></tr>");

            // --- FOOTER ---
            String printTime = "Trên đây là danh sách phân công chính thức được in lúc " + response.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy"));
            writer.println("<tr><td colspan='6' style='text-align:left;font-style:italic;border:none'>" + escape(printTime) + "</td></tr>");
            
            writer.println("</table>");
            writer.println("</body></html>");
        }
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
