#!/usr/bin/env bash 

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REF="$HOME/IdeaProjects/OC/OC/dist/OC.jar"
TAR=$(cd "$dir/target" && ls TCat*.jar)

C=$( cd $dir && find src/ target/ pom.xml ${REF} -printf "%T@  %p\n" | sort | awk -v t="${TAR}" 'BEGIN{FS="/";s=0;m=0}{ if(m==1){s++}; if (t==$NF){m=1} }END{print s}' )


if [[ "$C" -ge 1 ]]; then
	cd $dir && cp -p $REF src/main/resources/ && mvn clean install
	cd $dir && ls lib/ | grep ".jar$" | awk 'BEGIN{FS="-"; s=0; f="";fv="" }{ v=substr($NF,1,length($NF)-4); t=substr($0,1,length($0)-length($NF)-1); if ( t == f ) { print t"-"fv".jar" ;}  f=t; fv=v; }' | while read pa rest; do echo remove old $pa; rm lib/$pa; done

fi

libs=$( ls lib/ | awk -v base="lib/" 'BEGIN{s=""}{ s=s":"base $1}END{print s}' )
cd $dir && java -cp target/${TAR}${libs} com.macmario.services.web.tcat.TCat  -d -d -d -app $dir/webapp/config $@

exit 0

