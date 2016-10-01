#!/bin/bash

isalive() {
  sleep 300s
  echo "Uploading..."
  isalive
}

isalive &


files1=$(find */build/outputs/apk -type f)
files2=$(find */build/outputs/lint-results-*.* -type f)

files=$(echo $files1 $files2)

echo Uploading: $files

tar cvfz build.tar.gz $files

main=$PWD

cd $main

if [ -f build.tar.gz ]; then
  if [ "${TRAVIS_PULL_REQUEST}" = "false" ]; then
    md5=`md5sum build.tar.gz | fold -w 32 | head -n 1`
    commit=`git rev-parse HEAD`
    ap="md5.$md5.commit.$commit.type.tar.gz"
    doupload="curl -X POST -F iso=@build.tar.gz -F key=$UPLOADKEY -F ap=$ap https://mkg20001.sytes.net/os-loader/samremote.php --connect-timeout 10 -m 300"
    i=0;
    doupload_exec() {
      $doupload
      if [ $? -ne 0 ]; then
        let i=$i+1;
        echo "Upload FAIL! Retry $i..."
        doupload_exec
      fi
    }
    doupload_exec
    echo "Uploaded as build.tar.gz.$ap"
  else
    echo "Sucess - Skip Pull Upload"
  fi
else
  echo "Build was NOT SUCESSFULL - not uploading!"
fi
