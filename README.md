# Salttoday

## About

[Currently lives here](http://www.salttoday.ca)

Scrapes comments from Sootoday articles and displays like/dislike statistics for comments and users.

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

Datomic is used for storage, although you can use an in-mem instance for developing locally, you may still want to consider running an local instance. I personally it using [Docker](https://www.docker.com).

## Running

To start a web server for the application, run:

```bash
lein repl
```
```clj
(salttoday.core/-main)
```

Then run figwheel to get live interactive programming, run:

```bash
lein figwheel
```

By default, SaltToday connects to a in-memory Datomic instance. If you wish to have your data persisted, you can run your instance of Datomic with:

```bash
docker run -d -p 4334-4336:4334-4336 --name datomic-free akiel/datomic-free
```

Then modify the :database-url in [this file](env/dev/clj/salttoday/env.clj) to `datomic:free://localhost:4334/salttoday`.

For more information on running Datomic in docker check out these [docs](https://github.com/alexanderkiel/datomic-free).

### Running Locally Against Local In-Mem Database

You can run queries and such in the REPL once it is spun up, This allows for rapid development of the backend. For Example:

```clj
(require '[datomic.api :as d])
(def conn salttoday.db.core/conn)
(def db (d/db conn))
(def query (salttoday.db.core/create-get-comments-query db nil nil nil 17592186045892))
(def results (apply (partial d/q (:query query)) (:args query)))
;(def results (salttoday.db.core/get-comments db 0 nil nil 17592186045650))
```

## Datomic Usage on Server

It's also possible to use a repl in the server to query the live database:

```clj
(require '[datomic.api :as d])
(def conn (d/connect "datomic:free://localhost:4334/salttoday"))
(def db (d/db conn))
(def query (salttoday.db.core/create-get-comments-query db "Trudumb" nil))
(def results (apply (partial d/q (:query query)) (:args query)))
```
