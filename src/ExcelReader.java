import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ExcelReader {
    public ExcelData read(File file) throws Exception {
        try (ZipFile zip = new ZipFile(file)) {
            List<String> sharedStrings = readSharedStrings(zip);
            Document sheet = readXml(zip, "xl/worksheets/sheet1.xml");
            NodeList rowNodes = sheet.getElementsByTagName("row");
            java.util.Map<String, ExamOfficer> officers = new java.util.LinkedHashMap<>();
            ColumnMap columns = new ColumnMap();

            for (int i = 0; i < rowNodes.getLength(); i++) {
                Element row = (Element) rowNodes.item(i);
                Map<String, String> values = readRow(row, sharedStrings);
                if (columns.detect(values)) {
                    continue;
                }

                String id = first(values, columns.idColumn, "A", "0");
                String name = first(values, columns.nameColumn, "B", "1");
                String unit = first(values, columns.unitColumn, "C", "2");
                String birthDate = formatBirthDate(first(values, columns.birthDateColumn, "D", "3"));
                String oldRoom = columns.oldRoomColumn == null ? "" : first(values, columns.oldRoomColumn, "D", "3");
                if (name.isBlank()) {
                    continue;
                }
                if (name.matches("\\d+") && !first(values, "C", "2").isBlank()) {
                    id = first(values, "B", "1");
                    name = first(values, "C", "2");
                    birthDate = formatBirthDate(first(values, "D", "3"));
                    unit = first(values, "E", "4");
                    oldRoom = "";
                }
                
                String key = id + "_" + birthDate;
                if (officers.containsKey(key)) {
                    ExamOfficer existing = officers.get(key);
                    if (unit != null && !unit.isBlank() && !existing.getUnit().contains(unit)) {
                        String newUnit = existing.getUnit() + " - " + unit;
                        officers.put(key, new ExamOfficer(existing.getId(), existing.getName(), existing.getBirthDate(), newUnit, existing.getOldRoom()));
                    }
                } else {
                    officers.put(key, new ExamOfficer(id, name, birthDate, unit, oldRoom));
                }
            }
            List<ExamOfficer> officerList = new ArrayList<>(officers.values());

            List<Integer> roomList = new ArrayList<>();
            try {
                Document sheet2 = readXml(zip, "xl/worksheets/sheet2.xml");
                NodeList rowNodes2 = sheet2.getElementsByTagName("row");
                for (int i = 0; i < rowNodes2.getLength(); i++) {
                    Element row = (Element) rowNodes2.item(i);
                    Map<String, String> values = readRow(row, sharedStrings);
                    String roomStr = first(values, "B", "1");
                    if (roomStr != null && roomStr.matches("\\d+")) {
                        int roomNum = Integer.parseInt(roomStr);
                        if (!roomList.contains(roomNum)) {
                            roomList.add(roomNum);
                        }
                    }
                }
            } catch (Exception ex) {
                // Ignore if sheet 2 does not exist or has an error
            }

            return new ExcelData(officerList, roomList);
        }
    }

    private List<String> readSharedStrings(ZipFile zip) throws Exception {
        ZipEntry entry = zip.getEntry("xl/sharedStrings.xml");
        List<String> result = new ArrayList<>();
        if (entry == null) {
            return result;
        }
        Document document = readXml(zip, entry.getName());
        NodeList nodes = document.getElementsByTagName("si");
        for (int i = 0; i < nodes.getLength(); i++) {
            result.add(nodes.item(i).getTextContent());
        }
        return result;
    }

    private Map<String, String> readRow(Element row, List<String> sharedStrings) {
        Map<String, String> values = new HashMap<>();
        NodeList cells = row.getElementsByTagName("c");
        for (int i = 0; i < cells.getLength(); i++) {
            Element cell = (Element) cells.item(i);
            String ref = cell.getAttribute("r").replaceAll("[0-9]", "");
            if (ref.isBlank()) {
                ref = String.valueOf(i);
            }
            values.put(ref, readCell(cell, sharedStrings));
            values.put(String.valueOf(i), readCell(cell, sharedStrings));
        }
        return values;
    }

    private String readCell(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");
        NodeList inline = cell.getElementsByTagName("t");
        if (inline.getLength() > 0 && "inlineStr".equals(type)) {
            return inline.item(0).getTextContent().trim();
        }
        NodeList values = cell.getElementsByTagName("v");
        if (values.getLength() == 0) {
            return "";
        }
        String raw = values.item(0).getTextContent().trim();
        if ("s".equals(type)) {
            try {
                int index = Integer.parseInt(raw);
                return index >= 0 && index < sharedStrings.size() ? sharedStrings.get(index).trim() : raw;
            } catch (NumberFormatException ex) {
                return raw;
            }
        }
        return raw.replaceAll("\\.0$", "");
    }

    private Document readXml(ZipFile zip, String name) throws Exception {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            throw new IllegalArgumentException("Khong tim thay " + name + " trong file Excel.");
        }
        try (InputStream input = zip.getInputStream(entry)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(input);
        }
    }

    private String first(Map<String, String> values, String preferred, String column, String fallback) {
        String value = preferred == null ? "" : values.get(preferred);
        if (value == null || value.isBlank()) {
            value = values.get(column);
        }
        if (value == null || value.isBlank()) {
            value = values.get(fallback);
        }
        return value == null ? "" : value.trim();
    }

    private String first(Map<String, String> values, String column, String fallback) {
        return first(values, null, column, fallback);
    }

    private String formatBirthDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String value = raw.trim();
        if (value.matches("\\d+(\\.\\d+)?")) {
            try {
                long serial = Math.round(Double.parseDouble(value));
                return LocalDate.of(1899, 12, 30).plusDays(serial)
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (NumberFormatException ex) {
                return value;
            }
        }

        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                new DateTimeFormatterBuilder().parseCaseInsensitive()
                        .appendPattern("d-MMM-yyyy")
                        .toFormatter(Locale.ENGLISH)
        };
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException ignored) {
            }
        }
        return value;
    }

    private static class ColumnMap {
        private String idColumn;
        private String nameColumn;
        private String birthDateColumn;
        private String unitColumn;
        private String oldRoomColumn;

        private boolean detect(Map<String, String> values) {
            boolean header = false;
            for (Map.Entry<String, String> entry : values.entrySet()) {
                if (entry.getKey().matches("\\d+")) {
                    continue;
                }
                String text = normalize(entry.getValue());
                if (text.contains("ma gv") || text.contains("ma can bo") || text.contains("ma cb")) {
                    idColumn = entry.getKey();
                    header = true;
                } else if (text.contains("ho ten") || text.contains("ten can bo")) {
                    nameColumn = entry.getKey();
                    header = true;
                } else if (text.contains("ngay sinh") || text.contains("birth")) {
                    birthDateColumn = entry.getKey();
                    header = true;
                } else if (text.contains("don vi") || text.contains("cong tac")) {
                    unitColumn = entry.getKey();
                    header = true;
                } else if (text.contains("phong cu") || text.contains("phong da coi")) {
                    oldRoomColumn = entry.getKey();
                    header = true;
                }
            }
            return header;
        }

        private String normalize(String value) {
            return value == null ? "" : java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .replace("đ", "d")
                    .replace("Đ", "D")
                    .toLowerCase()
                    .trim();
        }
    }
}
