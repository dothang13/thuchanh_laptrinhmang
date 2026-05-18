import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelExporter {
    private static final int MAX_ASSIGNMENTS_PER_SHEET = 15;
    private static final int MAX_CORRIDOR_PER_SHEET = 25;

    public void export(File file, ScheduleResponse response) throws Exception {
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<?mso-application progid=\"Excel.Sheet\"?>");
            writer.println("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"");
            writer.println(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"");
            writer.println(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"");
            writer.println(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"");
            writer.println(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">");
            
            // Define Styles
            writer.println(" <Styles>");
            writer.println("  <Style ss:ID=\"Default\" ss:Name=\"Normal\">");
            writer.println("   <Alignment ss:Vertical=\"Center\"/>");
            writer.println("   <Borders/>");
            writer.println("   <Font ss:FontName=\"Times New Roman\" ss:Size=\"12\"/>");
            writer.println("  </Style>");
            
            writer.println("  <Style ss:ID=\"sMotto\">");
            writer.println("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>");
            writer.println("   <Font ss:FontName=\"Times New Roman\" ss:Size=\"13\" ss:Bold=\"1\"/>");
            writer.println("  </Style>");
            
            writer.println("  <Style ss:ID=\"sMottoSub\">");
            writer.println("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>");
            writer.println("   <Font ss:FontName=\"Times New Roman\" ss:Size=\"12\" ss:Bold=\"1\"/>");
            writer.println("  </Style>");

            writer.println("  <Style ss:ID=\"sDate\">");
            writer.println("   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>");
            writer.println("   <Font ss:FontName=\"Times New Roman\" ss:Size=\"12\" ss:Italic=\"1\"/>");
            writer.println("  </Style>");
            
            writer.println("  <Style ss:ID=\"sTitle\">");
            writer.println("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>");
            writer.println("   <Font ss:FontName=\"Times New Roman\" ss:Size=\"14\" ss:Bold=\"1\"/>");
            writer.println("  </Style>");
            
            writer.println("  <Style ss:ID=\"th\">");
            writer.println("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>");
            writer.println("   <Borders>");
            writer.println("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
            writer.println("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
            writer.println("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
            writer.println("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
            writer.println("   </Borders>");
            writer.println("   <Font ss:FontName=\"Times New Roman\" ss:Size=\"12\" ss:Bold=\"1\"/>");
            writer.println("  </Style>");
            
            writer.println("  <Style ss:ID=\"tdCenter\">");
            writer.println("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>");
            writer.println("   <Borders>");
            writer.println("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
            writer.println("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
            writer.println("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
            writer.println("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
            writer.println("   </Borders>");
            writer.println("  </Style>");
            
            writer.println("  <Style ss:ID=\"tdLeft\">");
            writer.println("   <Alignment ss:Horizontal=\"Left\" ss:Vertical=\"Center\"/>");
            writer.println("   <Borders>");
            writer.println("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
            writer.println("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
            writer.println("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
            writer.println("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
            writer.println("   </Borders>");
            writer.println("  </Style>");
            
            writer.println("  <Style ss:ID=\"sFooter\">");
            writer.println("   <Alignment ss:Horizontal=\"Left\" ss:Vertical=\"Center\"/>");
            writer.println("   <Font ss:FontName=\"Times New Roman\" ss:Size=\"12\" ss:Italic=\"1\"/>");
            writer.println("  </Style>");
            writer.println(" </Styles>");

            String locationDate = "Đà Nẵng, ngày " + response.getCreatedAt().format(DateTimeFormatter.ofPattern("dd")) 
                + " tháng " + response.getCreatedAt().format(DateTimeFormatter.ofPattern("MM")) 
                + " năm " + response.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy"));
            String printTime = "Trên đây là danh sách phân công chính thức được in lúc " + response.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy"));

            // Write Room Assignments
            List<Assignment> rooms = response.getRoomAssignments();
            int sheetCount = 1;
            int officerIndex = 1;
            for (int i = 0; i < rooms.size(); i += MAX_ASSIGNMENTS_PER_SHEET) {
                int end = Math.min(i + MAX_ASSIGNMENTS_PER_SHEET, rooms.size());
                List<Assignment> chunk = rooms.subList(i, end);
                writeRoomSheet(writer, chunk, "Danh sach CT " + sheetCount, locationDate, printTime, officerIndex, i + MAX_ASSIGNMENTS_PER_SHEET >= rooms.size());
                officerIndex += chunk.size() * 2;
                sheetCount++;
            }

            // Write Corridor Assignments
            List<Assignment> corridors = response.getCorridorAssignments();
            int corridorSheetCount = 1;
            int corridorIndex = 1;
            for (int i = 0; i < corridors.size(); i += MAX_CORRIDOR_PER_SHEET) {
                int end = Math.min(i + MAX_CORRIDOR_PER_SHEET, corridors.size());
                List<Assignment> chunk = corridors.subList(i, end);
                writeCorridorSheet(writer, chunk, "Giam sat " + corridorSheetCount, locationDate, printTime, corridorIndex, i + MAX_CORRIDOR_PER_SHEET >= corridors.size());
                corridorIndex += chunk.size();
                corridorSheetCount++;
            }

            writer.println("</Workbook>");
        }
    }

    private void writeRoomSheet(PrintWriter writer, List<Assignment> assignments, String sheetName, String locationDate, String printTime, int startIndex, boolean isLastSheet) {
        writer.println(" <Worksheet ss:Name=\"" + escape(sheetName) + "\">");
        // Print setup for A4
        writer.println("  <WorksheetOptions xmlns=\"urn:schemas-microsoft-com:office:excel\">");
        writer.println("   <PageSetup>");
        writer.println("    <PaperSizeIndex>9</PaperSizeIndex>"); // A4
        writer.println("   </PageSetup>");
        writer.println("   <Print>");
        writer.println("    <ValidPrinterInfo/>");
        writer.println("    <HorizontalResolution>600</HorizontalResolution>");
        writer.println("    <VerticalResolution>600</VerticalResolution>");
        writer.println("   </Print>");
        writer.println("  </WorksheetOptions>");
        writer.println("  <Table>");
        writer.println("   <Column ss:Width=\"37.5\"/>"); // 50px
        writer.println("   <Column ss:Width=\"80\"/>");   // 80pt
        writer.println("   <Column ss:Width=\"195\"/>");  // 260px
        writer.println("   <Column ss:Width=\"60\"/>");   // 60pt
        writer.println("   <Column ss:Width=\"60\"/>");   // 60pt
        writer.println("   <Column ss:Width=\"80\"/>");   // 80pt
        
        writeHeader(writer, "DANH SÁCH PHÂN CÔNG CÁN BỘ COI THI", locationDate);

        // Table Headers
        writer.println("   <Row ss:Height=\"20\">");
        writer.println("    <Cell ss:MergeDown=\"1\" ss:StyleID=\"th\"><Data ss:Type=\"String\">STT</Data></Cell>");
        writer.println("    <Cell ss:MergeDown=\"1\" ss:StyleID=\"th\"><Data ss:Type=\"String\">Mã GV</Data></Cell>");
        writer.println("    <Cell ss:MergeDown=\"1\" ss:StyleID=\"th\"><Data ss:Type=\"String\">Họ và tên</Data></Cell>");
        writer.println("    <Cell ss:MergeAcross=\"1\" ss:StyleID=\"th\"><Data ss:Type=\"String\">GIÁM THỊ</Data></Cell>");
        writer.println("    <Cell ss:MergeDown=\"1\" ss:StyleID=\"th\"><Data ss:Type=\"String\">Phòng thi</Data></Cell>");
        writer.println("   </Row>");
        writer.println("   <Row ss:Height=\"20\">");
        writer.println("    <Cell ss:Index=\"4\" ss:StyleID=\"th\"><Data ss:Type=\"String\">Giám thị 1</Data></Cell>");
        writer.println("    <Cell ss:StyleID=\"th\"><Data ss:Type=\"String\">Giám thị 2</Data></Cell>");
        writer.println("   </Row>");

        for (Assignment assignment : assignments) {
            ExamOfficer a = assignment.getOfficer1();
            ExamOfficer b = assignment.getOfficer2();
            
            writer.println("   <Row ss:Height=\"20\">");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"String\">" + String.format("%02d", startIndex++) + "</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"String\">" + escape(a.getId()) + "</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdLeft\"><Data ss:Type=\"String\">" + escape(a.getName()) + "</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"String\">X</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"String\"></Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"Number\">" + assignment.getRoomNumber() + "</Data></Cell>");
            writer.println("   </Row>");
            
            writer.println("   <Row ss:Height=\"20\">");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"String\">" + String.format("%02d", startIndex++) + "</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"String\">" + escape(b.getId()) + "</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdLeft\"><Data ss:Type=\"String\">" + escape(b.getName()) + "</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"String\"></Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"String\">X</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"Number\">" + assignment.getRoomNumber() + "</Data></Cell>");
            writer.println("   </Row>");
        }

        if (isLastSheet) {
            writer.println("   <Row ss:Height=\"15\"><Cell><Data ss:Type=\"String\"></Data></Cell></Row>");
            writer.println("   <Row>");
            writer.println("    <Cell ss:MergeAcross=\"5\" ss:StyleID=\"sFooter\"><Data ss:Type=\"String\">" + escape(printTime) + "</Data></Cell>");
            writer.println("   </Row>");
        }

        writer.println("  </Table>");
        writer.println(" </Worksheet>");
    }

    private void writeCorridorSheet(PrintWriter writer, List<Assignment> assignments, String sheetName, String locationDate, String printTime, int startIndex, boolean isLastSheet) {
        writer.println(" <Worksheet ss:Name=\"" + escape(sheetName) + "\">");
        writer.println("  <WorksheetOptions xmlns=\"urn:schemas-microsoft-com:office:excel\">");
        writer.println("   <PageSetup>");
        writer.println("    <PaperSizeIndex>9</PaperSizeIndex>"); // A4
        writer.println("   </PageSetup>");
        writer.println("   <Print>");
        writer.println("    <ValidPrinterInfo/>");
        writer.println("    <HorizontalResolution>600</HorizontalResolution>");
        writer.println("    <VerticalResolution>600</VerticalResolution>");
        writer.println("   </Print>");
        writer.println("  </WorksheetOptions>");
        writer.println("  <Table>");
        writer.println("   <Column ss:Width=\"37.5\"/>");
        writer.println("   <Column ss:Width=\"80\"/>");
        writer.println("   <Column ss:Width=\"195\"/>");
        writer.println("   <Column ss:Width=\"60\"/>");
        writer.println("   <Column ss:Width=\"60\"/>");
        writer.println("   <Column ss:Width=\"80\"/>");
        
        writeHeader(writer, "DANH SÁCH CÁN BỘ GIÁM SÁT HÀNH LANG", locationDate);

        writer.println("   <Row ss:Height=\"20\">");
        writer.println("    <Cell ss:StyleID=\"th\"><Data ss:Type=\"String\">STT</Data></Cell>");
        writer.println("    <Cell ss:StyleID=\"th\"><Data ss:Type=\"String\">Mã GV</Data></Cell>");
        writer.println("    <Cell ss:StyleID=\"th\"><Data ss:Type=\"String\">Họ và tên</Data></Cell>");
        writer.println("    <Cell ss:StyleID=\"th\"><Data ss:Type=\"String\">Từ phòng</Data></Cell>");
        writer.println("    <Cell ss:StyleID=\"th\"><Data ss:Type=\"String\">Đến phòng</Data></Cell>");
        writer.println("    <Cell ss:StyleID=\"th\"><Data ss:Type=\"String\">Ghi chú</Data></Cell>");
        writer.println("   </Row>");

        for (Assignment assignment : assignments) {
            ExamOfficer officer = assignment.getOfficer1();
            writer.println("   <Row ss:Height=\"20\">");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"String\">" + String.format("%02d", startIndex++) + "</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"String\">" + escape(officer.getId()) + "</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdLeft\"><Data ss:Type=\"String\">" + escape(officer.getName()) + "</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"Number\">" + assignment.getFromRoom() + "</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"Number\">" + assignment.getToRoom() + "</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"tdCenter\"><Data ss:Type=\"String\">Cán bộ giám sát</Data></Cell>");
            writer.println("   </Row>");
        }

        if (isLastSheet) {
            writer.println("   <Row ss:Height=\"15\"><Cell><Data ss:Type=\"String\"></Data></Cell></Row>");
            writer.println("   <Row>");
            writer.println("    <Cell ss:MergeAcross=\"5\" ss:StyleID=\"sFooter\"><Data ss:Type=\"String\">" + escape(printTime) + "</Data></Cell>");
            writer.println("   </Row>");
        }

        writer.println("  </Table>");
        writer.println(" </Worksheet>");
    }

    private void writeHeader(PrintWriter writer, String title, String locationDate) {
        writer.println("   <Row ss:Height=\"20\">");
        writer.println("    <Cell ss:MergeAcross=\"5\" ss:StyleID=\"sMotto\"><Data ss:Type=\"String\">CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</Data></Cell>");
        writer.println("   </Row>");
        writer.println("   <Row ss:Height=\"20\">");
        writer.println("    <Cell ss:MergeAcross=\"5\" ss:StyleID=\"sMottoSub\"><Data ss:Type=\"String\">Độc lập - Tự do - Hạnh phúc</Data></Cell>");
        writer.println("   </Row>");
        writer.println("   <Row ss:Height=\"20\">");
        writer.println("    <Cell ss:MergeAcross=\"5\" ss:StyleID=\"sDate\"><Data ss:Type=\"String\">" + escape(locationDate) + "</Data></Cell>");
        writer.println("   </Row>");
        writer.println("   <Row ss:Height=\"15\"><Cell><Data ss:Type=\"String\"></Data></Cell></Row>");
        writer.println("   <Row ss:Height=\"25\">");
        writer.println("    <Cell ss:MergeAcross=\"5\" ss:StyleID=\"sTitle\"><Data ss:Type=\"String\">" + escape(title) + "</Data></Cell>");
        writer.println("   </Row>");
        writer.println("   <Row ss:Height=\"15\"><Cell><Data ss:Type=\"String\"></Data></Cell></Row>");
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
