## Running

### Start the CLJS builds and REPL
1. From emacs run `cider-jack-in-cljs`
2. select `shadow-cljs`
3. open http://localhost:9630/builds
4. select the main build

### Start the CLJ server 
1. From emacs run `cider-jack-in-clj`
2. select `clojure-cli`
3. select `y` for a sibling REPL
4. Evaluate or run `(restart)`
5. Navigate to http://localhost:3000/index.html
  
### Troubleshooting
#### Emacs can't connect to the clj repl
check out the `~/.dir-locals.el` file, it specifies the default environment that cider jack in should use. 

