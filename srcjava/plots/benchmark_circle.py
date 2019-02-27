import benjmark

keys = ["cpp", "geex", "java", "clojure"];
#keys = ["geex", "java"];

circle_root = "../benchmarks/circle";

circle_settings = benjmark.default_settings.set('outputprefix', '../latex/images/benchmarks/circle').set('sizeformat', '{:d} points').set('xlabel', 'Number of points').set('ylabel', 'Computation time (seconds)');

benjmark.render_barplots(keys, circle_root, circle_settings);

benjmark.render_lineplot(keys, circle_root, circle_settings);
