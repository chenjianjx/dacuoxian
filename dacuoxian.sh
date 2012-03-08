#!/bin/sh

#Please search "top-doc" inside this script to understand high-level algorithm applied

HOSTS_FILE=/etc/hosts
SUCCESS=0
FAILURE=1 

#top-doc: validation
if [ ! -w $HOSTS_FILE ]; then
    echo "$HOSTS_FILE is not existing or you have no permission to write it" >&2
    exit $FAILURE
fi

if [ ! -r $HOSTS_FILE ]; then
    echo "You hava no permission to read the $HOSTS_FILE" >&2
    exit $FAILURE
fi


if [ "$1"x =  "disable"x ]; then
    mode=disable
    new_hosts_file=$2
    echo "Going to disable mapping declared in $new_hosts_file"
else
    new_hosts_file=$1
fi 

if [ -z $new_hosts_file  ]; then
   echo "Usage: dacuoxian.sh new-hosts-snippet-file-path" >&2
   echo "Usage: dacuoxian.sh disable hosts-snippet-file-path" >&2
   exit $FALIURE 
fi


if [ ! -r $new_hosts_file  ]; then 
    echo "$new_hosts_file is not existing or you have no permission to read it" >&2
    exit $FAILURE
fi

if [ -d $new_hosts_file ]; then
    echo "$new_hosts_file is a directory. Please input a file" >& 2
    exit $FAILURE
fi

#top-doc: Back up the old hosts file
backup_dir=`dirname $HOSTS_FILE`/hosts-backup
[ ! -d $backup_dir ] && `mkdir $backup_dir`
hosts_backup_file=$backup_dir/hosts.`date "+%Y%m%d%H%M%S%N"`
cat $HOSTS_FILE > $hosts_backup_file
echo


#top-doc: Now process the new hosts file
#top-doc: clean the new hosts file
echo Reading $new_hosts_file
new_hosts_cleaned=`mktemp -t`
#actions of the following lines
##remove blank lines
##remove blanks at the first of each line
##select lines starting with a number 
sed '/^\s*$/d' $new_hosts_file \
|sed 's/^\s\+//g' \
|sed -n '/^[0-9]/p' \
> $new_hosts_cleaned

echo New hosts cleaned as those in file "$new_hosts_cleaned"

#top-doc: re-arrange the new hosts so that each line has only a single domain, which can be delt with more easily
new_hosts_arranged=`mktemp -t`
while read line;
do 
    echo $line| \
    awk '{
        for ( f = 2; f <= NF; f ++){
            if(match($f,"^#.*")){
                break;   #ignore comments 
            }
            
            printf($1);
            printf(" ");
            printf($f);
            printf("\n");
	}

    }'	
done < $new_hosts_cleaned > $new_hosts_arranged 

echo New hosts re-arranged as those in  $new_hosts_arranged


#top-doc: now get all the domains defined in the new hosts file
all_domains=`mktemp -t`
awk '{printf($2); printf("\n"); }' $new_hosts_arranged > $all_domains
##sort the domains by length so that longest domains will be matched first, and thus avoid partial matching
awk '{print length(), $0 | "sort -n -r" }' $all_domains|awk '{print $2}' > $all_domains

echo Domains to be coped with this time are those in $all_domains

#top-doc: remove all the occurences of the domains in the old hosts file
old_hosts_with_domains_removed=`mktemp -t`
cat $HOSTS_FILE > $old_hosts_with_domains_removed
while read domain_line;
do
    ohwdr_temp=`mktemp -t`   
    sed "s/${domain_line}\s//g" $old_hosts_with_domains_removed > $ohwdr_temp
    cat $ohwdr_temp > $old_hosts_with_domains_removed

    sed "s/${domain_line}$//g" $old_hosts_with_domains_removed > $ohwdr_temp
    cat $ohwdr_temp > $old_hosts_with_domains_removed

done < $all_domains 


##There may be incomplete entries in old hosts file which has only the ip but no domain, after some domains have be removed
incomplete_temp=`mktemp -t`
cat $old_hosts_with_domains_removed > $incomplete_temp
sed -e '/^\s*[0-9.]\+\s*$/d' -e '/^\s*[0-9.]\+\s*#/d'  $incomplete_temp > $old_hosts_with_domains_removed

echo Old hosts with specified domains removed can be seen in  $old_hosts_with_domains_removed


if [ "$mode"x = "disable"x ]; then
    cat $old_hosts_with_domains_removed > $HOSTS_FILE
    echo "entries in $new_hosts_file have been removed"
    echo "Done"
    exit $SUCCESS 
fi


#top-doc: time to put new entries to the real hosts file. It will be made up of old-entries-without-domains + new ip-domain mappiings
cat $old_hosts_with_domains_removed > $HOSTS_FILE 
echo >> $HOSTS_FILE
hosts_name=`basename $new_hosts_file`
awk '{print $0, hn}' hn=\#$hosts_name-by-dacuoxian $new_hosts_cleaned  >> $HOSTS_FILE
echo >> $HOSTS_FILE

echo 
echo 
echo Done! 
echo "Hosts in $new_hosts_file have been added to $HOSTS_FILE"

#top-doc: finally, inform the user about the backup file
echo "And the old hosts file has been backed-up as $hosts_backup_file"

exit $SUCCESS
