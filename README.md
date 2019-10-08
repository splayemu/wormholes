Personal Fulcro template derived from the (Fulcro Getting Started Guide)[http://book.fulcrologic.com/fulcro3/#_getting_started].

## Running

1. Start the CLJS builds and REPL
  a. From emacs run `cider-jack-in-cljs`
  b. select `shadow-cljs`
  c. open http://localhost:9630/builds
  d. select the main build
2. Start the CLJ server 
  a. From emacs run `cider-jack-in-clj`
  b. select `clojure-cli`
  c. select `y` for a sibling REPL
  d. Evaluate or run `(restart)`
  3. Navigate to http://localhost:3000/index.html
  
### Troubleshooting
#### Emacs can't connect to the clj repl
check out the `~/.dir-locals.el` file, it specifies the default environment that cider jack in should use. 

