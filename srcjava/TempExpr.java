public class TempExpr {
    interface IExpr {
        int getRows();
        int getCols();
        double get(int i, int j);
    }

    class ColumnVectorFromArray implements IExpr {
        private double[] _data;
        
        ColumnVectorFromArray(double[] data) {
            _data = data;
        }

        public int getRows() {return _data.length;}
        public int getCols() {return 1;}

        public double get(int i, int j) {
            return _data[i];
        }
    }

    static int numel(IExpr expr) {
        return expr.getCols()*expr.getRows();
    }
    
    static int computeIndex(int rows, int i, int j) {
        return i + rows*j;
    }

    void dispMat(IExpr expr) {
        for (int i = 0; i < expr.getRows(); i++) {
            String s = "";
            for (int j = 0; j < expr.getCols(); j++) {
                s += expr.get(i, j) + " ";
            }
            System.out.println(s);
        }
    }

    class Matrix implements IExpr {
        double[] _data;
        int _rows;
        int _cols;
        
        Matrix(int rows, int cols) {
            _rows = rows;
            _cols = cols;
            _data = new double[rows*cols];
        }

        public int getRows() {return _rows;}
        public int getCols() {return _cols;}

        void set(int i, int j, double x) {
            _data[computeIndex(_rows, i, j)] = x;
        }

        public double get(int i, int j) {
            return _data[computeIndex(_rows, i, j)];
        }

        double[] getData() {return _data;}
    }

    Matrix realize(IExpr expr) {
        int rows = expr.getRows();
        int cols = expr.getCols();
        Matrix dst = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                dst.set(i, j, expr.get(i, j));
            }
        }
        return dst;
    }

    class Reshape implements IExpr {
        IExpr _src;
        int _rows;
        int _cols;

        Reshape(IExpr colvec, int rows) {
            _src = colvec;
            _rows = rows;
            _cols = colvec.getRows()/rows;
        }

        public int getCols() {return _cols;}
        public int getRows() {return _rows;}

        public double get(int i, int j) {
            return _src.get(i + _rows*j, 0);
        }
    }

    class Transpose implements IExpr {
        IExpr _src;

        Transpose(IExpr src) {
            _src = src;
        }

        public int getRows() {return _src.getCols();}
        public int getCols() {return _src.getRows();}

        public double get(int i, int j) {
            return _src.get(j, i);
        }
    }

    class SubMat implements IExpr {
        IExpr _a;
        IExpr _b;
        
        SubMat(IExpr a, IExpr b) {
            _a = a;
            _b = b;
        }

        public int getRows() {return _a.getRows();}
        public int getCols() {return _a.getCols();}
        

        public double get(int i, int j) {
            return _a.get(i, j) - _b.get(i, j);
        }
    }

    class MulMat implements IExpr {
        IExpr _a;
        IExpr _b;

        MulMat(IExpr a, IExpr b) {
            _a = a;
            _b = b;
        }

        public int getRows() {return _a.getRows();}
        public int getCols() {return _b.getCols();}

        public double get(int i, int j) {
            double sum = 0.0;
            int n = _a.getCols();
            for (int k = 0; k < n; k++) {
                sum += _a.get(i, k)*_b.get(k, j);
            }
            return sum;
        }
    }

    class Ones implements IExpr {
        int _rows;
        int _cols;
        
        Ones(int rows, int cols) {
            _rows = rows;
            _cols = cols;
        }

        public int getRows() {return _rows;}
        public int getCols() {return _cols;}
        
        public double get(int i, int j) {
            return 1.0;
        }
    }

    class ScaleMat implements IExpr {
        double _s;
        IExpr _x;

        ScaleMat(double s, IExpr x) {
            _s = s;
            _x = x;
        }

        public int getRows() {return _x.getRows();}
        public int getCols() {return _x.getCols();}
        
        public double get(int i, int j) {
            return _s*_x.get(i, j);
        }
    }

    Matrix covarianceMatrix(int dim, double[] data) {
        IExpr V = new ColumnVectorFromArray(data);
        IExpr X = new Reshape(V, dim);
        int N = data.length/dim;
        IExpr mu = realize(new ScaleMat(1.0/N, 
                new MulMat(
                    new Ones(1, N),
                    new Transpose(X))));

        IExpr muRepeated = new Transpose(
            new MulMat(
                new Ones(N, 1),
                mu));

        IExpr Xc = new SubMat(X, muRepeated);
        IExpr covariance = new ScaleMat(1.0/(N - 1), 
            new MulMat(Xc, new Transpose(Xc)));
        return realize(covariance);
    }

    public double[] run(int dim, double[] data) {
        return covarianceMatrix(dim, data).getData();
    }
    
}
