import pyrsistent as pyr
import json
import matplotlib
import matplotlib.pyplot as plt
import math

default_settings = pyr.pmap({
    "logx": True,
    "logy": True,
    "outputprefix": "/tmp/benjmark",
    "interactive": False,
    "fontsize": 12,
    "ylabel": 'Duration (s)',
    "xlabel": 'Data size',
    "sizeformat": "Data size {:d}"
})

def load_json_data(filename):
    with open(filename) as f:
        return json.load(f);

def load_candidates(root):
    return load_json_data(root + "/candidates.json");

def load_results(root):
    return load_json_data(root + "/results.json");

def load_problems(root):
    info = load_json_data(root + "/probleminfo.json");
    results = pyr.pvector();
    for i in range(0, info["count"]):
        prob = load_json_data(root + '/problem{:04d}.json'.format(i));
        results = results.append({'index': i, 'size': prob["size"]})
    return results

def make_per_problem_map(results, problems):
    top = {};
    for r in results:
        problem_index = r['problem-index'];
        cand_key = r['cand-key'];
        time_seconds = r['time-seconds'];
        problem = problems[problem_index];
        problem_size = problem['size'];
        sub_map = {};
        probkey = problem_index;
        if probkey in top:
            sub_map = top[probkey];
        sub_map[cand_key] = time_seconds;
        top[probkey] = sub_map;
    return top

def make_plot():
    fig = plt.figure();
    ax = fig.add_subplot(1, 1, 1);
    return (fig, ax);

def finish_plot(ax, settings):
    if settings['interactive']:
        plt.show();

def basic_format(x):
    if x <= 0:
        return '0'

    level = math.floor(math.log10(x));
    if level < 1:
        return ("{:." + str(1-level) + "f}").format(x);
    else:
        return "{:d}".format(round(x));
        

def render_barplots(keys, root, settings):
    candidates = load_candidates(root);
    results = load_results(root);
    problems = load_problems(root);

    for key in keys:
        assert(key in candidates);

    ppm = make_per_problem_map(results, problems);
    print(ppm)
    
    for prob in problems:
        leftmost = None;
        fig, ax = make_plot();
        
        problem_index = prob['index']
        problem_size = prob['size']
        m = ppm[problem_index];

        X = [];
        Y = [];
        labels = [];

        for k in keys:
            if k in m:
                X.append(len(X) + 1);
                dur = m[k];
                if leftmost == None:
                    leftmost = dur;
                Y.append(dur);
                labels.append(candidates[k]['name']);

        plt.bar(X, Y, tick_label=labels, log=settings['logy']);


        fs = settings['fontsize'];
        n = len(X);
        if 0 < leftmost:
            for i in range(0, n):
                x = X[i]
                y = Y[i]
                yp = 3*y;
                ax.plot([x], [yp], alpha=0.0);
                ax.text(x, yp, basic_format(y/leftmost) + "×\n" 
                        + basic_format(y) + " s", 
                        fontsize=fs,
                        horizontalalignment='center',
                        verticalalignment='top'
                );

                #ax.xaxis.set_tick_params(fontsize=settings['fontsize'])
        ax.tick_params(labelsize=fs)
        plt.title(settings['sizeformat'].format(problem_size));
        plt.ylabel(settings['ylabel'], fontsize=fs);
        fig.savefig(settings['outputprefix'] + 'bars{:04d}.pdf'.format(problem_index));
        finish_plot(ax, settings);
    
def get_sizes_and_times(results, problems, cand_key):
    pairs = [];
    for r in results:
        pi = r['problem-index'];
        problem = problems[pi];
        if r['cand-key'] == cand_key:
            pairs.append([problem['size'], r['time-seconds']]);
    pairs.sort(key=lambda pair: pair[0]);
    X = [];
    Y = [];
    for p in pairs:
        X.append(p[0])
        Y.append(p[1]);
    return (X, Y);


def render_lineplot(keys, root, settings):
    fs = settings['fontsize']
    results = load_results(root);
    problems = load_problems(root);
    cands = load_candidates(root);
    fig, ax = make_plot();
    if settings['logx']:
        ax.set_xscale('log');
    if settings['logy']:
        ax.set_yscale('log');
    for k in reversed(keys):
        assert(k in cands)
        X, Y = get_sizes_and_times(results, problems, k);
        label = cands[k]['name']
        ax.plot(X, Y, label=label);
    ax.legend(prop={'size': fs})
    ax.tick_params(labelsize=fs);
    plt.ylabel(settings['ylabel']);
    plt.xlabel(settings['xlabel']);
    finish_plot(ax, settings);
    fig.savefig(settings['outputprefix'] + "lineplot.pdf");
        
        
