#!/usr/bin/env bash

function concatenate_args
{
    string=""
    # take all arguments from the second one
    for a in "${@:2}" # Loop over arguments
    do
        if [[ "${a:0:1}" != "-" ]] # Ignore flags (first character is -)
        then
            if [[ "$string" != "" ]]
            then
                string+=" " # Delimeter
            fi
            string+="$a"
        fi
    done
    echo "$string"
}

args="$(concatenate_args "$@")"

echo "Loading application..."
# -q --console plain: do not show building outputs
./gradlew -q --console plain $1 -Pmyargs="$args"
