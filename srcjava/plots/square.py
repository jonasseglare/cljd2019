import matplotlib
import matplotlib.pyplot as plt
import numpy as np

# https://matplotlib.org/users/usetex.html

X = np.linspace(-5, 5, 100);
Y = np.square(X);

lw = 2;
fs = 14;
font = {'family' : 'normal',
        'weight' : 'normal',
        'size'   : fs}

matplotlib.rc('font', **font)

#plt.rc('text', usetex=True)
#plt.rc('font', family='serif')


plt.plot(X, Y, 'r');
plt.xlabel('x');
plt.ylabel(r'$x^2$');
plt.savefig('../latex/images/square.pdf');
plt.show();
