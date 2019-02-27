import pyrsistent as pyr
import json
import matplotlib
import matplotlib.pyplot as plt
import math
from mpl_toolkits.mplot3d import Axes3D

states = None;
with open('../benchmarks/nbody/stateseq.json') as f:
    states = json.load(f);

def make_plot():
    fig = plt.figure();
    ax = fig.add_subplot(111, projection='3d');
    #ax = plt.axes(projection='3d')
    return (fig, ax);

fs = 12;
ms = 60;
lw = 1;

def render_planets(ax, state):
    X = [];
    Y = [];
    Z = [];
    for k in state:
        body = state[k]
        pos = body['pos'];
        x = pos[0];
        y = pos[1];
        z = pos[2];
        X.append(x);
        Y.append(y);
        Z.append(z);
        ax.text(x, y, z, k.capitalize(), fontsize=fs)
    ax.scatter(X, Y, Z, color='blue', s=ms)

def render_connections(ax, state):
    for i in state:
        a = state[i];
        for j in state:
            b = state[j];
            
            apos = a['pos']
            bpos = b['pos']
            
            X = [apos[0], bpos[0]]
            Y = [apos[1], bpos[1]]
            Z = [apos[2], bpos[2]]

            ax.plot(X, Y, Z, color='red', linewidth=lw)

def render_velocities(ax, state, alpha):
    for k in state:
        body = state[k]
        pos = body['pos']
        vel = body['vel']
        X = [pos[0], pos[0] + alpha*vel[0]];
        Y = [pos[1], pos[1] + alpha*vel[1]];
        Z = [pos[2], pos[2] + alpha*vel[2]];
        ax.plot(X, Y, Z, color='red', linewidth=lw);

def render_trajectories(ax, states):
    first_state = states[0];
    for k in first_state:
        X = [];
        Y = [];
        Z = [];
        for state in states:
            body = state[k]
            p = body['pos']
            X.append(p[0])
            Y.append(p[1])
            Z.append(p[2])
        ax.plot(X, Y, Z, color='blue', linewidth=lw)

def set_view(ax):
    ax.view_init(30, 30)

outputpath = '../latex/images/nbody/'

### Main code
last_state = states[len(states)-1]

def trajectory_plot():
    fig, ax = make_plot()
    render_trajectories(ax, states)
    render_planets(ax, last_state)
    set_view(ax);
    fig.savefig(outputpath + 'trajectories.pdf');

def pair_plot():
    fig, ax = make_plot()
    render_connections(ax, last_state)
    render_planets(ax, last_state)
    set_view(ax);
    fig.savefig(outputpath + 'pairs.pdf');

def velocity_plot():
    fig, ax = make_plot()
    render_trajectories(ax, states)
    render_planets(ax, last_state)
    render_velocities(ax, last_state, 2.0)
    set_view(ax);
    fig.savefig(outputpath + 'velocities.pdf');

trajectory_plot()
pair_plot()
velocity_plot()
