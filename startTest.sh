#!/usr/bin/env bash 

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $dir && mvn clean install && java -cp target/TCat-1.0.jar$( ls lib/ | awk -v base="lib/" 'BEGIN{s=""}{ s=s":"base $1}END{print s}' ) com.macmario.services.web.tcat.TCatTest  -d -d -d -app /home/sumario/progs/apache-tomcat-9.0.56/webapps/docs/appdev/sample/sample.war $@
