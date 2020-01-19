## Development

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

## Release

This project is hosted on a DigitalOcean droplet in a dokku container. You need to add the your ssh keys to DigitalOcean in order to release.

The droplet's IP address is: 159.65.67.95

### Setup

1. Add SSH Key to DigitalOcean
2. Add droplet/dokku remote to your computer
```
git remote add dokku dokku@159.65.67.95:game
```
3. add alias dokku="ssh -t dokku@deploy.yourdomain.tld" to your .bashrc/.zshrc
4. Open the firewall on the box to the application port if it's the first time deploying this application
```
ssh root@159.65.67.95
sudo ufw allow 37481
```

#### Resources
(dokku on digitalocean)[https://codewithhugo.com/deployment-options-netlify-dokku-on-digitalocean-vs-now.sh-github-pages-heroku-and-aws/]

### Release

1. Push to our dokku droplet
```
git push dokku master
```
