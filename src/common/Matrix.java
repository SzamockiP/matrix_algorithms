package common;

public final class Matrix<A> {
    private final A data;
    private final int rows;
    private final int cols;

    Matrix(A data, int rows, int cols) {
        this.data = data;
        this.rows = rows;
        this.cols = cols;
    }

    public int rows() { return rows; }
    public int cols() { return cols; }

    public MatrixView<A> view() {
        return new MatrixView<>(data, 0, rows, cols, cols);
    }
}
