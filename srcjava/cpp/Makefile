circleopt: circleopt.cpp benjmark.o
	g++ circleopt.cpp benjmark.o -std=c++11 -O3 -o circleopt

tempexpr: tempexpr.cpp benjmark.o
	g++ tempexpr.cpp benjmark.o -std=c++11 -O3 -o tempexpr

benjmark.o: benjmark.h benjmark.cpp
	g++ benjmark.cpp -std=c++11 -O3 -c

/tmp/circleout.json: circleopt
	./circleopt ../benchmarks/circle/problem0000.json /tmp/circleout.json

nbody: nbody.gpp-3main.cpp benjmark.o
	g++ nbody.gpp-3main.cpp benjmark.o -fomit-frame-pointer -march=native -mfpmath=sse -msse3 -std=c++11 -O3 -o nbody
