#!/usr/bin/env bash 

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

#remove old 
#cd $dir && cp ~/IdeaProjects/OC/OC/dist/OC.jar src/main/resources/ && mvn clean install && ls lib/ | grep ".jar$" | awk 'BEGIN{FS="-"; s=0; f="";fv="" }{ v=substr($NF,1,length($NF)-4); t=substr($0,1,length($0)-length($NF)-1); if ( t == f ) { print t"-"fv".jar" ;}  f=t; fv=v; }' | while read pa rest; do echo remove old $pa; rm lib/$pa; done

cd $dir && rm ears/OKSM*.ear 2>/dev/null 
cd $dir && cp ../OKSM/target/OKSM-*.war ears/OKSM.ear && cd ears && jar -ufv OKSM.ear config.war

cd $dir && java -cp target/TCat-1.0.jar$( ls lib/ | awk -v base="lib/" 'BEGIN{s=""}{ s=s":"base $1}END{print s}' ) com.macmario.services.web.tcat.TCat  -d -d -d -app $dir/ears/OKSM.ear $@

