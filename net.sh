#!/bin/bash

select NETWORK in $(docker network ls --format {{.Name}});
do
  echo $NETWORK
  break
done