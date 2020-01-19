FROM clojure:openjdk-8-tools-deps

WORKDIR /app

# copies the code
COPY . ./

EXPOSE 5000

RUN curl -sL https://deb.nodesource.com/setup_12.x | bash - \
  && apt-get install -y nodejs \
  && npm install -g shadow-cljs \
  && npm install

RUN shadow-cljs release main

CMD ["clojure", "-m", "app.main"]