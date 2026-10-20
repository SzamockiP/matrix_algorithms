package common;

public final class MatrixView<A> {
    final A data;
    final int offset;
    final int rows;
    final int cols;
    final int stride;

    MatrixView(A data, int offset, int rows, int cols, int stride) {
        this.data = data;
        this.offset = offset;
        this.rows = rows;
        this.cols = cols;
        this.stride = stride;
    }

    public int rows() { return rows; }
    public int cols() { return cols; }

    int idx(int i, int j) { return offset + i * stride + j; }

    int checkedIdx(int i, int j) {
        if (i < 0 || j < 0 || i >= rows || j >= cols)
            throw new IndexOutOfBoundsException("(" + i + "," + j + ") poza " + rows + "x" + cols);
        return idx(i, j);
    }

    public MatrixView<A> block(int row0, int col0, int height, int width) {
        if (row0 < 0 || col0 < 0 || height < 0 || width < 0
                || row0 + height > rows || col0 + width > cols)
            throw new IndexOutOfBoundsException("Blok poza macierzą");
        return new MatrixView<>(data, idx(row0, col0), height, width, stride);
    }
}
