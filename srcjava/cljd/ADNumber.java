package cljd;

public class ADNumber {
    private final double _value;
    private final double _deriv;

    public ADNumber(double value, double deriv) {
        _value = value;
        _deriv = deriv;
    }

    public static ADNumber variable(double value) {
        return new ADNumber(value, 1.0);
    }

    public static ADNumber constant(double value) {
        return new ADNumber(value, 0.0);
    }

    public ADNumber add(ADNumber other) {
        return new ADNumber(
            _value + other._value,
            _deriv + other._deriv);
    }

    public ADNumber sub(ADNumber other) {
        return new ADNumber(
            _value - other._value,
            _deriv - other._deriv);
    }

    public ADNumber mul(ADNumber other) {
        return new ADNumber(
            _value*other._value,
            _value*other._deriv + _deriv*other._value);
    }

    public ADNumber sqrt() {
        double s = Math.sqrt(_value);
        return new ADNumber(s, (0.5/s)*_deriv);
    }

    public ADNumber square() {
        return mul(this);
    }

    public double getValue() {return _value;}
    public double getDerivative() {return _deriv;}
};
