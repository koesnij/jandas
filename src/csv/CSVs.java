package csv;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CSVs {
    private static final char DELIMITER = ',';
    private static final class Pair<X, Y> {
        private X x;
        private Y y;

        Pair(X x, Y y) {
            this.x = x;
            this.y = y;
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new TableImpl(parsedTable, isFirstLineHeader);
    }

    /**
     * @return 새로운 Table 객체를 반환한다. 즉, 첫 번째 매개변수 Table은 변경되지 않는다.
     */
    public static Table sort(Table table, int byIndexOfColumn, boolean isAscending, boolean isNullFirst) {
        var newTable = table.selectRows(0, table.getRowCount());
        var criteria = table.getColumn(byIndexOfColumn);

        // make a table for sorting
        List<Pair<Integer, String>> list = new ArrayList<>();
        for(int i = 0; i < criteria.count(); ++i) {
            var value = criteria.getValue(i);
            if (value.equals("null")) {
                // asc&&nullFirst -MAX / asc&&!nullFirst MAX / !asc&&nullFirst MAX / !asc&&!nullFirst -MAX
                if (criteria.isNumericColumn())
                    value = Double.MAX_VALUE * (isNullFirst ^ isAscending ? 1 : -1) + "";
                else
                    value = isNullFirst ^ isAscending ? "\uffff" : "\u0000";
            }
            list.add(new Pair<>(i, value));
        }

        // sort
        if (criteria.isNumericColumn())
            list.sort((o1, o2) -> isAscending ? Double.valueOf(o1.y).compareTo(Double.valueOf(o2.y)) : Double.valueOf(o2.y).compareTo(Double.valueOf(o1.y)));
        else
            list.sort((o1, o2) -> isAscending ? o1.y.compareTo(o2.y) : o2.y.compareTo(o1.y));

        // set value
        for(int row = 0; row < newTable.getRowCount(); ++row)
            for (int col = 0; col < newTable.getColumnCount(); ++col)
                newTable.getColumn(col).setValue( row, table.getColumn(col).getValue(list.get(row).x) );

        return newTable;
    }

    /**
     * @return 새로운 Table 객체를 반환한다. 즉, 첫 번째 매개변수 Table은 변경되지 않는다.
     */
    public static Table shuffle(Table table) {
        var newTable = table.selectRows(0, table.getRowCount());

        // make a table for shuffle
        List<Integer> list = new ArrayList<>();
        for(int i = 0; i < table.getRowCount(); ++i) list.add(i);

        // shuffle
        Collections.shuffle(list);

        // set value
        for(int row = 0; row < newTable.getRowCount(); ++row)
            for (int col = 0; col < newTable.getColumnCount(); ++col)
                newTable.getColumn(col).setValue( row, table.getColumn(col).getValue(list.get(row)) );

        return newTable;
    }


}