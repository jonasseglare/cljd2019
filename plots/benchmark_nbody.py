import benjmark

keys = ["cpp", "geex", "java", "clojure"];
#keys = ["geex", "java"];
#keys = ["cpp", ""java", "clojure"];

root = "../benchmarks/nbody";

settings = benjmark.default_settings.set('outputprefix', '../latex/images/benchmarks/nbody').set('sizeformat', '{:d} iterations').set('xlabel', 'Number of iterations').set('logy', False).set('logx', False);

benjmark.render_barplots(keys, root, settings);
benjmark.render_lineplot(keys, root, settings);
