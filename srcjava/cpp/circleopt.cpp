#include <iostream>
#include "common.h"
#include "benjmark.h"
#include <array>


typedef std::array<double, 2> Vec2d;

template <typename T>
struct Parameters {
  T cx;
  T cy;
  T r;
};

template <typename T>
std::ostream& operator<<(std::ostream& s, const Parameters<T>& p) {
  s << "{cx=" << p.cx << ", cy=" << p.cy << ", r=" << p.r << "}";
  return s;
}

struct Settings {
  double step_size = 0.0;
  int iterations = 0;
};

struct Problem {
  Settings settings;
  Parameters<double> parameters;
  std::vector<Vec2d> points;
};


template <typename T>
Parameters<T> operator+(const Parameters<T>& a, const Parameters<T>& b) {
  return {a.cx + b.cx, a.cy + b.cy, a.r + b.r};
}

template <typename T>
Parameters<T> operator*(T s, const Parameters<T>& b) {
  return {s*b.cx, s*b.cy, s*b.r};
}

class ADNumber {
public:
  ADNumber() : _value(0.0), _deriv(0.0) {}
  ADNumber(double value, double deriv = 0.0) : _value(value), _deriv(deriv) {}

  static ADNumber variable(double value) {
    return ADNumber(value, 1.0);
  }

  double getValue() const {return _value;}
  double getDeriv() const {return _deriv;}

  double _value = 0.0;
  double _deriv = 0.0;
};

std::ostream& operator<<(std::ostream& s, const ADNumber& x) {
  s << "{ value=" << x._value << ", deriv=" << x._deriv << "}";
  return s;
}

ADNumber sqrt(const ADNumber& x) {
  double s = sqrt(x._value);
  return ADNumber(s, (0.5/s)*x._deriv);
}

ADNumber operator+(const ADNumber& a, const ADNumber& b) {
  return ADNumber(a._value + b._value, a._deriv + b._deriv);
}

ADNumber operator-(const ADNumber& a, const ADNumber& b) {
  return ADNumber(a._value - b._value, a._deriv - b._deriv);
}

ADNumber operator*(const ADNumber& a, const ADNumber& b) {
  return ADNumber(a._value*b._value, a._value*b._deriv + a._deriv*b._value);
}

ADNumber square(ADNumber x) {
  return x*x;
}

ADNumber evaluatePoint(const Vec2d& pt, const Parameters<ADNumber>& params) {
  ADNumber distToCentre = sqrt(square(pt[0] - params.cx) + square(pt[1] - params.cy));
  ADNumber distToCircle = distToCentre - params.r;
  return square(distToCircle);
}

ADNumber evaluateCircleFitness(const std::vector<Vec2d>& points, const Parameters<ADNumber>& params) {
  ADNumber sum = 0.0;
  for (const auto& pt: points) {
    sum = sum + evaluatePoint(pt, params);
  }
  return (1.0/points.size())*sum;
}

Parameters<ADNumber> params2ad(const Parameters<double>& src, int which) {
  Parameters<ADNumber> dst;
  dst.cx = ADNumber(src.cx, which == 0? 1.0 : 0.0);
  dst.cy = ADNumber(src.cy, which == 1? 1.0 : 0.0);
  dst.r = ADNumber(src.r, which == 2? 1.0 : 0.0);
  return dst;
}

Parameters<double> gradient(const std::vector<Vec2d>& points, const Parameters<double>& params) {
  Parameters<double> result;
  result.cx = evaluateCircleFitness(points, params2ad(params, 0))._deriv;
  result.cy = evaluateCircleFitness(points, params2ad(params, 1))._deriv;
  result.r = evaluateCircleFitness(points, params2ad(params, 2))._deriv;
  return result;
}

Parameters<double> takeStep(
  const std::vector<Vec2d>& points, 
  const Parameters<double>& params,
  double step_size) {
  auto g = gradient(points, params);
  return params + ((-step_size)*g);
}

Parameters<double> optimize(const Problem& problem) {
  Parameters<double> params = problem.parameters;
  for (int i = 0; i < problem.settings.iterations; i++) {
    params = takeStep(problem.points, params, problem.settings.step_size);
  }
  return params;
}

struct Setup {
  Problem input(const nlohmann::json& src) const {
    Problem dst;
    dst.settings.iterations = src["settings"]["iterations"];
    dst.settings.step_size = src["settings"]["step-size"];

    auto ip = src["init-params"];
    dst.parameters.cx = ip["cx"];
    dst.parameters.cy = ip["cy"];
    dst.parameters.r = ip["r"];

    auto pts = src["points"];
    for (const auto& pt: pts) {
      dst.points.push_back({pt[0], pt[1]});
    }
    
    return dst;
  }

  Parameters<double> compute(const Problem& problem) const {
    return optimize(problem);
      //return gradient(problem.points, problem.parameters);
  }

  nlohmann::json output(const Parameters<double>& results) const {
    nlohmann::json dst;
    dst["cx"] = results.cx;
    dst["cy"] = results.cy;
    dst["r"] = results.r;
    return dst;
  }
};

int main(int argc, const char** argv) {
  CHECK(3 == argc);
  std::string input_file = argv[1];
  std::string output_file = argv[2];

  Setup setup;
  bj::perform(setup, input_file, output_file);
  
  return 0;
}
