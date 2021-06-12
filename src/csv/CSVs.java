package csv;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

public class CSVs {
    private static final char DELIMITER = ',';

    /**
     * @param isFirstLineHeader csv 파일의 첫 라인을 헤더(타이틀)로 처리할까요?
     */
    public static Table createTable(File csv, boolean isFirstLineHeader) throws FileNotFoundException {
        List<List<String>> parsedTable = new ArrayList<>();
        List<String> line;

        try {
            BufferedReader br = new BufferedReader(new FileReader(csv));
            String input = "", token = "";
            while ((input = br.readLine()) != null) {
                line = new ArrayList<>();
                int start = 0;
                char current = 0;
                boolean hasQuote = false;

                for (int i = 0; i < input.length(); ++i) {
                    current = input.charAt(i);

                    if (current == '"') hasQuote = !hasQuote;

                    // new token
                    if (!hasQuote && current == DELIMITER || i == input.length() - 1) {
                        int end = i;
                        if (current != DELIMITER && i == input.length() - 1) end += 1;

                        token = input.substring(start, end);

                        if (token.startsWith("\"")) {
                            token = token
                                    .substring(1, token.length() - 1)
                                    .replaceAll("\"\"", "\"");
                        }

                        line.add(token);
                        start = i + 1;
                    }

                    if (current == DELIMITER && i == input.length() - 1) line.add("");
                }
                parsedTable.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new TableImpl(parsedTable, isFirstLineHeader);
    }

    /**
     * @return 새로운 Table 객체를 반환한다. 즉, 첫 번째 매개변수 Table은 변경되지 않는다.
     */
    public static Table sort(Table table, int byIndexOfColumn, boolean isAscending, boolean isNullFirst) {
        return table
                .selectRows(0, table.getRowCount())
                .sort(byIndexOfColumn, isAscending, isNullFirst);
    }

    /**
     * @return 새로운 Table 객체를 반환한다. 즉, 첫 번째 매개변수 Table은 변경되지 않는다.
     */
    public static Table shuffle(Table table) {
        return table
                .selectRows(0, table.getRowCount())
                .shuffle();
    }
}