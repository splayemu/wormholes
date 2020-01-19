## Hosting and Deployment

Where to deploy the game?

I wanted something easy and I had heard of dokku. Dokku is a personal PaSS, platform as a service, that you can throw onto any VPS (debian based?).

It makes it easy to host and deploy the project as it follows heruku's git deploy model.

Simply add the box as a git remote and you can push your code to it. It will build and deploy your code right there.

I went with digitalocean to host my dokku instance.

The sample app built from the leiningen heroku template:
```
lein new heroku my-heroku-app
```

Worked completely out of the box with dokku. This gave me false confidence.

Deploying my own webapp was slightly more complicated. Heroku and Dokku use buildpacks which Heroku wrote to deploy apps easier. They are a framework for deploying certain kinds of apps on the Heroku infrastructure. Dokku also builds your images with buildpacks.

Trying to bulid my app with a buildpack didn't work out of the box for two reasons:
- we also have a `package.json` file which dokku uses to interpret that we have a node.js project
- we don't have a `project.clj` file because we are just using clojure tools-deps

I went down the dockerfile path as dokku also supports that natively. It took longer than expected as I am not very familiar with docker.

### Monitoring

One thing I am thinking about is monitoring. It's good for a couple of reasons: to track how popular the game is and to understand the usage of the components.

It might be sufficient to have the monitoring on the same dokku vps as the game, but obviously if the VPS goes down, we won't be able to check the monitoring.
