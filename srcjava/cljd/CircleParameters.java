package cljd;

public class CircleParameters {
    public double cx = 0.0;
    public double cy = 0.0;
    public double r = 0.0;

    public CircleParameters mul(double s) {
        CircleParameters dst = new CircleParameters();
        dst.cx = s*cx;
        dst.cy = s*cy;
        dst.r = s*r;
        return dst;
    }

    public CircleParameters add(CircleParameters x) {
        CircleParameters dst = new CircleParameters();
        dst.cx = cx + x.cx;
        dst.cy = cy + x.cy;
        dst.r = r + x.r;
        return dst;
    }
};
