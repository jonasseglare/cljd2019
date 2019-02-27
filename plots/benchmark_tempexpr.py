import benjmark

keys = ["cpp", "geex", "java", "clojure"];
#keys = ["geex", "java"];
#keys = ["cpp", ""java", "clojure"];
#keys = ["cpp", "geex", "clojure"];

root = "../benchmarks/tempexpr";

settings = benjmark.default_settings.set('outputprefix', '../latex/images/benchmarks/tempexpr').set('sizeformat', '{:d} points').set('xlabel', 'Number of vectors').set('logy', True).set('logx', True);

benjmark.render_barplots(keys, root, settings);
benjmark.render_lineplot(keys, root, settings);
