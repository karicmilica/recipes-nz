# recipes-nz

An application written in Clojure and using ClojureScript, Noir, Enlive and Monger libraries.

Main functionalities:
 1. filling database with the recipes and their ratings 
 2. providing recipe recommendation to users (web application)

1) First, application finds links to recipes in the specific web pages (https://foodinaminute.co.nz/). 
Then, call service "Microdata to RDF Distiller" to extract recipe data based on recipe url. For every 
recipe, application extracts ratings and users who rated it. At the end, recipes, users and recipe ratings
are stored in database (MongoDB).
Some ideas related to managing agents are taken from book "Clojure Programming" Chas Emerick,Brian Carper,
and Christophe Grand (Chapter 4. Concurrency and Parallelism - Agents).    

2) Web application allows users to register themselves and log in. When user is logged in, he/she can 
rate a recipe. To make recommendations to a user, system determines which users are similar based on 
their rated recipes. 
If a user is not logged in, he/she can only search recipes by ingredient. 

- to compile clojurescript, launch lein cljsbuild once from the terminal
- to fill database with recipes, set :main to extraction.recipe-extractor in project.clj and
  launch lein run from the terminal
- to start web-application set :main to server.server in project.clj and launch lein run from
  the terminal

##### Goal

The primary goal of this application development was that I get an insight in MongoDB and Clojure. 
Before I started working on this project, I had always used relational databases. Surprisingly, everything
was easy with Clojure and MongoDB. I didn't have to learn new query language, I needed only Clojure 
maps. Clojure offers various powerful concepts, particularly in the domain of parallelization. For
development of this application, of course the most important was agent. It drew my attention, because I 
had not seen before anything like that. Also, some other concepts are so interesting for me, like passing
functions as arguments and returning them from function calls; atoms and the way to change their states;
multimethods; and of course map and reduce function that are present everywhere. Solely, I had a lot of 
problems with compiling ClojureScript, and finding appropriate documentations. Developing this application
I had opportunity to experience one new and efficient approach in considering the problems,thinking and code
writing.  

### Literature

 - HTML templating: https://github.com/cgrand/enlive/wiki/Table-and-Layout-Tutorial%2C-Part-1%3A-The-Goal (views.views)
 - Scraping: https://github.com/swannodette/enlive-tutorial (util.util-extraction, extraction.recipe-extractor)
 - ClojureScript: https://github.com/bahmanm/clojure-web-ajax-sample (cljs.main, server.server)
 - Web framework: http://www.webnoir.org/ (server.server)
 - Monger: http://clojuremongodb.info/articles/getting_started.html (db.db)
 - MongoDB schema design: http://www.10gen.com/presentations/schema-design-basics-1
  
## License

Distributed under the Eclipse Public License, the same as Clojure.