package csv;

import java.util.*;

class ColumnImpl implements Column {
    private String header;
    private List<String> cells;
    List<String> canvas; // for Column.print(), Table.print()

    ColumnImpl() {
        this.cells = new ArrayList<>();
    }

    ColumnImpl(String header) {
        this();
        this.header = header;
    }

    void addCell(String cell) {
        if (cell.length() == 0) cell = null;
        cells.add(cell);
    }

    boolean isIntegerColumn() {
        for (String cell : cells) {
            try {
                if (cell != null) Integer.parseInt(cell);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    int getWidth() {
        int maxWidth = header.length();

        if (isNumericColumn()) {
            for (String cell : cells) {
                // null
                if (cell == null) {
                    maxWidth = Math.max(maxWidth, 4);
                    continue;
                }

                // Double type
                if (cell.contains(".")) {
                    var iLength = cell.substring(0, cell.indexOf('.')).length();
                    var fLength = cell.substring(cell.indexOf('.')).length();
                    maxWidth = Math.max(maxWidth, iLength + Math.min(fLength, 7));
                    continue;
                }

                // Integer type & null
                maxWidth = Math.max(maxWidth, cell.length());
            }

            return maxWidth;
        }

        // Not numeric column
        for (String cell : cells) {
            maxWidth = Math.max(maxWidth, cell == null ? 4 : cell.length());
        }

        return maxWidth;
    }

    private double getQuartile(int q) {
        // make double array
        List<Double> doubles = new ArrayList<>();
        for (int i = 0; i < count(); ++i) {
            try {
                doubles.add(getValue(i, Double.class));
            } catch (NumberFormatException e) {
                // Not a Number
            }
        }
        Collections.sort(doubles); // sort

        // get quartile
        double index = q * (doubles.size() - 1) / 4.0;
        int lower = (int) Math.floor(index);

        if (lower < 0) return doubles.get(0); // defensive code

        if (lower >= doubles.size() - 1) return doubles.get(doubles.size() - 1);

        double fraction = index - lower;
        return doubles.get(lower) + fraction * (doubles.get(lower + 1) - doubles.get(lower));
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String getHeader() {
        return header;
    }

    @Override
    public String getValue(int index) {
        return cells.get(index);
    }

    @Override
    public <T extends Number> T getValue(int index, Class<T> t) throws NumberFormatException {
        if(cells.get(index) == null) return null;
        if (t.isAssignableFrom(Double.class)) {
            return t.cast(Double.parseDouble(cells.get(index)));
        } else if (t.isAssignableFrom(Integer.class)) {
            return t.cast(Integer.parseInt(cells.get(index)));
        } else {
            throw new IllegalArgumentException("Bad type.");
        }
    }

    @Override
    public void setValue(int index, String value) {
        cells.set(index, value);
    }

    @Override
    public <T extends Number> void setValue(int index, T value) {
        cells.set(index, String.valueOf(value));
    }

    @Override
    public int count() {
        return cells.size();
    }

    @Override
    public void print() {
        var width = getWidth();
        canvas = new ArrayList<>(); // init

        if (isNumericColumn()) {
            for (String cell : cells) {
                // double 인 경우 반올림
                if (cell != null && cell.contains(".")) {
                    cell = Math.round(Double.parseDouble(cell) * 1000000) / 1000000.0 + "";
                }
                canvas.add(String.format("%" + width + "s", cell));
            }
        } else {
            for (String cell : cells) {
                canvas.add(String.format("%" + width + "s", cell));
            }
        }
    }

    @Override
    public boolean isNumericColumn() {
        for (String cell : cells) {
            try {
                if (cell != null) Double.parseDouble(cell);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public long getNullCount() {
        var count = 0;
        for (String cell : cells) {
            if (cell == null) ++count;
        }
        return count;
    }

    @Override
    public long getNumericCount() {
        var count = 0;
        for (String element : cells) {
            try {
                Double.parseDouble(element);
                ++count;
            } catch (NumberFormatException e) {
                // pass NaN
            }
        }
        return count;
    }

    @Override
    public double getNumericMin() {
        var min = Double.MAX_VALUE;
        for (String cell : cells) {
            try {
                min = Math.min(min, Double.parseDouble(cell));
            } catch (NumberFormatException e) {
                // pass NaN
            }
        }
        return min;
    }

    @Override
    public double getNumericMax() {
        var max = -Double.MAX_VALUE;
        for (String cell : cells) {
            try {
                max = Math.max(max, Double.parseDouble(cell));
            } catch (NumberFormatException e) {
                // pass NaN
            }
        }
        return max;
    }

    @Override
    public double getMean() {
        var sum = 0.0;
        for (String cell : cells) {
            try {
                sum += Double.parseDouble(cell);
            } catch (NumberFormatException e) {
                // pass NaN
            }
        }
        return sum / getNumericCount();
    }

    @Override
    public double getStd() {
        var sum = 0.0;
        var mean = getMean();

        for (String cell : cells) {
            try {
                sum += Math.pow(Double.parseDouble(cell) - mean, 2);
            } catch (NumberFormatException e) {
                // pass NaN
            }
        }
        return Math.sqrt(sum / (getNumericCount() - 1)); // divide by (n-1)
    }

    @Override
    public double getQ1() {
        return getQuartile(1);
    }

    @Override
    public double getMedian() {
        return getQuartile(2);
    }

    @Override
    public double getQ3() {
        return getQuartile(3);
    }

    @Override
    public boolean fillNullWithMean() {
        if (!isNumericColumn() || getNullCount() == 0) return false;

        var mean = getMean();
        for (var row = 0; row < cells.size(); ++row) {
            if (getValue(row) == null) {
                setValue(row, mean);
            }
        }
        return true;
    }

    @Override
    public boolean fillNullWithZero() {
        if (!isNumericColumn() || getNullCount() == 0) return false;

        for (var row = 0; row < cells.size(); ++row) {
            if (getValue(row) == null) {
                setValue(row, 0);
            }
        }
        return true;
    }

    @Override
    public boolean standardize() {
        if (!isNumericColumn()) return false;

        var isModified = false;
        var std = getStd();
        var mean = getMean();

        for (var row = 0; row < count(); ++row) {
            try {
                double value = getValue(row, Double.class);
                value = (value - mean) / std; // standardize
                setValue(row, value);

                isModified = true; // check column is modified
            } catch (NumberFormatException e) {
                // pass null
            }
        }
        return isModified;
    }

    @Override
    public boolean normalize() {
        if (!isNumericColumn()) return false;

        var isModified = false;
        var min = getNumericMin();
        var range = getNumericMax() - min;

        for (var row = 0; row < count(); ++row) {
            try {
                double value = getValue(row, Double.class);
                value = (value - min) / range; // normalize
                setValue(row, value);

                isModified = true; // check column is modified
            } catch (NumberFormatException e) {
                // pass null
            }
        }
        return isModified;
    }

    @Override
    public boolean factorize() {
        TreeSet<String> set = new TreeSet<>(cells);
        set.remove(null);
        if (set.size() != 2) return false;

        var isModified = false;
        var first = set.first();
        var last = set.last();

        for (var row = 0; row < count(); ++row) {
            if (getValue(row).equals(first)) {
                setValue(row, 0);
                isModified = true;
            } else if (getValue(row).equals(last)) {
                setValue(row, 1);
                isModified = true;
            }
        }
        return isModified;
    }
}
