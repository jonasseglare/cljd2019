/* The Computer Language Benchmarks Game
   https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

   contributed by Mark C. Lewis
   modified slightly by Chad Whipkey
   modified slightly by Stefan Feldbinder
   modified slightly by Tagir Valeev

   modified by Jonas Östlund to be testable from Clojure
*/

public final class NBodySystem {
   private static final int LENGTH = 5;

   private Body[] bodies;

   public NBodySystem(){
      bodies = new Body[]{
            Body.sun(),
            Body.jupiter(),
            Body.saturn(),
            Body.uranus(),
            Body.neptune()
         };

      double px = 0.0;
      double py = 0.0;
      double pz = 0.0;
      for(int i=0; i < LENGTH; ++i) {
         px += bodies[i].vx * bodies[i].mass;
         py += bodies[i].vy * bodies[i].mass;
         pz += bodies[i].vz * bodies[i].mass;
      }
      bodies[0].offsetMomentum(px,py,pz);
   }

   public void advance(double dt) {
      Body[] b = bodies;
      for(int i=0; i < LENGTH-1; ++i) {
         Body iBody = b[i];
         double iMass = iBody.mass;
         double ix = iBody.x, iy = iBody.y, iz = iBody.z;

         for(int j=i+1; j < LENGTH; ++j) {
            Body jBody = b[j];
            double dx = ix - jBody.x;
            double dy = iy - jBody.y;
            double dz = iz - jBody.z;

            double dSquared = dx * dx + dy * dy + dz * dz;
            double distance = Math.sqrt(dSquared);
            double mag = dt / (dSquared * distance);

            double jMass = jBody.mass;

            iBody.vx -= dx * jMass * mag;
            iBody.vy -= dy * jMass * mag;
            iBody.vz -= dz * jMass * mag;

            jBody.vx += dx * iMass * mag;
            jBody.vy += dy * iMass * mag;
            jBody.vz += dz * iMass * mag;
         }
      }

      for(int i=0; i < LENGTH; ++i) {
         Body body = b[i];
         body.x += dt * body.vx;
         body.y += dt * body.vy;
         body.z += dt * body.vz;
      }
   }

    public void iterate(int iterations, double dt) {
        for (int i = 0; i < iterations; i++) {
            advance(dt);
        }
    }

   public double energy(){
      double dx, dy, dz, distance;
      double e = 0.0;

      for (int i=0; i < bodies.length; ++i) {
         Body iBody = bodies[i];
         e += 0.5 * iBody.mass *
            ( iBody.vx * iBody.vx
                + iBody.vy * iBody.vy
                + iBody.vz * iBody.vz );

         for (int j=i+1; j < bodies.length; ++j) {
            Body jBody = bodies[j];
            dx = iBody.x - jBody.x;
            dy = iBody.y - jBody.y;
            dz = iBody.z - jBody.z;

            distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
            e -= (iBody.mass * jBody.mass) / distance;
         }
      }
      return e;
   }
}
