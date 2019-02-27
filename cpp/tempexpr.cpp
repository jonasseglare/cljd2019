#include <iostream>
#include "benjmark.h"
#include <vector>
#include <memory>
#include "common.h"

class ColumnVectorFromArray {
public:
  ColumnVectorFromArray(const std::vector<double>& src) : _n(src.size()), _data(src.data()) {}

  int rows() const {return _n;}
  int cols() const {return 1;}
  double get(int i, int j) const {
    return _data[i];
  }
private:
  const int _n;
  const double* _data;
};

template <typename T>
int numel(const T& src) {
  return src.rows()*src.cols();
}

int computeIndex(int rows, int i, int j) {
  return i + rows*j;
}

class Matrix {
public:
  Matrix(int rows, int cols) : 
    _rows(rows), _cols(cols), 
    _storage(std::make_shared<std::vector<double>>(rows*cols)) {
    _data = _storage->data();
  }

  int rows() const {
    return _rows;
  } 

  int cols() const {
    return _cols;
  }
  
  void setElement(int i, int j, double value) {
    _data[computeIndex(_rows, i, j)] = value;
  }

  double get(int i, int j) const {
    return _data[computeIndex(_rows, i, j)];
  }
private:
  int _rows;
  int _cols;
  double* _data;
  std::shared_ptr<std::vector<double>> _storage;
};

template <typename T>
Matrix realize(const T& mat) {
  int rows = mat.rows();
  int cols = mat.cols();
  Matrix dst(rows, cols);
  for (int i = 0; i < rows; i++) {
    for (int j = 0; j < cols; j++) {
      dst.setElement(i, j, mat.get(i, j));
    }
  }
  return dst;
}

template <typename T>
class Reshape {
public:
  Reshape(const T& src, int rows) : _src(src), _rows(rows), _cols(src.rows()/rows) {}

  int rows() const {return _rows;}
  int cols() const {return _cols;}
  
  double get(int i, int j) const {
    return _src.get(i + j*_rows, 0);
  }
private:
  int _rows;
  int _cols;
  T _src;
};

template <typename T>
Reshape<T> reshape(const T& src, int rows) {
  return Reshape<T>(src, rows);
}

template <typename T>
class Transpose {
public:
  Transpose(const T& src) : _src(src) {}  
  int rows() const {return _src.cols();}
  int cols() const {return _src.rows();}
  double get(int i, int j) const {
    return _src.get(j, i);
  }
private:
  T _src;
};

template <typename T>
Transpose<T> transpose(const T& x) {
  return Transpose<T>(x);
}

template <typename A, typename B>
class SubMat {
public:
  SubMat(const A& a, const B& b) : _a(a), _b(b) {}

  int rows() const {return _a.rows();}
  int cols() const {return _a.cols();}
  
  double get(int i, int j) const {
    return _a.get(i, j) - _b.get(i, j);
  }
private:
  A _a;
  B _b;
};

template <typename A, typename B>
SubMat<A, B> subMat(const A& a, const B& b) {
  return SubMat<A, B>(a, b);
}


template <typename A, typename B>
class MulMat {
public:
  MulMat(const A& a, const B& b) : _a(a), _b(b) {}

  int rows() const {return _a.rows();}
  int cols() const {return _b.cols();}
  
  double get(int i, int j) const {
    double sum = 0.0;
    int n = _a.cols();
    for (int k = 0; k < n; k++) {
      sum += _a.get(i, k)*_b.get(k, j);
    }
    return sum;
  }
private:
  A _a;
  B _b;
};

template <typename A, typename B>
MulMat<A, B> mulMat(const A& a, const B& b) {
  return MulMat<A, B>(a, b);
}

class Ones {
public:
  Ones(int rows, int cols) : _rows(rows), _cols(cols) {}
  int rows() const {return _rows;}
  int cols() const {return _cols;}
  double get(int i, int j) const {return 1.0;}
private:
  int _rows;
  int _cols;
};

template <typename T>
class ScaleMat {
public:
  ScaleMat(double s, const T& x) : _s(s), _x(x) {}

  int rows() const {return _x.rows();}
  int cols() const {return _x.cols();}

  double get(int i, int j) const {
    return _s*_x.get(i, j);
  }
private:
  double _s;
  T _x;
};

template <typename T>
ScaleMat<T> scaleMat(double s, const T& src) {
  return ScaleMat<T>(s, src);
}

Matrix covarianceMatrix(int dim, const std::vector<double>& data) {
  ColumnVectorFromArray V(data);
  auto X = reshape(V, dim);
  int N = data.size()/dim;
  auto mu = realize(scaleMat(1.0/N, mulMat(Ones(1, N), transpose(X))));
  auto muRepeated = transpose(mulMat(Ones(N, 1), mu));
  auto Xc = subMat(X, muRepeated);
  auto covariance = scaleMat(1.0/(N - 1), mulMat(Xc, transpose(Xc)));
  return realize(covariance);
}

struct Problem {
  int dim = 0;
  std::vector<double> data;
};

struct Setup {
  Problem input(const nlohmann::json& src) const {
    Problem dst;
    dst.dim = src["dim"];
    for (auto x: src["data"]) {
      dst.data.push_back(x);
    }
    return dst;
  }
  
  Matrix compute(const Problem& src) const {
    return covarianceMatrix(src.dim, src.data);
  }

  nlohmann::json output(const Matrix& src) const {
    nlohmann::json dst;
    for (int i = 0; i < src.rows(); i++) {
      nlohmann::json row;
      for (int j = 0; j < src.cols(); j++) {
        row.push_back(src.get(i, j));
      }
      dst.push_back(row);
    }
    return dst;
  }
  
};


int main(int argc, const char** argv) {
  Setup setup;
  CHECK(3 == argc);
  bj::perform(setup, argv[1], argv[2]);
  return 0;
}
