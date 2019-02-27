package cljd;

public class CircleOpt {
    public double stepSize = 0.25;
    public double iterations = 30;

    public static double square(double x) {return x*x;}

    public double evaluate(double[] data, CircleParameters params) {
        int N = data.length/2;
        double sum = 0.0;
        for (int i = 0; i < N; i++) {
            int at = 2*i;
            double x = data[at + 0];
            double y = data[at + 1];

            double distToCentre = Math.sqrt(
                square(x - params.cx) + square(y - params.cy));
            double distToCircle = distToCentre - params.r;
            sum += square(distToCircle);
        }
        return (1.0/N)*sum;
    }

    public ADNumber evaluate(double[] data, ADCircleParameters params) {
        int N = data.length/2;
        ADNumber sum = ADNumber.constant(0.0);
        
        for (int i = 0; i < N; i++) {
            int at = 2*i;
            ADNumber x = ADNumber.constant(data[at + 0]);
            ADNumber y = ADNumber.constant(data[at + 1]);

            ADNumber distToCentre = x.sub(params.cx).square()
                .add(y.sub(params.cy).square())
                .sqrt();
            ADNumber distToCircle = distToCentre.sub(params.r);
            sum = sum.add(distToCircle.square());            
        }

        return sum.mul(ADNumber.constant(1.0/N));
    }

    private ADCircleParameters makeADParams(CircleParameters p, int which) {
        ADCircleParameters dst = new ADCircleParameters();
        dst.cx = new ADNumber(p.cx, which == 0? 1.0 : 0.0);
        dst.cy = new ADNumber(p.cy, which == 1? 1.0 : 0.0);
        dst.r = new ADNumber(p.r, which == 2? 1.0 : 0.0);
        return dst;
    }

    public CircleParameters gradient(
        double[] data, CircleParameters params) {
        ADCircleParameters adp = new ADCircleParameters();

        CircleParameters result = new CircleParameters();
        result.cx = evaluate(data, makeADParams(params, 0)).getDerivative();
        result.cy = evaluate(data, makeADParams(params, 1)).getDerivative();
        result.r = evaluate(data, makeADParams(params, 2)).getDerivative();
        return result;
    }

    public CircleParameters optimize(
        double[] data, CircleParameters p0) {
        CircleParameters p = p0;
        for (int i = 0; i < iterations; i++) {
            p = p.add(gradient(data, p).mul(-stepSize));
        }
        return p;
    }
};
