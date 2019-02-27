import matplotlib
import matplotlib.pyplot as plt
import numpy as np
import pyrsistent as pyr
import json
import math

###################################### Functions

default_settings = pyr.pmap({
    'color': 'g',
    'pointcolor': "grey",
    'annotcolor': 'black',
    'linewidth': 2,
    'markersize': 10,
    'fontsize': 12,
    'interactive': False,
    'costangle': math.pi/4.0,
    'costfmt': '{:0.1f}',
    'outputpath': '../latex/images/circle',
    'head_width': 0.1,
    'step_size': 0.5,
    'min_arrow_length': 0.2,
    'iterations': 16
});

font = {'family' : 'normal',
        'weight' : 'normal',
        'size'   : default_settings['fontsize']};

matplotlib.rc('font', **font)


def split_coords(coords):
    n = len(coords);
    if n == 0:
        return None;

    dim = len(coords[0]);
    dst = [];
    for i in range(0, dim):
        X = [];
        for j in range(0, n):
            X.append(coords[j][i]);
        dst.append(X);
    return dst;

def plot_points(ax, points, settings):
    s = split_coords(points);
    ax.plot(s[0], s[1], 'o', color=settings['pointcolor']);

def plot_circle_cost(ax, params, settings):
    cx = params['cx'];
    cy = params['cy'];
    r = params['r'];
    angle = settings['costangle'];
    x = cx + r*math.cos(angle);
    y = cy + r*math.sin(angle);
    ax.text(x, y, "cost=" + settings['costfmt'].format(params['cost']), 
            fontsize=settings['fontsize'],
            color=settings['color'])

def plot_circle(ax, params, settings):
    n = 100;
    X = [];
    Y = [];
    step = 2.0*math.pi/(n - 1);
    cx = params['cx'];
    cy = params['cy'];
    r = params['r'];
    for i in range(0, n):
        angle = i*step;
        X.append(cx + r*math.cos(angle));
        Y.append(cy + r*math.sin(angle));
    color = settings.get('circlecolor');
    if color == None:
        return ax.plot(X, Y);
    else:
        return ax.plot(X, Y, color=color);

def point_str(xlab, ylab, xval, yval, with_values):
    base = '(' + xlab + ', ' + ylab + ')';
    if with_values:
        return '$' + base + ' = ({:0.1f}, {:0.1f})$'.format(xval, yval);
    else:
        return '$' + base + '$';

def val_str(lab, val, with_values):
    if with_values:
        return '$'+lab+' = {:0.1f}$'.format(val);
    else:
        return '$'+lab+'$';

def plot_circle_center(ax, params, settings):
    color = settings.get('annotcolor');
    cx = params['cx'];
    cy = params['cy'];
    ax.plot([cx], [cy], 'o', color=color);

def plot_position_gradient(ax, params, settings):
    cx = params['cx'];
    cy = params['cy'];
    grad = params['gradient'];
    gx = grad['cx'];
    gy = grad['cy'];
    color = settings['annotcolor'];
    ax.text(cx+gx + 0.1, cy+gy, '$(\dfrac{\partial f}{\partial c_x}, \dfrac{\partial f}{\partial c_y})$', 
            color=color);
    k = 1.3;
    ax.plot([cx+k*gx], [cy+k*gy], alpha=0.0);
    ax.arrow(cx, cy, gx, gy, color=color, 
             #width=0.025,
             head_width=settings['head_width']
    );

def plot_position_step(ax, params, settings, show_text):
    cx = params['cx'];
    cy = params['cy'];
    grad = params['gradient'];
    alpha = settings['step_size'];
    gx = -alpha*grad['cx'];
    gy = -alpha*grad['cy'];

    if math.sqrt(gx*gx + gy*gy) < settings['min_arrow_length']:
        return;

    color = settings['annotcolor'];
    if show_text:
        ax.text(cx+gx + 0.1, cy+gy, 
                '$- k (\dfrac{\partial f}{\partial c_x}, \dfrac{\partial f}{\partial c_y})$', 
                color=color);
    ax.arrow(cx, cy, gx, gy, color=color, 
             #width=0.025,
             head_width=settings['head_width']
    );
    

def plot_circle_params(ax, params, settings, with_values):
    cx = params['cx'];
    cy = params['cy'];
    r = params['r'];
    hr = 0.5*r;
    angle = math.pi/3.0;
    sin = math.sin(angle);
    cos = math.cos(angle);
    to = 0.15*r;
    color = settings['annotcolor'];
    ax.plot([cx, cx + r*cos], [cy, cy + r*sin], color=color);
    #plot_circle_center(ax, params, settings);
    laby = cy - to;
    ax.text(cx, laby, point_str('c_x', 'c_y', cx, cy, with_values),
            horizontalalignment='center', color=color);
    rx = cx + hr*cos + 0.05*r;
    ry = cy + hr*sin;
    ax.text(rx, ry, val_str('r', r, with_values), color=color);

def plot_circle_with_cost(ax, params, settings):
    [circle] = plot_circle(ax, params, settings);
    plot_circle_cost(ax, params, settings.set('color', circle.get_color()));

def finish_plot(ax, settings):
    ax.axis('equal');
    if settings['interactive']:
        plt.show();

def make_plot():
    fig = plt.figure();
    ax = fig.add_subplot(1, 1, 1);
    return (fig, ax);

def plot_sample_cost(data, settings, sample):
    fig, ax = make_plot();
    plot_points(ax, data['points'], settings);
    plot_circle_with_cost(ax, sample, settings);
    finish_plot(ax, settings);
    return fig;
    

def filename_generator(fmt):
    filename_generator.counter = 0;
    def generate():
        filename = fmt.format(filename_generator.counter);
        filename_generator.counter += 1;
        return filename;
    return generate;
        


def optimization_illustrations(data, settings, inds):
    gen = filename_generator(settings['outputpath'] + '/optillu{:02d}.pdf');
    for i in inds:
        sample = data['samples'][i];
        plot_sample_cost(data, settings, sample).savefig(gen());
    plot_sample_cost(data, settings, data['opt_params']).savefig(gen());
    

def noninteractive(s):
    return s.set('interactive', False);

def point_illustration(data, settings):
    fig, ax = make_plot();
    plot_points(ax, data['points'], settings);
    finish_plot(ax, settings);
    fig.savefig(settings['outputpath'] + '/problem.pdf');
    
def point_and_circle_illustration(data, settings0):
    fig, ax = make_plot();
    settings = settings0.set('circlecolor', 'blue').set('color', 'black');
    plot_points(ax, data['points'], settings);
    params = data['opt_params'];
    plot_circle(ax, params, settings);
    plot_circle_params(ax, params, settings, True);
    finish_plot(ax, settings);
    fig.savefig(settings['outputpath'] + '/problemsolved.pdf');


def problem_illustrations(data, settings):
    point_illustration(data, settings);
    point_and_circle_illustration(data, settings);


def opt_sample_final(opt_sample):
    n = len(opt_sample);
    return opt_sample[n-1]

def opt_sample_initial(opt_sample):
    return opt_sample[0]

def opt_sample_cost(opt_sample):
    return opt_sample_final(opt_sample)['cost'];

max_cost = 0.1;

def check_opt_samples(data, settings):
    opt_samples = data['opt_samples'];
    for sample in opt_samples:
        cost = opt_sample_cost(sample);
        if max_cost < cost:
            print('Cost is high!!!');

def eval_opt_sample_suitability(opt_sample):
    if opt_sample_cost(opt_sample) < max_cost:
        first = opt_sample[0];
        return first['cost'];
    else:
         return 0.0;   

def select_opt_sample(a, b):
    if a == None:
        return b;

    ac = eval_opt_sample_suitability(a);
    bc = eval_opt_sample_suitability(b);
    print('Best cost = {:0.2f} Candidate cost = {:0.2f}'.format(ac, bc))
    if ac < bc:
        return b;
    else:
        return a;

def select_good_opt_sample(data):
    return data['opt_samples'][1];

    points = data['points'];
    params = data['params'];
    best = None;
    for sample in data['opt_samples']:
        best = select_opt_sample(best, sample);
    return best;
    
def eval_pt(d, x):
    return d[0]*x[0] + d[1]*x[1];

def select_point(d, a, b):
    if a == None:
        return b;

    if eval_pt(d, a) < eval_pt(d, b):
        return b;
    else:
        return a;



def select_good_point(data, opt_sample):
    init = opt_sample_initial(opt_sample);
    params = data['params'];
    dx = params['cx'] - init['cx'];
    dy = params['cy'] - init['cy'];
    best = None;
    for pt in data['points']:
        best = select_point([dx, dy], best, pt);
    return best;

def error_line(ax, params, pt):
    ptx = pt[0];
    pty = pt[1];
    cx = params['cx'];
    cy = params['cy'];
    dx = ptx - cx;
    dy = pty - cy;
    f = 1.0/math.sqrt(dx*dx + dy*dy);
    rf = f*params['r'];
    x = cx + rf*dx;
    y = cy + rf*dy;
    ax.plot([x, ptx], [y, pty], 'red');

def plot_single_point(data, settings):
    opt_sample = select_good_opt_sample(data);
    params = opt_sample[0];
    pt = select_good_point(data, opt_sample);

    fig, ax = make_plot();
    plot_points(ax, data['points'], settings);
    plot_circle(ax, params, settings);
    plot_circle_params(ax, params, settings, False);
    ptx = pt[0];
    pty = pt[1];
    ax.plot([ptx], [pty], 'o', color='black');
    ax.text(ptx - 0.2, pty - 0.1, '$(x_i, y_i)$',#'$(x_i, y_i) = ({:0.1f}, {:0.1f})$'.format(ptx, pty), 
            color='black', horizontalalignment='center',
            verticalalignment='top');
    error_line(ax, params, pt);
    finish_plot(ax, settings);
    fig.savefig(settings['outputpath'] + '/singlepoint.pdf');
    
def plot_multiple_points(data, settings):
    opt_sample = select_good_opt_sample(data);
    params = opt_sample[0];
    points = data['points'];

    fig, ax = make_plot();
    plot_points(ax, points, settings);
    plot_circle(ax, params, settings);
    plot_circle_params(ax, params, settings, False);
    for pt in points:
        error_line(ax, params, pt);
    finish_plot(ax, settings);
    fig.savefig(settings['outputpath'] + '/multiplepoint.pdf');

def gradient_illustration(data, settings):
    points = data['points'];
    opt_sample = select_good_opt_sample(data);
    params = opt_sample[0];

    fig, ax = make_plot();
    
    plot_points(ax, points, settings);
    plot_circle(ax, params, settings);
    #plot_circle_center(ax, params, settings);
    plot_position_gradient(ax, params, settings);
    
    finish_plot(ax, settings);
    fig.savefig(settings['outputpath'] + '/gradient.pdf');

def step_illustration(data, settings):
    points = data['points'];
    opt_sample = select_good_opt_sample(data);
    params = opt_sample[0];

    fig, ax = make_plot();
    
    plot_points(ax, points, settings);
    plot_circle(ax, params, settings);
    #plot_circle_center(ax, params, settings);
    plot_position_step(ax, params, settings, True);
    finish_plot(ax, settings);
    fig.savefig(settings['outputpath'] + '/step.pdf');

def gradient_descent(data, settings):
    settings = settings.set('costfmt', '{:0.3f}');
    gen = filename_generator(settings['outputpath'] + '/descent{:02d}.pdf');
    points = data['points'];
    opt_sample = select_good_opt_sample(data)[0:settings['iterations']];
    for s in opt_sample:
        print('The cost is: ' + str(s['cost']));

    for params in opt_sample:
        fig, ax = make_plot();
        plot_points(ax, points, settings);
        plot_circle_with_cost(ax, params, settings);
        plot_position_step(ax, params, settings, False);
        finish_plot(ax, settings);
        fig.savefig(gen());
    


def select_best(a, b):
    if a == None:
        return b;
    elif a['cost'] > b['cost']:
        return b;
    return a;

def naive_opt_illustration(data, settings, n):
    fig, ax = make_plot();
    
    samples = data['samples'];
    
    circle_settings = settings.set('circlecolor', 'lightgray');

    best = None;
    for i in range(0, n):
        params = samples[i];
        plot_circle(ax, params, circle_settings);
        
        best = select_best(best, params);
    plot_points(ax, data['points'], settings.set('pointcolor', 'blue'));
    plot_circle_with_cost(ax, best, 
                          settings
                          .set('circlecolor', 'red')
                          .set('costfmt', '{:0.2f}'));

    finish_plot(ax, settings);
    return fig;
        
def naive_opt_illustrations(data, settings, counts):
    gen = filename_generator(settings['outputpath'] + '/naiveopt{:02d}.pdf');
    for n in counts:
        naive_opt_illustration(data, settings, n).savefig(gen());


###################################### The code

with open('../circledata/samples.json') as f:
    data = json.load(f)
check_opt_samples(data, default_settings);
#print(select_good_opt_sample(data)[0]);

## Just showing the problem we want to sovle
problem_illustrations(data, default_settings);

## Showing what we mean by optimization
optimization_illustrations(data, default_settings, [1, 2, 3, 4]);

## Showing the cost for a single point
plot_single_point(data, default_settings);

## Showing the costs of multiple points
plot_multiple_points(data, default_settings);

## How naive optimization would work
naive_opt_illustrations(data, default_settings, [1, 5, 10, 20, 40, 80, 100]);

## What the gradient looks like
gradient_illustration(data, default_settings);

## What the step looks like
step_illustration(data, default_settings);

## What gradient descent looks like
gradient_descent(data, default_settings);

